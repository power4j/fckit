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
import com.power4j.coca.kit.common.lang.Pair;
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
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/16
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class WhiteListFilter implements GlobalFilter {

	private final static String REASON_IP_BLOCKED = "IP blocked";

	private final static String REASON_EXPIRED = "IP Permission expired";

	private final static String UNKNOWN_IP = "unknown";

	private final ObjectMapper objectMapper;

	private final IpRuleRepository ipRuleRepository;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		final LocalDateTime requestTime = LocalDateTime.now();
		String remoteIp = ServerHttpRequestUtil.getRemoteIp(exchange.getRequest()).orElse(null);
		if (Objects.isNull(remoteIp)) {
			if (log.isDebugEnabled()) {
				log.debug("Can not obtain ip address from request {},Reject by default",
						exchange.getRequest().getURI());
			}
			return denyAndPublishEvent(UNKNOWN_IP, requestTime, REASON_IP_BLOCKED, exchange.getResponse());
		}
		Map<String, IpRule> ipBlockMap = ipRuleRepository.listByType(IpListEnum.WHITE.getValue()).stream()
				.collect(Collectors.toMap(IpRule::getIp, o -> o));
		Pair<Boolean, String> status = checkAccess(ipBlockMap, remoteIp, requestTime);
		if (Boolean.FALSE.equals(status.getKey())) {
			return denyAndPublishEvent(remoteIp, requestTime, status.getValue(), exchange.getResponse());
		}
		return chain.filter(exchange);

	}

	Mono<Void> denyAndPublishEvent(String remoteIp, LocalDateTime requestTime, @Nullable String reason,
			ServerHttpResponse response) {
		if (log.isDebugEnabled()) {
			log.debug("Block IP address {},{}", remoteIp, reason);
		}
		Result<Void> result = Results.resultWithHint(ErrorCode.A0301, ErrorCode.A0301, null, reason);
		IpRuleEvent event = new IpRuleEvent(remoteIp, IpListEnum.WHITE, requestTime, reason);
		SpringEventUtil.publishEvent(event);
		return ServerHttpResponseUtil.responseWithJsonObject(response, objectMapper, result, HttpStatus.FORBIDDEN);
	}

	Pair<Boolean, String> checkAccess(Map<String, IpRule> rulesMap, String requestIp, LocalDateTime requestTime) {
		if (!ipMatch(rulesMap, requestIp)) {
			return Pair.of(Boolean.FALSE, REASON_IP_BLOCKED);
		}
		IpRule rule = rulesMap.get(requestIp);
		boolean expired = (requestTime.isBefore(rule.getStartTime())) || (requestTime.isAfter(rule.getEndTime()));
		if (expired) {
			return Pair.of(Boolean.FALSE, REASON_EXPIRED);
		}
		else {
			return Pair.of(Boolean.TRUE, null);
		}
	}

	boolean ipMatch(Map<String, IpRule> rulesMap, @Nullable String ip) {
		return Objects.nonNull(ip) && rulesMap.containsKey(ip);
	}

}
