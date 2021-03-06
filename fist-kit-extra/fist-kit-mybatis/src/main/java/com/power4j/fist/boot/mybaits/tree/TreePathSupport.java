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

import com.power4j.fist.data.tree.domain.NodeIdx;
import org.springframework.lang.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/19
 * @since 1.0
 */
public interface TreePathSupport<T extends NodeIdx<ID, T>, ID> {

	/**
	 * 查询下级节点关系
	 * @param id 起始节点
	 * @param distanceMin 最小层距离(包含)
	 * @param distanceMax 最大层距离(包含)
	 * @return 返回 id 为祖先的所有节点路径
	 */
	List<T> findAllDescendant(ID id, @Nullable Integer distanceMin, @Nullable Integer distanceMax);

	/**
	 * 查询下级节点关系
	 * @param ids 起始节点
	 * @param distanceMin 最小层距离(包含)
	 * @param distanceMax 最大层距离(包含)
	 * @return 返回 id 为祖先的所有节点路径
	 */
	List<T> findAllDescendant(Collection<ID> ids, @Nullable Integer distanceMin, @Nullable Integer distanceMax);

	/**
	 * 统计下级节点数量
	 * @param id 起始节点
	 * @param distanceMin 最小层距离(包含)
	 * @param distanceMax 最大层距离(包含)
	 * @return 返回 id 为祖先的所有节点路径
	 */
	long countAllDescendant(ID id, @Nullable Integer distanceMin, @Nullable Integer distanceMax);

	/**
	 * 查询上级节点关系
	 * @param id 起始节点
	 * @param distanceMin 最小层距离(包含)
	 * @param distanceMax 最大层距离(包含)
	 * @return 返回 id 的所有祖先节点路径
	 */
	List<T> findAllAncestor(ID id, @Nullable Integer distanceMin, @Nullable Integer distanceMax);

	/**
	 * 查询上级节点关系
	 * @param ids 起始节点
	 * @param distanceMin 最小层距离(包含)
	 * @param distanceMax 最大层距离(包含)
	 * @return 返回 id 的所有祖先节点路径
	 */
	List<T> findAllAncestor(Collection<ID> ids, @Nullable Integer distanceMin, @Nullable Integer distanceMax);

	/**
	 * 统计上级节点数量
	 * @param id 起始节点
	 * @param distanceMin 最小层距离(包含)
	 * @param distanceMax 最大层距离(包含)
	 * @return 返回 id 的所有祖先节点路径
	 */
	long countAllAncestor(ID id, @Nullable Integer distanceMin, @Nullable Integer distanceMax);

	/**
	 * 查询全部关系
	 * @param distanceMin 最小层距离(包含)
	 * @param distanceMax 最大层距离(包含)
	 * @return 返回关系集合列表
	 */
	List<T> getAll(@Nullable Integer distanceMin, @Nullable Integer distanceMax);

	/**
	 * 删除路径信息,当节点删除后应该使用此方法
	 * @param id 节点ID,与该节点相关的所有路径会被删除
	 */
	void removeAllPath(ID id);

	/**
	 * 添加路径信息,新增节点时应该使用此方法
	 * @param newNode 新增节点的ID
	 * @param parent 父节点
	 */
	void generatePath(ID newNode, ID parent);

	/**
	 * 计算构建子树树所需要的节点
	 * @param ids 候选根节点,它们的上下级关系不确定
	 * @return 返回节点集合,包含候选根节点以及它们的子节点,输入为空则返回空集
	 */
	Set<ID> subTreeNodes(Collection<ID> ids);

	/**
	 * 根据传入的节点,查找它们的公共根节点,如果数据源只有一个根节点,那么总是返回根节点
	 * @param ids 子节点ID
	 * @return 返回根节点集合,输入为空则返回空集
	 */
	Set<ID> findSharedRoot(Collection<ID> ids);

}
