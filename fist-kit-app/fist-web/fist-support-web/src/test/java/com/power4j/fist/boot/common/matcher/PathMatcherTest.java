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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.pattern.PathPattern;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/21
 * @since 1.0
 */
class PathMatcherTest {

	private final List<String> patterns = Arrays.asList("/api/demo", "/api/user/last-login", "/api/user/home",
			"/api/user/{id}", "/api/user/{id}/{date}/photo");

	private final PathMatcher pathMatcher = new PathMatcher();

	@Test
	void shouldNotMatch() {
		String path = "/hello";

		List<String> result = this.pathMatcher.patternFilter(this.patterns, path);

		Assertions.assertTrue(result.isEmpty());

		Optional<String> best = this.pathMatcher.bestMatch(this.patterns, path);

		Assertions.assertFalse(best.isPresent());
	}

	@Test
	void shouldMatch() {
		String path = "/api/user/home";

		List<String> result = this.pathMatcher.patternFilter(this.patterns, path);

		Assertions.assertTrue(result.containsAll(Arrays.asList("/api/user/home", "/api/user/{id}")));

		Optional<String> best = this.pathMatcher.bestMatch(this.patterns, path);

		Assertions.assertTrue(best.isPresent());
		Assertions.assertEquals("/api/user/home", best.get());
	}

	@Test
	void shouldReturnMatchInfo() {
		String pattern = "/lina/{p1}/to/{p2}";
		String path = "/lina/want/to/play";

		PathPattern.PathMatchInfo info = new PathMatcher().match(pattern, path).orElseThrow();

		Assertions.assertEquals("want", info.getUriVariables().get("p1"));
		Assertions.assertEquals("play", info.getUriVariables().get("p2"));
	}

}
