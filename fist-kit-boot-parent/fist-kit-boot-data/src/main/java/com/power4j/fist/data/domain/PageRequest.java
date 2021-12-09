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

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/18
 * @since 1.0
 */
public class PageRequest extends AbstractPageRequest {

	public final static int DEFAULT_PAGE_SIZE = 20;

	public final static int FIRST_PAGE = 0;

	private final Sort sort;

	PageRequest(int pageNumber, int pageSize, Sort sort) {
		super(pageNumber, pageSize);
		this.sort = sort;
	}

	public static PageRequest of(int pageNumber, int pageSize, Sort sort) {
		return new PageRequest(pageNumber, pageSize, sort);
	}

	public static PageRequest of(int pageNumber, int pageSize) {
		return of(pageNumber, pageSize, Sort.UNSORTED);
	}

	public static PageRequest of(int pageNumber) {
		return of(pageNumber, DEFAULT_PAGE_SIZE, Sort.UNSORTED);
	}

	public static PageRequest firstPage() {
		return of(FIRST_PAGE, DEFAULT_PAGE_SIZE, Sort.UNSORTED);
	}

	@Override
	public Pageable at(int pageNumber) {
		return new PageRequest(getPageNumber(), getPageSize(), getSort());
	}

	@Override
	public Sort getSort() {
		return sort;
	}

	@Override
	public PageRequest next() {
		return new PageRequest(getPageNumber() + 1, getPageSize(), getSort());
	}

	@Override
	public PageRequest first() {
		return new PageRequest(FIRST_PAGE, getPageSize(), getSort());
	}

	@Override
	public PageRequest previous() {
		return getPageNumber() == 0 ? this : new PageRequest(getPageNumber() - 1, getPageSize(), getSort());
	}

}
