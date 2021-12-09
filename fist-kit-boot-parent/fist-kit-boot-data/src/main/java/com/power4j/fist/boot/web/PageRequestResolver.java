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

package com.power4j.fist.boot.web;

import com.power4j.fist.data.domain.PageRequest;
import com.power4j.fist.data.domain.Pageable;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/9
 * @since 1.0
 */
public class PageRequestResolver implements HandlerMethodArgumentResolver {

	private String pageNumberKey = "current";

	private String pageSizeKey = "size";

	public void setPageNumberKey(String pageNumberKey) {
		this.pageNumberKey = pageNumberKey;
	}

	public void setPageSizeKey(String pageSizeKey) {
		this.pageSizeKey = pageSizeKey;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.getParameterType().isAssignableFrom(Pageable.class);
	}

	@Nullable
	@Override
	public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {
		HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
		if (Objects.isNull(request)) {
			return null;
		}
		int page = parseInt(request.getParameter(pageNumberKey), PageRequest.FIRST_PAGE);
		int size = parseInt(request.getParameter(pageSizeKey), PageRequest.DEFAULT_PAGE_SIZE);

		// TODO : 支持排序
		return PageRequest.of(page, size);
	}

	private int parseInt(@Nullable String str, int defaultVal) {
		if (Objects.isNull(str) || str.isEmpty()) {
			return defaultVal;
		}
		try {
			return Integer.parseInt(str);
		}
		catch (NumberFormatException e) {
			return defaultVal;
		}
	}

}
