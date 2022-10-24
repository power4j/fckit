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

package com.power4j.fist.boot.common.matcher


import org.springframework.web.util.pattern.PathPattern
import spock.lang.Specification
/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/21
 * @since 1.0
 */
class PathMatcherTest extends Specification {
	List<String> patterns = Arrays.asList(
			"/api/demo",
			"/api/user/last-login",
			"/api/user/home",
			"/api/user/{id}",
			"/api/user/{id}/{date}/photo"
	)
	PathMatcher pathMatcher = new PathMatcher()

	def "Should not match"(){
		given:
		String path = "/hello"
		when:
		List<String> result = pathMatcher.patternFilter(patterns,path)

		then:
		result.isEmpty()

		when:
		Optional<String> best = pathMatcher.bestMatch(patterns,path)

		then:
		!best.isPresent()
	}

	def "Should match"(){
		given:
		String path = "/api/user/home"
		when:
		List<String> result = pathMatcher.patternFilter(patterns,path)

		then:
		result.containsAll(Arrays.asList("/api/user/home","/api/user/{id}"))

		when:
		Optional<String> best = pathMatcher.bestMatch(patterns,path)

		then:
		best.isPresent()
		best.get() == "/api/user/home"
	}

	def "Test match info"(){
		given:
		String pattern = "/lina/{p1}/to/{p2}"
		String path = "/lina/want/to/play"
		when:
		PathPattern.PathMatchInfo info = new PathMatcher().match(pattern,path).orElse(null)

		then:
		info.uriVariables.get("p1") == "want"
		info.uriVariables.get("p2") == "play"
	}
}
