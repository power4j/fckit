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

package com.power4j.fist.cloud.gateway.auth.filter

import com.fasterxml.jackson.databind.ObjectMapper
import com.power4j.fist.boot.util.SpringEventUtil
import com.power4j.fist.cloud.gateway.auth.entity.IpRule
import com.power4j.fist.cloud.gateway.auth.enums.IpListEnum
import com.power4j.fist.cloud.gateway.auth.repository.IpRuleRepository
import org.junit.runner.RunWith
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import spock.lang.Specification

import java.time.LocalDateTime
/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/18
 * @since 1.0
 */
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Sputnik.class)
@PrepareForTest([SpringEventUtil.class])
class WhiteListFilterTest extends Specification {

	def setup(){
		PowerMockito.mockStatic(SpringEventUtil.class)
	}

	def "Test denyAccess" () {
		given:
		Map<String,IpRule> ruleMap = new HashMap<>(1);
		WhiteListFilter filter = new WhiteListFilter(null,null)

		and:
		ruleMap.put(ip,getIpRule(ip,startTime,endTime))

		expect:
		filter.checkAccess(ruleMap,requestIp,requestTime).getKey() == access

		where:
		requestTime     | requestIp   | startTime       | endTime         | ip          || access
		getTime(2021,3) | "127.0.0.1" | getTime(2021,4) | getTime(2021,4) | "127.0.0.1" || false
		getTime(2021,4) | "127.0.0.2" | getTime(2021,4) | getTime(2021,4) | "127.0.0.1" || false
		getTime(2021,4) | "127.0.0.1" | getTime(2021,4) | getTime(2021,4) | "127.0.0.1" || true

	}

	def "Test response status" () {

		given:
		GatewayFilterChain filterChain = Mock()
		IpRuleRepository ipRuleRepository = Mock()
		ObjectMapper objectMapper = new ObjectMapper();
		WhiteListFilter filter = Spy(new WhiteListFilter(objectMapper,ipRuleRepository))
		ipRuleRepository.listByType(_ as String) >> []
		filter.checkAccess(*_) >> Pair.of(false,"Mock access denied")

		and:
		MockServerHttpRequest request = MockServerHttpRequest
				.get("/foo")
				.header("x-forwarded-for","1.1.1.1")
				.build()
		MockServerWebExchange exchange = MockServerWebExchange.from(request)

		when:
		filter.filter(exchange,filterChain).block()

		then:
		exchange.getResponse().getStatusCode() == HttpStatus.FORBIDDEN

	}

	def getIpRule(String ip,LocalDateTime startTime,LocalDateTime endTime){
		IpRule rule = new IpRule();
		rule.setEndTime(endTime)
		rule.setStartTime(startTime)
		rule.setIp(ip)
		rule.setType(IpListEnum.WHITE.value)
		return rule
	}

	def getTime(int year,int month){
		return LocalDateTime.of(year,month,1,1,1,1)
	}
}
