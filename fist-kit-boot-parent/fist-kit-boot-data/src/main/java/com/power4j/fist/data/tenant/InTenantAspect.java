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

package com.power4j.fist.data.tenant;

import com.power4j.fist.boot.common.aop.AopUtil;
import com.power4j.fist.boot.common.spel.MethodParameterResolver;
import com.power4j.fist.boot.common.spel.SpringElUtil;
import com.power4j.fist.data.tenant.annotation.InTenant;
import com.power4j.fist.data.tenant.isolation.TenantBroker;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/11
 * @since 1.0
 */
@Slf4j
@Aspect
public class InTenantAspect {

	private final SpelExpressionParser parser = new SpelExpressionParser();

	private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

	@Around("@annotation(inTenant)")
	public Object around(ProceedingJoinPoint point, InTenant inTenant) throws Throwable {
		Object[] arguments = point.getArgs();
		Method method = AopUtil.getMethod(point);
		final String tenant = SpringElUtil.eval(MethodParameterResolver.of(method, arguments), inTenant.value(),
				String.class, null);
		Ret ret = TenantBroker.applyAs(tenant, () -> {
			try {
				Object object = point.proceed();
				return new Ret(null, object);
			}
			catch (Throwable e) {
				return new Ret(e, null);
			}
		});
		if (Objects.nonNull(ret.getThrowable())) {
			throw ret.getThrowable();
		}
		return ret.getObject();
	}

	@Data
	@RequiredArgsConstructor
	static class Ret {

		private final Throwable throwable;

		private final Object object;

	}

}
