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

package com.power4j.fist.boot.mybaits.tree;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.power4j.fist.data.tree.domain.NodeIdx;
import com.power4j.fist.boot.mybaits.crud.repository.Repository;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 负责维护数据库中树形结构的节点关系
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/23
 * @since 1.0
 * @param <T> NodeIdx 的子类
 * @param <ID> ID 类型
 * @param <R> Repository
 */
public abstract class AbstractNodeIdxSupport<T extends NodeIdx<ID, T>, ID extends Serializable, R extends Repository<T, ?>>
		implements TreePathSupport<T, ID> {

	/**
	 * Link Repository
	 * @return Repository
	 */
	protected abstract R getRepository();

	/**
	 * 创建对象
	 * @param ancestor 祖先节点ID
	 * @param descendant 后代节点ID
	 * @param distance 层距离
	 * @return C
	 */
	protected abstract T createObject(ID ancestor, ID descendant, int distance);

	/**
	 * 创建一个指向自己的 Path
	 * @param id 节点ID
	 * @return C
	 */
	protected T createObject(ID id) {
		return createObject(id, id, 0);
	}

	@Override
	public long countAllDescendant(ID id, @Nullable Integer distanceMin, @Nullable Integer distanceMax) {
		// @formatter:off
		LambdaQueryWrapper<T> wrapper = getRepository().lambdaWrapper()
				.eq(T::getAncestor,id)
				.ge(Objects.nonNull(distanceMin), T::getDistance,  distanceMin)
				.le(Objects.nonNull(distanceMax), T::getDistance,  distanceMax);
		return getRepository().countBy(wrapper);
		// @formatter:on
	}

	@Override
	public List<T> findAllDescendant(ID id, @Nullable Integer distanceMin, @Nullable Integer distanceMax) {
		// @formatter:off
		LambdaQueryWrapper<T> wrapper = getRepository().lambdaWrapper()
				.eq(T::getAncestor,id)
				.ge(Objects.nonNull(distanceMin), T::getDistance,  distanceMin)
				.le(Objects.nonNull(distanceMax), T::getDistance,  distanceMax);
		return getRepository().findAllBy(wrapper);
		// @formatter:on
	}

	@Override
	public List<T> findAllDescendant(Collection<ID> ids, @Nullable Integer distanceMin, @Nullable Integer distanceMax) {
		// @formatter:off
		LambdaQueryWrapper<T> wrapper = getRepository().lambdaWrapper()
				.in(T::getAncestor,ids)
				.ge(Objects.nonNull(distanceMin), T::getDistance,  distanceMin)
				.le(Objects.nonNull(distanceMax), T::getDistance,  distanceMax);
		return getRepository().findAllBy(wrapper);
		// @formatter:on
	}

	@Override
	public long countAllAncestor(ID id, @Nullable Integer distanceMin, @Nullable Integer distanceMax) {
		// @formatter:off
		LambdaQueryWrapper<T> wrapper = getRepository().lambdaWrapper()
				.eq(T::getAncestor,id)
				.ge(Objects.nonNull(distanceMin), T::getDistance,  distanceMin)
				.le(Objects.nonNull(distanceMax), T::getDistance,  distanceMax);
		return getRepository().countBy(wrapper);
		// @formatter:on
	}

	@Override
	public List<T> findAllAncestor(ID id, @Nullable Integer distanceMin, @Nullable Integer distanceMax) {
		// @formatter:off
		LambdaQueryWrapper<T> wrapper = getRepository().lambdaWrapper()
				.eq(T::getDescendant,id)
				.ge(Objects.nonNull(distanceMin), T::getDistance,  distanceMin)
				.le(Objects.nonNull(distanceMax), T::getDistance,  distanceMax);
		return getRepository().findAllBy(wrapper);
		// @formatter:on
	}

	@Override
	public List<T> getAll(@Nullable Integer distanceMin, @Nullable Integer distanceMax) {
		// @formatter:off
		LambdaQueryWrapper<T> wrapper = getRepository().lambdaWrapper()
				.ge(Objects.nonNull(distanceMin), T::getDistance,  distanceMin)
				.le(Objects.nonNull(distanceMax), T::getDistance,  distanceMax);
		return getRepository().findAllBy(wrapper);
		// @formatter:on
	}

	/**
	 * 删除节点关系,当节点删除后应该使用此方法
	 * @param id 节点ID,与该节点相关的所有路径会被删除
	 */
	@Override
	public void removeAllPath(ID id) {
		// @formatter:off
		LambdaQueryWrapper<T> wrapper = getRepository().lambdaWrapper()
				.eq(T::getAncestor,id)
				.or()
				.eq(T::getDescendant,id);
		getRepository().deleteAllBy(wrapper);
		// @formatter:on
	}

	/**
	 * 添加路径信息,新增节点时应该使用此方法
	 * @param newNode 新增节点的ID
	 * @param parent 父节点,可以为空
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void generatePath(ID newNode, @Nullable ID parent) {
		Assert.notNull(newNode, "new node can not null");
		// @formatter:off
		if(Objects.nonNull(parent)){
			List<T> pathList = findAllAncestor(parent,null,null)
					.stream().map(o -> createObject(o.getAncestor(),newNode,o.getDistance() + 1))
					.collect(Collectors.toList());

			getRepository().saveAll(pathList);
		}
		// @formatter:on
		getRepository().saveOne(createObject(newNode));
	}

	@Override
	public Set<ID> subTreeNodes(Collection<ID> ids) {
		List<T> paths = findAllDescendant(ids, null, null);
		Set<ID> set = new HashSet<>(ids.size());
		// @formatter:off
		paths.stream().filter(NodeIdx::isNodeOrImmediate)
				.collect(Collectors.toList())
				.forEach(o -> {
					set.add(o.getAncestor());
					set.add(o.getDescendant());
				});
		// @formatter:on
		return set;
	}

	protected LambdaQueryWrapper<T> allEq(@Nullable Long ancestor, @Nullable Long descendant,
			@Nullable Integer distance) {
		// @formatter:off
		return getRepository().lambdaWrapper()
				.eq(Objects.nonNull(ancestor), T::getAncestor,ancestor)
				.eq(Objects.nonNull(descendant), T::getDescendant,  descendant)
				.eq(Objects.nonNull(distance), T::getDistance,  distance);
		// @formatter:on
	}

}
