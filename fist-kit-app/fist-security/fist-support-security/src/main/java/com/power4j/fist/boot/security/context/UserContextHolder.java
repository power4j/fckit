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

package com.power4j.fist.boot.security.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.power4j.fist.boot.security.core.UserInfo;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/15
 * @since 1.0
 */
public final class UserContextHolder {

	private static final TransmittableThreadLocal<Ctx> CTX = new TransmittableThreadLocal<Ctx>() {
		@Override
		protected Ctx initialValue() {
			return new Ctx();
		}
	};

	private UserContextHolder() {
	}

	public static Optional<UserInfo> getUser() {
		return Optional.ofNullable(CTX.get().info);
	}

	public static UserInfo requireUser() {
		return getUser()
			.orElseThrow(() -> new IllegalStateException("Can not offer " + UserInfo.class.getSimpleName()));
	}

	public static void setUser(@Nullable UserInfo val) {
		CTX.get().setInfo(val);
	}

	public static void setOriginalValue(String val) {
		CTX.get().setOriginalValue(val);
	}

	public static Optional<String> getOriginalValue() {
		return Optional.ofNullable(CTX.get().getOriginalValue());
	}

	@Data
	static class Ctx {

		@Nullable
		private UserInfo info;

		@Nullable
		private String originalValue;

	}

}
