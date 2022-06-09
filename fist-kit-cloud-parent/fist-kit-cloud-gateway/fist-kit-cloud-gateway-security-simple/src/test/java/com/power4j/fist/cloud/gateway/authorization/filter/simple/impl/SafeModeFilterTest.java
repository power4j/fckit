/*
 * Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.power4j.fist.cloud.gateway.authorization.filter.simple.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/4/24
 * @since 1.0
 */
class SafeModeFilterTest {

	@Test
	void checkIp() {
		boolean pass1 = SafeModeFilter.matchAny("172.1.2.3", Collections.singleton("172.*"));
		Assertions.assertTrue(pass1);

		boolean pass2 = SafeModeFilter.matchAny("172.1.2.3", Collections.singleton("172.1.2.*"));
		Assertions.assertTrue(pass2);

		boolean pass3 = SafeModeFilter.matchAny("172.1.2.3", Collections.singleton("172.1.*.3"));
		Assertions.assertTrue(pass3);

		boolean pass4 = SafeModeFilter.matchAny("172.1.2.3", Collections.singleton("172.1.1.*"));
		Assertions.assertFalse(pass4);

		List<String> rules = new ArrayList<>(4);
		rules.add("127.*");
		rules.add("192.*");
		rules.add("10.*");
		boolean pass5 = SafeModeFilter.matchAny("10.1.2.3", rules);
		Assertions.assertTrue(pass5);
		boolean pass6 = SafeModeFilter.matchAny("11.1.2.3", rules);
		Assertions.assertFalse(pass6);
	}

}