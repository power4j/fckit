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

package com.power4j.fist.boot.common.spel;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.lang.Nullable;

/**
 * Spring 表达式工具 <a href=
 * "https://docs.spring.io/spring-framework/docs/3.2.x/spring-framework-reference/html/expressions.html">reference</a>
 *
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/18
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class SpringElUtil {

	private final SpelExpressionParser parser = new SpelExpressionParser();

	/**
	 * 表达式求值
	 * @param variableProvider 变量信息
	 * @param parserContext 模板解析上下文
	 * @param expr 表达式,表达式通过‘#’来引用变量
	 * @param clazz 返回结果的类型
	 * @param defVal 默认值
	 * @return 执行表达式后的结果,求值发生异常时返回默认值
	 * @see #evalWithVariable(VariableProvider,ParserContext, String, Class)
	 */
	@Nullable
	public <T> T evalWithVariable(@Nullable VariableProvider variableProvider, @Nullable ParserContext parserContext,
			String expr, Class<T> clazz, @Nullable T defVal) {
		try {
			return evalWithVariable(variableProvider, parserContext, expr, clazz);
		}
		catch (Exception e) {
			log.warn(e.getMessage(), e);
			return defVal;
		}
	}

	/**
	 * 表达式求值 <pre>
	 *     VariableProvider vp = () -> Map.of("foo",1,"bar",2);
	 *     eval(vp,"#foo + #bar",Integer.class) ->  3
	 *     eval(null,"1 + 2",String.class) ->  3
	 * </pre>
	 * @param variableProvider 变量信息
	 * @param parserContext 模板解析上下文
	 * @param expr 表达式,表达式通过‘#’来引用变量
	 * @param clazz 返回结果的类型
	 * @return 执行表达式后的结果
	 */
	@Nullable
	public <T> T evalWithVariable(@Nullable VariableProvider variableProvider, @Nullable ParserContext parserContext,
			String expr, Class<T> clazz) {
		EvaluationContext context = new StandardEvaluationContext();
		if (variableProvider != null) {
			variableProvider.getVariables().forEach(context::setVariable);
		}
		return eval(context, parserContext, expr, clazz);
	}

	/**
	 * 表达式求值 <pre>
	 *     eval(null,"'Hello World'.concat('!')",String.class) ->  "Hello World!"
	 *     eval(null,"new String('hello world').toUpperCase()",String.class) ->  "HELLO WORLD"
	 *
	 *     EvaluationContext context = new StandardEvaluationContext("abc");
	 *     eval(context,"#root",String.class) ->  "abc"
	 * </pre>
	 * @param evaluationContext 求值上下文
	 * @param parserContext 模板解析上下文
	 * @param expr 表达式
	 * @param clazz 返回结果的类型
	 * @return 执行表达式后的结果
	 */
	@Nullable
	public <T> T eval(@Nullable EvaluationContext evaluationContext, @Nullable ParserContext parserContext, String expr,
			Class<T> clazz) {
		Expression expression = parser.parseExpression(expr, parserContext);
		if (evaluationContext != null) {
			return expression.getValue(evaluationContext, clazz);
		}
		return expression.getValue(clazz);
	}

}
