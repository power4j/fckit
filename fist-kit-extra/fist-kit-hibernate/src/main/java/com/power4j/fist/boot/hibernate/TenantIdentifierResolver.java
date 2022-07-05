package com.power4j.fist.boot.hibernate;

import com.power4j.fist.data.tenant.isolation.TenantHolder;
import lombok.RequiredArgsConstructor;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/7/5
 * @since 1.0
 */
@RequiredArgsConstructor
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {
	private final String defaultTenant;
	@Override
	public String resolveCurrentTenantIdentifier() {
		return TenantHolder.getTenant().orElse(defaultTenant);
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}
}
