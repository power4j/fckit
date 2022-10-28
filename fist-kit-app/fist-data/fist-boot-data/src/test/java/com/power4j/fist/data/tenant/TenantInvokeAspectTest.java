package com.power4j.fist.data.tenant;

import com.power4j.fist.boot.apidoc.ApiTrait;
import com.power4j.fist.data.tenant.isolation.TenantBroker;
import com.power4j.fist.data.tenant.isolation.TenantHolder;
import com.power4j.fist.data.tenant.isolation.TenantHolderTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.aop.framework.AopProxy;
import org.springframework.aop.framework.DefaultAopProxyFactory;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
class TenantInvokeAspectTest {

	private final TenantInvokeAspect aspect = new TenantInvokeAspect();

	private SiteLvlController controllerProxy;

	@BeforeEach
	public void setup() {
		AspectJProxyFactory aspectJProxyFactory = new AspectJProxyFactory(new SiteLvlController());
		aspectJProxyFactory.addAspect(aspect);

		DefaultAopProxyFactory proxyFactory = new DefaultAopProxyFactory();
		AopProxy aopProxy = proxyFactory.createAopProxy(aspectJProxyFactory);

		controllerProxy = (SiteLvlController) aopProxy.getProxy();
	}

	@Test
	public void siteLevelTest() {
		TenantHolderTestSupport.setCurrentTenant("1");
		Optional<?> ret = controllerProxy.expose();
		Assertions.assertTrue(ret.isEmpty());
	}

	@Slf4j
	static class SiteLvlController {

		@ApiTrait(level = ApiTrait.ApiLevel.PL)
		public Optional<String> expose() {
			log.info("tenant context :{}", TenantBroker.dump());
			return TenantHolder.getTenant();
		}

	}

}