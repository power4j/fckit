/*
 *  Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 *  Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.gnu.org/licenses/lgpl.html
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.power4j.fist.data.tree;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.map.MapUtil;
import com.power4j.fist.data.tree.domain.NodeIdx;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 工具类，用于根据树形节点关系构建树形结构
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/27
 * @since 1.0
 * @param <ID> ID 类型
 * @param <T> T 业务对象类型
 * @deprecated 请使用TreeMaker替代
 * @see TreeMaker
 */
public class TreeBuilder<ID, T> {

	// @formatter:off
	private final TreeUtil.NodeOp<ID, Tree<ID>> treeNodeOp = TreeUtil.NodeOp.<ID, Tree<ID>>builder()
			.idGetter(Tree::getId)
			.pidGetter(Tree::getParentId)
			.childConsumer(Tree::addChildren)
			.build();
	private final TreeUtil.AccessOp<ID, Tree<ID>> nodeAccessOp = TreeUtil.AccessOp.<ID, Tree<ID>>builder()
			.idGetter(Tree::getId)
			.pidGetter(Tree::getParentId)
			.childrenGetter(Tree::getChildren)
			.build();
	// @formatter:on

	@Nullable
	private TreeNodeConfig config;

	private final Function<? super T, ID> idFunc;

	private final Function<? super T, ID> pidFunc;

	private final Map<ID, T> sourceData;

	@Nullable
	private TreeNodeCustomizer<ID, ? super T> customizer;

	TreeBuilder(Map<ID, T> sourceData, Function<? super T, ID> idFunc, Function<? super T, ID> pidFunc) {
		this.sourceData = sourceData;
		this.pidFunc = pidFunc;
		this.idFunc = idFunc;
	}

	/**
	 * 构造方法
	 * @param data 业务数据
	 * @param idFunc 提取ID的函数
	 * @param pidFunc 提取父级ID函数
	 * @param <ID> ID 类型
	 * @param <T> 业务树类型
	 * @return 返回实例
	 */
	public static <ID, T> TreeBuilder<ID, T> use(Collection<T> data, Function<? super T, ID> idFunc,
			Function<? super T, ID> pidFunc) {
		Map<ID, T> sourceData = data.stream().collect(Collectors.toMap(idFunc, Function.identity()));
		return new TreeBuilder<>(sourceData, idFunc, pidFunc);
	}

	/**
	 * 构造方法
	 * @param nodes 树形节点索引
	 * @param <ID> ID 类型
	 * @return 返回实例
	 */
	public static <ID> TreeBuilder<ID, Tree<ID>> use(Collection<? extends NodeIdx<ID, ?>> nodes) {
		Function<ID, Tree<ID>> treeMapper = id -> {
			Tree<ID> tree = new Tree<>();
			tree.setId(id);
			return tree;
		};
		Map<ID, ID> parentMap = nodes.stream().filter(o -> (1 == o.getDistance()))
				.collect(Collectors.toMap(NodeIdx::getDescendant, NodeIdx::getAncestor));
		Map<ID, Tree<ID>> data = nodes.stream().filter(o -> (0 == o.getDistance())).map(NodeIdx::getAncestor)
				.map(treeMapper).collect(Collectors.toMap(Tree::getId, Function.identity()));
		data.values().forEach(o -> o.setParentId(parentMap.get(o.getId())));
		return new TreeBuilder<>(data, Tree::getId, Tree::getParentId);
	}

	public TreeBuilder<ID, T> nodeConfig(TreeNodeConfig config) {
		this.config = config;
		return this;
	}

	public TreeBuilder<ID, T> customizer(@Nullable TreeNodeCustomizer<ID, ? super T> customizer) {
		this.customizer = customizer;
		return this;
	}

	/**
	 * 构建树形结构
	 * @param id 根点ID,必须存在于数据源中,并且不是顶层节点
	 * @return 返回树形结构,如果数据源不包含根节点数据则返回empty
	 */
	public Optional<Tree<ID>> build(ID id) {
		List<Tree<ID>> roots = build(o -> Objects.equals(o.getId(), id));
		return roots.stream().findFirst();
	}

	/**
	 * 构建树形结构,自动推测根节点
	 * @return 返回根节点列表
	 */
	public List<Tree<ID>> build() {
		return makeTree(TreeBuilder::findTopNodes);
	}

	/**
	 * 构建树形结构
	 * @param rootPred 根节点断言
	 * @return 返回根节点列表,如果数据源不包含根节点数据则返回empty
	 */
	public List<Tree<ID>> build(Predicate<Tree<ID>> rootPred) {
		// @formatter:off
		Function<Map<ID, Tree<ID>>,Map<ID, Tree<ID>>> rootSelect = map -> map.entrySet()
				.stream()
				.filter(et -> rootPred.test(et.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		// @formatter:on
		return makeTree(rootSelect);
	}

	/**
	 * 构建树形结构 支持多个根节点
	 * @param rootSelect 根节选择器
	 * @return 返回根节点列表,如果数据源不包含根节点数据则返回empty
	 */
	protected List<Tree<ID>> makeTree(Function<Map<ID, Tree<ID>>, Map<ID, Tree<ID>>> rootSelect) {
		if (ObjectUtils.isEmpty(sourceData)) {
			return Collections.emptyList();
		}
		Map<ID, Tree<ID>> nodeMap = makeNodeMap();
		if (Objects.nonNull(customizer)) {
			nodeMap.values().forEach(node -> customizer.customize(node, sourceData.get(node.getId())));
		}
		// 根节点
		final Map<ID, Tree<ID>> roots = rootSelect.apply(nodeMap);
		if (ObjectUtils.isEmpty(roots)) {
			return Collections.emptyList();
		}
		fetch(nodeMap, roots);
		if (Objects.nonNull(customizer) && !roots.isEmpty()) {
			treeIteration(roots.values(), node -> customizer.customize(node, sourceData.get(node.getId())));
		}
		return new ArrayList<>(roots.values());
	}

	// ~ Utils
	// ===================================================================================================

	void treeIteration(Collection<Tree<ID>> list, Consumer<Tree<ID>> consumer) {
		TreeUtil.treeWalk(list, nodeAccessOp, consumer);
	}

	protected Map<ID, Tree<ID>> makeNodeMap() {
		// @formatter:off
		Map<ID, Tree<ID>> nodeMap = sourceData.values()
				.stream()
				.map(o -> makeNode(idFunc.apply(o), pidFunc.apply(o), config))
				.collect(Collectors.toMap(Tree::getId,Function.identity()));
		// @formatter:on
		return MapUtil.sortByValue(nodeMap, false);
	}

	/**
	 * 填充子级
	 * @param nodeMap 节点数据
	 * @param roots 根节点
	 */
	private void fetch(Map<ID, Tree<ID>> nodeMap, Map<ID, Tree<ID>> roots) {
		TreeUtil.fetch(nodeMap, roots, treeNodeOp);
	}

	protected static <ID> Map<ID, Tree<ID>> findTopNodes(Map<ID, Tree<ID>> input) {
		return TreeUtil.findAncestors(input, Tree::getId, Tree::getParentId);
	}

	protected static <ID> Tree<ID> makeNode(ID id, @Nullable ID pid, @Nullable TreeNodeConfig treeNodeConfig) {
		Tree<ID> tree = new Tree<>(treeNodeConfig);
		tree.setId(id);
		tree.setParentId(pid);
		return tree;
	}

	protected static <U, ID> Map<ID, List<Pair<ID, U>>> buildIdMap(Collection<U> nodes,
			Function<? super U, ID> idExtractor, Function<? super U, ID> pidExtractor) {
		Map<ID, List<Pair<ID, U>>> map = new HashMap<>(16);
		// @formatter:off
		nodes.stream()
				.filter(o -> Objects.nonNull(pidExtractor.apply(o)))
				.collect(Collectors.groupingBy(pidExtractor))
				.forEach((k,v) -> map.put(k,convertToPair(v,idExtractor,Function.identity())));
		// @formatter:on
		return map;
	}

	protected static <ID, T extends NodeIdx<ID, T>, U> Map<ID, List<Pair<ID, U>>> buildIdMap(Collection<T> nodes,
			Function<? super T, U> dataConverter) {
		Map<ID, List<Pair<ID, U>>> map = new HashMap<>(16);
		// @formatter:off
		nodes.stream().filter(o -> o.getDistance() == 1)
				.collect(Collectors.groupingBy(NodeIdx::getAncestor))
				.forEach((k,v) -> map.put(k,convertToPair(v,NodeIdx::getDescendant, dataConverter)));
		// @formatter:on
		return map;
	}

	protected static <ID, T, U> List<Pair<ID, U>> convertToPair(Collection<T> src, Function<? super T, ID> idExtractor,
			Function<? super T, U> dataConverter) {
		// @formatter:off
		return src.stream()
				.map(o -> Pair.of(idExtractor.apply(o),Objects.requireNonNull(dataConverter.apply(o))))
				.collect(Collectors.toList());
		// @formatter:on
	}

}
