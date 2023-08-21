package com.power4j.fist.support.spring.spel;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.ParserContext;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
class SpringElUtilTest {

	@Test
	void testUseVar() {
		Map<String, Object> vars = Map.of("foo", 1, "bar", 2);
		Integer ret = SpringElUtil.evalWithVariable(() -> vars, null, "#foo + #bar", Integer.class);
		Assertions.assertEquals(3, ret);

		String ret2 = SpringElUtil.evalWithVariable(() -> vars, null, "#xx?.toString()", String.class);
		Assertions.assertEquals(null, ret2);
	}

	@Test
	void testUseRoot() {
		String ret = SpringElUtil.eval(new StandardEvaluationContext("abc"), null, "#root", String.class);
		Assertions.assertEquals("abc", ret);
	}

	@Test
	void testUseBean() {
		Map<String, Object> beanMap = Map.of("foo", 1, "bar", 2);
		BeanResolver resolver = (ctx, name) -> beanMap.get(name);

		StandardEvaluationContext context = new StandardEvaluationContext();
		context.setBeanResolver(resolver);
		Integer ret = SpringElUtil.eval(context, null, "@foo + @bar", Integer.class);
		Assertions.assertEquals(3, ret);
	}

	@Test
	void testUseTemplate() {
		ParserContext parserContext = ParserContext.TEMPLATE_EXPRESSION;

		String ret = SpringElUtil.eval(null, parserContext, "1 + 2", String.class);
		Assertions.assertEquals("1 + 2", ret);

		ret = SpringElUtil.eval(null, parserContext, "#{1 + 2}", String.class);
		Assertions.assertEquals("3", ret);

		ret = SpringElUtil.eval(null, parserContext, "hello #{1 + 2}", String.class);
		Assertions.assertEquals("hello 3", ret);

		Map<String, Object> vars = Map.of("foo", 1, "bar", 2);
		StandardEvaluationContext context = new StandardEvaluationContext();
		vars.forEach(context::setVariable);

		ret = SpringElUtil.eval(context, parserContext, "#{#foo + #bar}", String.class);
		Assertions.assertEquals("3", ret);

	}

}
