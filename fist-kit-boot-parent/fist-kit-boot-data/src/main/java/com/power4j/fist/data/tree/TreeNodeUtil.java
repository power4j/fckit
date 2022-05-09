/*
 * Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.power4j.fist.data.tree;

import com.power4j.fist.data.tree.domain.Node;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/9
 * @since 2022.0.3
 */
@UtilityClass
public class TreeNodeUtil {

	/**
	 * 迭代树节点
	 * @param roots 根节点
	 * @param consumer 节点消费函数
	 * @param <ID> ID 类型
	 * @param <N> Node 子类
	 */
	public <ID, N extends Node<ID, N>> void treeWalk(Collection<? extends N> roots, Consumer<? super N> consumer) {
		for (final N node : roots) {
			consumer.accept(node);
			List<N> children = node.getChildren();
			if (ObjectUtils.isNotEmpty(children)) {
				treeWalk(children, consumer);
			}
		}
	}

	/**
	 * 根据属性排序
	 * @param list 根节点
	 * @param keyExtractor 属性取值函数
	 * @param <ID> ID 类型
	 * @param <N> Node 子类
	 */
	public static <ID, N extends Node<ID, N>, U extends Comparable<? super U>> void sortNodes(final List<N> list,
			final Function<? super N, ? extends U> keyExtractor) {
		list.sort(Comparator.comparing(keyExtractor));
		for (N node : list) {
			final List<N> children = node.getChildren();
			if (ObjectUtils.isNotEmpty(children)) {
				sortNodes(children, keyExtractor);
			}
		}
	}

	/**
	 * 树节点转list
	 * @param roots 根节点
	 * @param <ID> ID 类型
	 * @param <N> Node 子类
	 * @return 返回节点MAP
	 */
	public <ID, N extends Node<ID, N>> List<N> flatten(List<N> roots) {
		List<N> list = new ArrayList<>(roots.size());
		treeWalk(roots, list::add);
		return list;
	}

	/**
	 * 填充子级
	 * @param source 数据源
	 * @param roots 根节点
	 * @param <ID> ID 类型
	 * @param <N> Node 子类
	 */
	public <ID, N extends Node<ID, N>> void fetch(Map<ID, N> source, Map<ID, N> roots) {
		if (roots.isEmpty()) {
			return;
		}
		for (N node : source.values()) {
			if (null == node) {
				continue;
			}
			final ID id = Objects.requireNonNull(node.getId());
			if (roots.containsKey(id)) {
				continue;
			}
			final ID parentId = node.getParentId();
			final N parentNode = Optional.ofNullable(roots.get(parentId)).orElseGet(() -> source.get(parentId));
			if (null != parentNode) {
				parentNode.appendChild(node);
			}
		}
	}

}
