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

package com.power4j.fist.cloud.gateway.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.power4j.coca.kit.common.datetime.DateTimePattern;
import com.power4j.coca.kit.common.lang.Result;
import com.power4j.fist.boot.common.api.Results;
import com.power4j.fist.boot.common.error.ErrorCode;
import com.power4j.fist.boot.util.SpringEventUtil;
import com.power4j.fist.boot.web.reactive.util.ServerHttpRequestUtil;
import com.power4j.fist.boot.web.reactive.util.ServerHttpResponseUtil;
import com.power4j.fist.cloud.gateway.auth.entity.IpRule;
import com.power4j.fist.cloud.gateway.auth.enums.IpListEnum;
import com.power4j.fist.cloud.gateway.auth.event.IpRuleEvent;
import com.power4j.fist.cloud.gateway.auth.repository.IpRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/14
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class BlackListFilter implements GlobalFilter {

	private final ObjectMapper objectMapper;

	private final IpRuleRepository ipRuleRepository;

	private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DateTimePattern.DATETIME_UTC);

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String remoteIp = ServerHttpRequestUtil.getRemoteIp(exchange.getRequest()).orElse(null);
		if (Objects.isNull(remoteIp)) {
			if (log.isDebugEnabled()) {
				log.debug("Can not obtain ip address from request {},Skip IP filter", exchange.getRequest().getURI());
			}
			return chain.filter(exchange);
		}
		Map<String, IpRule> ipBlockMap = ipRuleRepository.listByType(IpListEnum.BLACK.getValue()).stream()
				.collect(Collectors.toMap(IpRule::getIp, o -> o));
		IpRule ipRule = ipBlockMap.getOrDefault(remoteIp, null);
		if (Objects.isNull(ipRule)) {
			return chain.filter(exchange);
		}
		final LocalDateTime now = LocalDateTime.now();
		boolean policyExpired = (now.isBefore(ipRule.getStartTime())) || (now.isAfter(ipRule.getEndTime()));
		if (policyExpired) {
			return chain.filter(exchange);
		}
		if (log.isDebugEnabled()) {
			log.debug("Block IP address {}", remoteIp);
		}
		String reason = String.format("%s blocked form %s to %s", remoteIp,
				dateTimeFormatter.format(ipRule.getStartTime()), dateTimeFormatter.format(ipRule.getEndTime()));
		Result<Void> result = Results.resultWithHint(ErrorCode.A0301, ErrorCode.A0301, null, reason);
		IpRuleEvent event = new IpRuleEvent(remoteIp, IpListEnum.BLACK, LocalDateTime.now(), reason);
		SpringEventUtil.publishEvent(event);
		return ServerHttpResponseUtil.responseWithJsonObject(exchange.getResponse(), objectMapper, result,
				HttpStatus.FORBIDDEN);
	}

}
