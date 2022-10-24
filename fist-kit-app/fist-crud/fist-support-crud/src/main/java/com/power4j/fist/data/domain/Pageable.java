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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.util.Assert;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/18
 * @since 1.0
 */
public interface Pageable {

	/**
	 * 页码
	 * @return 返回页码.
	 */
	int getPageNumber();

	/**
	 * 页大小
	 * @return 返回页大小
	 */
	int getPageSize();

	/**
	 * 返回排序参数
	 * @return 返回排序参数
	 */
	Sort getSort();

	/**
	 * 返回排序参数
	 * @param sort 默认值，不能是null
	 * @return Sort object
	 */
	@JsonIgnore
	default Sort getSortOr(Sort sort) {

		Assert.notNull(sort, "Fallback Sort must not be null!");

		return getSort().isSorted() ? getSort() : sort;
	}

	/**
	 * 翻页，下一页
	 * @return Pageable
	 */
	@JsonIgnore
	Pageable next();

	/**
	 * 翻页，前一页或者第一页
	 * @return Pageable
	 */
	@JsonIgnore
	Pageable previousOrFirst();

	/**
	 * 翻页，第一页
	 * @return Pageable
	 */
	@JsonIgnore
	Pageable first();

	/**
	 * 翻页，跳转到指定页码
	 * @param pageNumber 页码
	 * @return Pageable
	 */
	@JsonIgnore
	Pageable at(int pageNumber);

	/**
	 * 检查是否有前一页
	 * @return true 表示可以向前翻页
	 */
	boolean hasPrevious();

}
