package com.power4j.fist.security.core.authorization.filter.reactive;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/7
 * @since 1.0
 */
class DefaultServerAuthFilterChainTest {

	ServerAuthFilter<List<String>> filter1 = (ctx, chain) -> {
		ctx = new ArrayList<>(ctx);
		ctx.add("filter-1");
		return chain.filter(ctx);
	};

	ServerAuthFilter<List<String>> filter2 = (ctx, chain) -> {
		ctx.add("filter-2");
		return chain.filter(ctx);
	};

	@Test
	void filter() {
		DefaultServerAuthFilterChain<List<String>, ServerAuthFilter<List<String>>> chan = new DefaultServerAuthFilterChain<>(
				Arrays.asList(filter1, filter2));
		List<String> list = chan.filter(new ArrayList<>()).checkpoint().block();
		Assertions.assertEquals(2, list.size());
	}

}