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

package com.power4j.fist.data.domain;

import java.util.List;
import java.util.function.Function;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/18
 * @since 1.0
 * @param <T> 实体类型
 */
public interface Paged<T> {

	/**
	 * 返回当前页码
	 * @return 返回当前页码
	 */
	int getPageNumber();

	/**
	 * 返回页大小
	 * @return 返回页大小
	 */
	int getPageSize();

	/**
	 * 返回当前数据条数
	 * @return 返回当前数据条数
	 */
	int getNumberOfElements();

	/**
	 * 总页数
	 * @return 返回总页数
	 */
	int getTotalPages();

	/**
	 * 总记录数
	 * @return 返回总记录数
	 */
	long getTotalElements();

	/**
	 * 获取数据
	 * @return 返回数据
	 */
	List<T> getContent();

	/**
	 * 是否包含数据
	 * @return boolean
	 */
	boolean hasContent();

	/**
	 * 返回请求使用的排序参数
	 * @return Sort
	 */
	Sort getSort();

	/**
	 * 是否为第一页
	 * @return boolean
	 */
	boolean isFirst();

	/**
	 * 是否为最后一页
	 * @return boolean
	 */
	boolean isLast();

	/**
	 * 是否有下一页
	 * @return 表示可以向后翻页
	 */
	boolean hasNext();

	/**
	 * 是否有前一页
	 * @return true 表示可以向前翻页
	 */
	boolean hasPrevious();

	/**
	 * 返回当前页的分页请求,可用于再次请求当前分页
	 * @return Pageable
	 */
	default Pageable getPageable() {
		return PageRequest.of(getPageNumber(), getPageSize(), getSort());
	}

	/**
	 * 返回下一页的分页请求，使用方应该先检查是否存在下一页
	 * @return Pageable
	 * @throws IllegalArgumentException 当前页已经是最后一页
	 * @see #nextOrLastPageable()
	 */
	Pageable nextPageable();

	/**
	 * 如果还有下一页则返回下一页的请求,否则返回当前页的请求
	 * @return Pageable
	 */
	default Pageable nextOrLastPageable() {
		return hasNext() ? nextPageable() : getPageable();
	}

	/**
	 * 返回前一页的分页请求，使用方应该先检查是否存在前一页
	 * @return Pageable
	 * @see #previousPageable()
	 * @throws IllegalArgumentException 当前页是第一页
	 */
	Pageable previousPageable();

	/**
	 * 转换方法
	 * @param converter 转换函数
	 * @return Paged
	 */
	<U> Paged<U> map(Function<? super T, ? extends U> converter);

}
