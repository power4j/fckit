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

import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PatternParseException;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/21
 * @since 1.0
 */
public class PathMatcher {

	private final PathPatternParser pathPatternParser = new PathPatternParser();

	/**
	 * 测试单一模式匹配
	 * @param pattern 模式,比如 {@code '/user/{id}','/resource/**'}
	 * @param path 路径值,比如 {@code '/user/1'}
	 * @return 返回true表示匹配
	 * @throws PatternParseException 模式解析出错
	 */
	public boolean matches(String pattern, String path) {
		return getPathPattern(pattern).matches(getPathContainer(path));
	}

	/**
	 * 单一模式匹配
	 * @param pattern 模式,比如 {@code '/user/{id}','/resource/**'}
	 * @param path 路径值,比如 {@code '/user/1'}
	 * @return 返回匹配结果,不能匹配返回empty
	 */
	public Optional<PathPattern.PathMatchInfo> match(String pattern, String path) {
		return Optional.ofNullable(getPathPattern(pattern).matchAndExtract(getPathContainer(path)));
	}

	/**
	 * 模式查找,返回与路径值匹配的模式
	 * @param patterns 模式列表
	 * @param path 路径值
	 * @return 返回匹配成功的模式,全都不匹配返回 empty
	 * @throws PatternParseException 模式解析出错
	 */
	public List<String> patternFilter(Collection<String> patterns, String path) {
		// @formatter:off
		return patterns.stream()
				.filter(o -> getPathPattern(o).matches(getPathContainer(path)))
				.collect(Collectors.toList());
		// @formatter:on
	}

	public <T> List<T> patternFilter(Collection<T> objects, String path, Function<T, String> patternExtractor) {
		// @formatter:off
		return objects.stream()
				.filter(o -> getPathPattern(patternExtractor.apply(o)).matches(getPathContainer(path)))
				.collect(Collectors.toList());
		// @formatter:on
	}

	/**
	 * 模式查找,返回第一个匹配
	 * @param patterns 模式列表
	 * @param path 路径值
	 * @return 返回最佳匹配
	 */
	public Optional<String> firstMatch(Collection<String> patterns, String path) {
		for (String p : patterns) {
			if (getPathPattern(p).matches(getPathContainer(path))) {
				return Optional.of(p);
			}
		}
		return Optional.empty();
	}

	/**
	 * 模式查找,返回最佳匹配
	 * @param patterns 模式列表
	 * @param path 路径值
	 * @return 返回最佳匹配
	 */
	public Optional<String> bestMatch(Collection<String> patterns, String path) {
		// @formatter:off
		return patternFilter(patterns, path)
				.stream()
				.min(Comparator.comparing(this::getPathPattern));
		// @formatter:on
	}

	public <T> Optional<T> bestMatch(Collection<T> objects, String path, Function<T, String> patternExtractor) {
		// @formatter:off
		return patternFilter(objects, path, patternExtractor)
				.stream()
				.min(Comparator.comparing(o -> getPathPattern(patternExtractor.apply(o))));
		// @formatter:on
	}

	protected PathPattern getPathPattern(String pattern) {
		return pathPatternParser.parse(pattern);
	}

	protected PathContainer getPathContainer(String path) {
		return PathContainer.parsePath(path);
	}

}
