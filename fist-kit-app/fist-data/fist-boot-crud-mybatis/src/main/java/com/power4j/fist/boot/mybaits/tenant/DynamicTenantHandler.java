package com.power4j.fist.boot.mybaits.tenant;

import com.power4j.fist.data.tenant.isolation.TenantHolder;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @since 2022.1
 */
@Slf4j
public class DynamicTenantHandler extends TenantHandler {

	public DynamicTenantHandler(String tenantColumn, Set<String> tables) {
		super(tenantColumn, tables);
	}

	@Override
	public boolean ignoreTable(String tableName) {
		if (TenantHolder.getTenant().isEmpty()) {
			if (log.isDebugEnabled()) {
				log.debug("No tenant value present,ignore tenant process");
			}
			return true;
		}
		return super.ignoreTable(tableName);
	}

}
