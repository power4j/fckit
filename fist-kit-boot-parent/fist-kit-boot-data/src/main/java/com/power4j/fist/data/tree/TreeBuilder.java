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
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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

	private final Collection<? extends NodeIdx<ID, ?>> nodes;

	@Nullable
	private TreeNodeConfig config;

	@Nullable
	private TreeCustomizer<ID> customizer;

	private TreeBuilder(Collection<? extends NodeIdx<ID, ?>> nodes) {
		this.nodes = nodes;
	}

	/**
	 * 构造方法
	 * @param nodes 树形节点关系的集合
	 * @param <ID> ID类型
	 * @return TreeMaker
	 */
	public static <ID> TreeBuilder<ID> with(Collection<? extends NodeIdx<ID, ?>> nodes) {
		return new TreeBuilder<>(nodes);
	}

	/**
	 * 设置配置参数,用于定制化标准字段的名称
	 * @param config 配置参数
	 * @return TreeBuilder
	 */
	public TreeBuilder<ID> nodeConfig(@Nullable TreeNodeConfig config) {
		this.config = config;
		return this;
	}

	/**
	 * 设置个性化处理回调,用于填充业务相关属性,默认只有ID
	 * @param customizer 回调接口
	 * @return TreeBuilder
	 */
	public TreeBuilder<ID> customizer(@Nullable TreeCustomizer<ID> customizer) {
		this.customizer = customizer;
		return this;
	}

	/**
	 * 构建树
	 * @param rootId 起始节点
	 * @return 如果关系集合中没有该起始节点，返回empty
	 */
	public Optional<Tree<ID>> build(ID rootId) {
		// 父子关系
		Map<ID, Set<ID>> childrenMap = new HashMap<>(16);
		// @formatter:off
		nodes.stream().filter(o -> o.getDistance() == 1)
				.collect(Collectors.groupingBy(NodeIdx::getAncestor))
				.forEach((k,v) -> childrenMap.put(k,v.stream().map(NodeIdx::getDescendant).collect(Collectors.toSet())));
		// @formatter:on

		// 根节点
		Tree<ID> root = makeNode(rootId, null, config);
		Map<ID, Tree<ID>> map = new HashMap<>(16);
		childrenMap.forEach((key, value) -> value.forEach(id -> map.put(id, makeNode(id, key, config))));
		map.put(rootId, root);
		if (Objects.nonNull(customizer)) {
			customizer.customize(map.values());
		}
		return Optional.ofNullable(makeTree(rootId, map));
	}

	private Tree<ID> makeTree(ID rootId, Map<ID, Tree<ID>> source) {
		if (MapUtil.isEmpty(source)) {
			return null;
		}
		Tree<ID> root = source.get(rootId);
		if (Objects.isNull(root)) {
			return null;
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

	protected static <ID> Tree<ID> makeNode(ID id, ID pid, @Nullable TreeNodeConfig treeNodeConfig) {
		Tree<ID> tree = new Tree<>(treeNodeConfig);
		tree.setId(id);
		tree.setParentId(pid);
		return tree;
	}

}
