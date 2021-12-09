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

import com.power4j.coca.kit.common.lang.Obj;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/18
 * @since 1.0
 * @param <T> 实体类型
 */
public class PageImpl<T> implements Paged<T> {

	protected final static int FIRST_PAGE = 0;

	private final Pageable pageable;

	private final List<T> content;

	private final long total;

	public PageImpl(@Nullable List<T> content, Pageable pageable, long total) {
		this.pageable = pageable;
		this.total = total;
		this.content = Obj.keepIfNotNull(content, Collections::emptyList);
	}

	@Override
	public int getPageNumber() {
		return pageable.getPageNumber();
	}

	@Override
	public int getPageSize() {
		return pageable.getPageSize();
	}

	@Override
	public int getNumberOfElements() {
		return content.size();
	}

	@Override
	public int getTotalPages() {
		return getPageSize() == 0 ? 1 : (int) Math.ceil((double) total / (double) getPageSize());
	}

	@Override
	public long getTotalElements() {
		return total;
	}

	@Override
	public List<T> getContent() {
		return Collections.unmodifiableList(content);
	}

	@Override
	public boolean hasContent() {
		return !content.isEmpty();
	}

	@Override
	public Sort getSort() {
		return pageable.getSort();
	}

	@Override
	public boolean isFirst() {
		return !hasPrevious();
	}

	@Override
	public boolean isLast() {
		return !hasNext();
	}

	@Override
	public boolean hasNext() {
		return getPageNumber() + 1 < getTotalPages();
	}

	@Override
	public boolean hasPrevious() {
		return getPageNumber() > FIRST_PAGE;
	}

	@Override
	public Pageable nextPageable() {
		if (!hasNext()) {
			throw new IllegalArgumentException("No next page");
		}
		return pageable.next();
	}

	@Override
	public Pageable previousPageable() {
		if (!hasPrevious()) {
			throw new IllegalArgumentException("No previous page");
		}
		return pageable.previousOrFirst();
	}

	@Override
	public <U> Paged<U> map(Function<? super T, ? extends U> converter) {
		return new PageImpl<>(getConvertedContent(converter), getPageable(), total);
	}

	protected <U> List<U> getConvertedContent(Function<? super T, ? extends U> converter) {

		Assert.notNull(converter, "Function must not be null!");

		return this.content.stream().map(converter).collect(Collectors.toList());
	}

}
