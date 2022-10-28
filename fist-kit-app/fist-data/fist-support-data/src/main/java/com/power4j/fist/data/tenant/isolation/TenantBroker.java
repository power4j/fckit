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
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.function.Failable;
import org.apache.commons.lang3.function.FailableConsumer;
import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.FailableSupplier;
import org.apache.commons.lang3.mutable.MutableObject;
import org.springframework.lang.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author CJ (power4j@outlook.com)
 * @since 2022.1
 */
@UtilityClass
public class TenantBroker {

	private static final TransmittableThreadLocal<Deque<String>> DEBUG = new TransmittableThreadLocal<Deque<String>>() {
		@Override
		protected Deque<String> initialValue() {
			return new ArrayDeque<>();
		}
	};

	/**
	 * 在指定的租户上下文中执行业务逻辑,并返回业务逻辑执行结构
	 * @param tenant 租户
	 * @param action 业务逻辑
	 * @param errorHandler 异常处理器
	 * @return 返回值由 action 的返回值确定
	 * @param <T> 返回值类型
	 */
	public <T> T applyAs(final @Nullable String tenant, FailableSupplier<T, ? extends Throwable> action,
			@Nullable final FailableFunction<Throwable, T, ? extends Throwable> errorHandler) {
		MutableObject<T> ret = new MutableObject<>();
		FailableRunnable<? extends Throwable> runnable = () -> ret.setValue(action.get());
		FailableConsumer<Throwable, ? extends Throwable> errorResume;
		if (errorHandler == null) {
			errorResume = Failable::rethrow;
		}
		else {
			errorResume = ex -> ret.setValue(errorHandler.apply(ex));
		}
		runAs(tenant, runnable, errorResume);
		return ret.getValue();
	}

	/**
	 * 在指定的租户上下文中执行业务逻辑,并返回业务逻辑执行结构
	 * @param tenant 租户
	 * @param action 业务逻辑
	 * @return 返回值由 action 的返回值确定
	 * @param <T> 返回值类型
	 */
	public <T> T applyAs(final @Nullable String tenant, FailableSupplier<T, ? extends Throwable> action) {
		return applyAs(tenant, action, null);
	}

	/**
	 * 在指定的租户上下文中执行业务逻辑
	 * @param tenant 租户
	 * @param action 业务逻辑
	 * @param errorHandler 异常处理器
	 */
	public void runAs(final @Nullable String tenant, final FailableRunnable<? extends Throwable> action,
			@Nullable final FailableConsumer<Throwable, ? extends Throwable> errorHandler) {

		final String pre = TenantHolder.getTenant().orElse(null);
		TenantHolder.setTenant(tenant);
		DEBUG.get().push(ObjectUtils.defaultIfNull(tenant, "null"));
		Failable.tryWithResources(action, errorHandler, () -> {
			TenantHolder.setTenant(pre);
			DEBUG.get().pop();
		});
	}

	/**
	 * 在指定的租户上下文中执行业务逻辑
	 * @param tenant 租户
	 * @param action 业务逻辑
	 */
	public void runAs(final @Nullable String tenant, final FailableRunnable<? extends Throwable> action) {
		runAs(tenant, action, null);
	}

	public String dump() {
		return StringUtils.join(DEBUG.get(), " > ");
	}

}
