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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import com.power4j.fist.data.tree.domain.NodeIdx;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 工具类，用于根据树形节点关系构建树形结构
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/27
 * @since 1.0
 * @param <ID> ID 类型
 */
public class TreeBuilder<ID> {

	@Nullable
	private TreeNodeConfig config;

	@Nullable
	private TreeCustomizer<ID> customizer;

	@Nullable
	private Map<ID, List<ID>> nodeMap;

	public TreeBuilder<ID> nodeConfig(TreeNodeConfig config) {
		this.config = config;
		return this;
	}

	public TreeBuilder<ID> customizer(TreeCustomizer<ID> customizer) {
		this.customizer = customizer;
		return this;
	}

	public TreeBuilder<ID> nodeMap(Map<ID, List<ID>> nodeMap) {
		this.nodeMap = nodeMap;
		return this;
	}

	public <U> TreeBuilder<ID> nodes(Collection<U> nodes, Function<? super U, ? extends ID> idFunc,
			Function<? super U, ? extends ID> pidFunc) {
		this.nodeMap = buildIdMap(nodes, idFunc, pidFunc);
		return this;
	}

	public TreeBuilder<ID> nodes(Collection<? extends NodeIdx<ID, ?>> nodes) {
		this.nodeMap = buildIdMap(nodes);
		return this;
	}

	/**
	 * 构建树形结构
	 * @param vId 挂载节点ID,用于构造树的根
	 * @return 返回树形结构
	 */
	public Tree<ID> build(ID vId) {
		// 根节点
		Tree<ID> root = makeNode(vId, null, config);
		if (ObjectUtils.isEmpty(nodeMap)) {
			return root;
		}
		Map<ID, Tree<ID>> map = new HashMap<>(16);
		nodeMap.forEach((key, value) -> value.forEach(id -> map.put(id, makeNode(id, key, config))));
		map.put(vId, root);
		if (Objects.nonNull(customizer)) {
			customizer.customize(map.values());
		}
		return makeTree(root, map);
	}

	private Tree<ID> makeTree(Tree<ID> root, @Nullable Map<ID, Tree<ID>> source) {
		if (ObjectUtils.isEmpty(source)) {
			return root;
		}
		fetch(root, source);
		return root;
	}

	/**
	 * 填充子级
	 * @param root 根节点
	 * @param source 节点MAP
	 */
	private void fetch(Tree<ID> root, Map<ID, Tree<ID>> source) {
		final Map<ID, Tree<ID>> eTreeMap = MapUtil.sortByValue(source, false);
		List<Tree<ID>> rootTreeList = CollUtil.newArrayList();
		ID parentId;
		for (Tree<ID> node : eTreeMap.values()) {
			if (null == node) {
				continue;
			}
			parentId = node.getParentId();
			if (ObjectUtil.equals(root.getId(), parentId)) {
				root.addChildren(node);
				rootTreeList.add(node);
				continue;
			}

			final Tree<ID> parentNode = eTreeMap.get(parentId);
			if (null != parentNode) {
				parentNode.addChildren(node);
			}
		}
	}

	protected static <ID> Tree<ID> makeNode(ID id, @Nullable ID pid, @Nullable TreeNodeConfig treeNodeConfig) {
		Tree<ID> tree = new Tree<>(treeNodeConfig);
		tree.setId(id);
		tree.setParentId(pid);
		return tree;
	}

	protected static <U, ID> Map<ID, List<ID>> buildIdMap(Collection<U> nodes,
			Function<? super U, ? extends ID> idExtractor, Function<? super U, ? extends ID> pidExtractor) {
		Map<ID, List<ID>> map = new HashMap<>(16);
		// @formatter:off
		nodes.stream()
				.filter(o -> Objects.nonNull(pidExtractor.apply(o)))
				.collect(Collectors.groupingBy(pidExtractor))
				.forEach((k,v) -> map.put(k,v.stream().map(idExtractor).collect(Collectors.toList())));
		// @formatter:on
		return map;
	}

	protected static <ID> Map<ID, List<ID>> buildIdMap(Collection<? extends NodeIdx<ID, ?>> nodes) {
		Map<ID, List<ID>> map = new HashMap<>(16);
		// @formatter:off
		nodes.stream().filter(o -> o.getDistance() == 1)
				.collect(Collectors.groupingBy(NodeIdx::getAncestor))
				.forEach((k,v) -> map.put(k,v.stream().map(NodeIdx::getDescendant).collect(Collectors.toList())));
		// @formatter:on
		return map;
	}

}
