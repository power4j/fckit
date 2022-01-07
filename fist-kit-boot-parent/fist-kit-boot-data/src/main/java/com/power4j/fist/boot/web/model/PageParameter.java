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

package com.power4j.fist.boot.web.model;

import com.power4j.fist.data.domain.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/12/15
 * @since 1.0
 */
@Data
public class PageParameter implements Serializable {

	private final static long serialVersionUID = 1L;

	public final static int DEFAULT_PAGE_SIZE = 20;

	public final static int FIRST_PAGE = 1;

	@Schema(description = "页码", defaultValue = "0")
	private Integer pageNumber;

	@Schema(description = "页大小", defaultValue = "20")
	private Integer pageSize;

	public static PageParameter of(int pageNumber, int pageSize) {
		PageParameter page = new PageParameter();
		page.setPageNumber(Math.max(pageNumber, FIRST_PAGE));
		page.setPageSize(pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE);
		return page;
	}

	public PageRequest toPageRequest() {
		return PageRequest.of(pageNumber, pageSize);
	}

}
