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

package com.power4j.fist.data.tenant.isolation;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.springframework.lang.Nullable;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/11
 * @since 1.0
 */
public class TenantHolder {

	private static final TransmittableThreadLocal<String> TENANT_ID = new TransmittableThreadLocal<String>() {
		@Nullable
		@Override
		protected String initialValue() {
			return null;
		}
	};

	private TenantHolder() {
	}

	public static Optional<String> getTenant() {
		return Optional.ofNullable(TENANT_ID.get());
	}

	public static String getRequired() {
		return getTenant().orElseThrow(() -> new IllegalStateException("Can not offer tenant id"));
	}

	static void setTenant(@Nullable String val) {
		TENANT_ID.set(val);
	}

}
