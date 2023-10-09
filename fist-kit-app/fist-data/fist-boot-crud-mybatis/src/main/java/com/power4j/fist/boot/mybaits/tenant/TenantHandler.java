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

package com.power4j.fist.boot.mybaits.tenant;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.power4j.fist.boot.security.core.SecurityConstant;
import com.power4j.fist.data.tenant.isolation.TenantHolder;
import lombok.Setter;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/11
 * @since 1.0
 */
public class TenantHandler implements TenantLineHandler {

	private final String tenantColumn;

	private final Set<String> tables;

	@Setter
	private String defaultTenant = SecurityConstant.TENANT_ZERO;

	public TenantHandler(String tenantColumn, Set<String> tables) {
		this.tenantColumn = tenantColumn;
		this.tables = tables;
	}

	@Override
	public Expression getTenantId() {
		return new StringValue(TenantHolder.getTenant().orElse(defaultTenant));
	}

	@Override
	public String getTenantIdColumn() {
		return tenantColumn;
	}

	@Override
	public boolean ignoreTable(String tableName) {
		return !tables.contains(tableName);
	}

}
