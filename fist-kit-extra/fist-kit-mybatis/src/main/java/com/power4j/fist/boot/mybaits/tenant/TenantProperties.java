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

import com.power4j.fist.boot.common.prop.PropConstant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

/**
 * 数据库租户隔离(行级)配置
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/11
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = TenantProperties.PROP_PREFIX)
public class TenantProperties {

	public static final String PROP_ENTRY = "db.tenant";

	public static final String PROP_PREFIX = PropConstant.PROP_ROOT_PREFIX + PROP_ENTRY;

	/**
	 * 租户字段名称,MP插件限制,必须统一
	 */
	private String column = "tenant_id";

	/**
	 * 支持租户隔离的表名称
	 */
	private Set<String> tables = new HashSet<>();

}
