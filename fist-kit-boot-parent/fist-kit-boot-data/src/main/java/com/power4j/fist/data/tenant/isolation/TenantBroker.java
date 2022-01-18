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

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.power4j.coca.kit.common.exception.WrappedException;
import com.power4j.coca.kit.common.util.function.RunAny;
import com.power4j.coca.kit.common.util.function.SupplyAny;
import lombok.experimental.UtilityClass;
import org.springframework.lang.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/11
 * @since 1.0
 */
@UtilityClass
public class TenantBroker {

	private static final TransmittableThreadLocal<Deque<String>> DEBUG = new TransmittableThreadLocal<Deque<String>>() {
		@Override
		protected Deque<String> initialValue() {
			return new ArrayDeque<>();
		}
	};

	public void runAs(@Nullable String tenant, RunAny runnable) throws WrappedException {
		String pre = TenantHolder.getTenant().orElse(null);
		try {
			TenantHolder.setTenant(tenant);
			DEBUG.get().push(tenant);
			runnable.run();
		}
		catch (Throwable throwable) {
			throw WrappedException.wrap(throwable);
		}
		finally {
			TenantHolder.setTenant(pre);
			DEBUG.get().pop();
		}
	}

	public <T> T applyAs(@Nullable String tenant, SupplyAny<T> supplier) throws WrappedException {
		String pre = TenantHolder.getTenant().orElse(null);
		try {
			TenantHolder.setTenant(tenant);
			DEBUG.get().push(tenant);
			return supplier.get();
		}
		catch (Throwable throwable) {
			throw WrappedException.wrap(throwable);
		}
		finally {
			TenantHolder.setTenant(pre);
			DEBUG.get().pop();
		}
	}

	public String dump() {
		return CharSequenceUtil.join(" > ", DEBUG.get());
	}

}
