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

package com.power4j.fist.cloud.gateway.proxy;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.cloud.gateway.route.Route;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@Getter
@Builder
public class RouteInfo {

	private final String id;

	private final URI uri;

	private final int order;

	private final Map<String, Object> metadata;

	private final List<String> filters;

	public static RouteInfo from(Route route) {
		List<String> filters = Collections.emptyList();
		if (ObjectUtils.isNotEmpty(route.getFilters())) {
			filters = route.getFilters().stream().map(o -> o.getClass().getName()).collect(Collectors.toList());
		}
		// @formatter:off
		return RouteInfo.builder()
				.id(route.getId())
				.uri(route.getUri())
				.order(route.getOrder())
				.metadata(route.getMetadata())
				.filters(filters)
				.build();
		// @formatter:on
	}

}
