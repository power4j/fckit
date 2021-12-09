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

package com.power4j.fist.boot.common.matcher;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/21
 * @since 1.0
 */
public class FastPathMatcher extends PathMatcher {

	private final Cache<String, PathPattern> pathPatterCache;

	private final Cache<String, PathContainer> pathContainerCache;

	public FastPathMatcher(int cacheSize) {
		pathPatterCache = Caffeine.newBuilder().maximumSize(cacheSize).build();
		pathContainerCache = Caffeine.newBuilder().maximumSize(cacheSize).build();
	}

	@Override
	protected PathPattern getPathPattern(String pattern) {
		return pathPatterCache.get(pattern, this::getPathPatternWithoutCache);
	}

	@Override
	protected PathContainer getPathContainer(String path) {
		return pathContainerCache.get(path, this::getPathContainerWithoutCache);
	}

	private PathPattern getPathPatternWithoutCache(String pattern) {
		return super.getPathPattern(pattern);
	}

	private PathContainer getPathContainerWithoutCache(String path) {
		return super.getPathContainer(path);
	}

}
