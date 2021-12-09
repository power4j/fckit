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

import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.cloud.gateway.route.Route;

import java.net.URI;
import java.util.Collections;
import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
@Getter
public class RouteInfo {

	private final String id;

	private final URI uri;

	private final int order;

	private final Map<String, Object> metadata;

	public RouteInfo(String id, URI uri, int order, Map<String, Object> metadata) {
		this.id = id;
		this.uri = uri;
		this.order = order;
		this.metadata = ObjectUtils.defaultIfNull(metadata, Collections.emptyMap());
	}

	public static RouteInfo from(Route route) {
		return new RouteInfo(route.getId(), route.getUri(), route.getOrder(), route.getMetadata());
	}

}
