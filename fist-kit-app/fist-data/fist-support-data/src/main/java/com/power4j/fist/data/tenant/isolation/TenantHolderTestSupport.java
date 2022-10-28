package com.power4j.fist.data.tenant.isolation;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

/**
 * @author CJ (power4j@outlook.com)
 * @since 2022.1
 */
@Slf4j
@UtilityClass
public class TenantHolderTestSupport {

	/**
	 * 强制设置当前租户值
	 * @param val 租户值
	 */
	public void setCurrentTenant(@Nullable String val) {
		log.warn("Set Tenant to {}", val);
		TenantHolder.setTenant(val);
	}

}
