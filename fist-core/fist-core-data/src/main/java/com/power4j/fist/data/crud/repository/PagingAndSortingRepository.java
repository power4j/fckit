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

package com.power4j.fist.data.crud.repository;

import com.power4j.fist.data.domain.Pageable;
import com.power4j.fist.data.domain.Paged;
import com.power4j.fist.data.domain.Sort;

import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/18
 * @since 1.0
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface PagingAndSortingRepository<T, ID> extends CrudRepository<T, ID> {

	/**
	 * 分页查询
	 * @param pageable 页信息
	 * @return 返回分页数据
	 */
	Paged<T> findAll(Pageable pageable);

	/**
	 * 查询
	 * @param sort 排序参数
	 * @return 返回分页数据
	 */
	List<T> findAllSorted(Sort sort);

}
