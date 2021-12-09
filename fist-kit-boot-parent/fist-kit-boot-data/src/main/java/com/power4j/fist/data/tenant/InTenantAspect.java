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

import com.power4j.fist.data.tenant.annotation.InTenant;
import com.power4j.fist.data.tenant.isolation.TenantBroker;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

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
		Method method = getMethod(point);
		final String tenant = evaluationExpr(method, arguments, inTenant.value(), String.class, null);
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

	/**
	 * 表达式求值
	 * @param method 方法
	 * @param arguments 参数
	 * @param expr 表达式
	 * @param clazz 返回结果的类型
	 * @param defVal 默认值
	 * @return 执行表达式后的结果
	 */
	<T> T evaluationExpr(Method method, Object[] arguments, String expr, Class<T> clazz, T defVal) {
		EvaluationContext context = new StandardEvaluationContext();
		String[] params = discoverer.getParameterNames(method);
		if (null != params) {
			for (int len = 0; len < params.length; len++) {
				context.setVariable(params[len], arguments[len]);
			}
		}
		try {
			Expression expression = parser.parseExpression(expr);
			return expression.getValue(context, clazz);
		}
		catch (Exception e) {
			log.warn(e.getMessage(), e);
			return defVal;
		}
	}

	private Method getMethod(ProceedingJoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		if (method.getDeclaringClass().isInterface()) {
			try {
				method = joinPoint.getTarget().getClass().getDeclaredMethod(joinPoint.getSignature().getName(),
						method.getParameterTypes());
			}
			catch (SecurityException | NoSuchMethodException e) {
				throw new IllegalStateException(e);
			}
		}
		return method;
	}

	@Data
	@RequiredArgsConstructor
	static class Ret {

		private final Throwable throwable;

		private final Object object;

	}

}
