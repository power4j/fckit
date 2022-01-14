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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 */
public class TreeBuilder<ID, T> {

	@Nullable
	private TreeNodeConfig config;

	@Nullable
	private TreeNodeCustomizer<ID, T> customizer;

	@Nullable
	private Map<ID, List<Pair<ID, T>>> dataMap;

	TreeBuilder(Map<ID, List<Pair<ID, T>>> dataMap) {
		this.dataMap = dataMap;
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
		return new TreeBuilder<>(buildIdMap(data, idFunc, pidFunc));
	}

	/**
	 * 构造方法
	 * @param nodes 树形节点索引
	 * @param <ID> ID 类型
	 * @return 返回实例
	 */
	public static <ID> TreeBuilder<ID, ?> use(Collection<? extends NodeIdx<ID, ?>> nodes) {
		return new TreeBuilder<>(buildIdMap(nodes, Function.identity()));
	}

	public TreeBuilder<ID, T> nodeConfig(TreeNodeConfig config) {
		this.config = config;
		return this;
	}

	public TreeBuilder<ID, T> customizer(TreeNodeCustomizer<ID, T> customizer) {
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
	 * 构建树形结构 支持多个根节点
	 * @param isRootNode 根节点断言
	 * @return 返回根节点列表,如果数据源不包含根节点数据则返回empty
	 */
	public List<Tree<ID>> build(Predicate<Tree<ID>> isRootNode) {
		if (ObjectUtils.isEmpty(dataMap)) {
			return Collections.emptyList();
		}
		Map<ID, Tree<ID>> map = new HashMap<>(16);
		dataMap.forEach((key, value) -> {
			value.forEach(pair -> map.put(pair.getKey(), makeNode(pair.getKey(), key, config)));
		});
		dataMap.keySet().forEach(key -> map.putIfAbsent(key, makeNode(key, null, config)));
		if (Objects.nonNull(customizer)) {
			// @formatter:off
			Map<ID,T> metaMap = dataMap.values()
					.stream()
					.flatMap(Collection::stream).collect(Collectors.toMap(Pair::getKey,Pair::getValue));
			map.values().forEach(node -> {
				customizer.customize(node,metaMap.get(node.getId()));
			});
			// @formatter:on
		}
		// 根节点列表
		Map<ID, Tree<ID>> roots = fetch(map, isRootNode);
		return roots.values().stream().sorted().collect(Collectors.toList());
	}

	/**
	 * 填充子级
	 * @param data 节点数据
	 * @param rootCheck 判断节点是不是根节点
	 * @return 根节点Map,没有找到根节点返回empty
	 */
	private Map<ID, Tree<ID>> fetch(Map<ID, Tree<ID>> data, Predicate<Tree<ID>> rootCheck) {
		final Map<ID, Tree<ID>> eTreeMap = MapUtil.sortByValue(data, false);
		// @formatter:off
		final Map<ID,Tree<ID>> roots = eTreeMap.entrySet()
				.stream()
				.filter(et -> rootCheck.test(et.getValue()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		// @formatter:on
		if (roots.isEmpty()) {
			return roots;
		}
		for (Tree<ID> node : eTreeMap.values()) {
			if (null == node) {
				continue;
			}
			ID parentId = node.getParentId();
			if (rootCheck.test(node)) {
				continue;
			}

			final Tree<ID> parentNode = Optional.ofNullable(eTreeMap.get(parentId))
					.orElseGet(() -> roots.get(parentId));
			if (null != parentNode) {
				parentNode.addChildren(node);
			}
		}
		return roots;
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

	protected static <ID, U> Map<ID, List<Pair<ID, U>>> buildIdMap(Collection<? extends NodeIdx<ID, ?>> nodes,
			Function<? super NodeIdx<ID, ?>, U> dataConverter) {
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
