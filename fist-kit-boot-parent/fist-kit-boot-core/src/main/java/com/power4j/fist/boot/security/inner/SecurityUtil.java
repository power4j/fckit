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

package com.power4j.fist.boot.security.inner;

import com.power4j.coca.kit.common.exception.WrappedException;
import com.power4j.coca.kit.common.util.function.RunAny;
import com.power4j.fist.boot.common.error.CommonErrors;
import com.power4j.fist.boot.security.context.UserContextHolder;
import com.power4j.fist.boot.security.core.UserInfo;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/20
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class SecurityUtil {

	/**
	 * 当前登录用户
	 */
	public Optional<UserInfo> getUser() {
		return UserContextHolder.getUser();
	}

	public UserInfo requireUser() {
		return getUser().orElseThrow(CommonErrors::authRequiredError);
	}

	/**
	 * 当前登录用户的租户ID
	 */
	public Optional<String> getTenantId() {
		return getUser().map(UserInfo::getTenantId);
	}

	public String requireTenantId() {
		return getTenantId().orElseThrow(CommonErrors::authRequiredError);
	}

	/**
	 * 当前登录用户的用户名
	 */
	public Optional<String> getUsername() {
		return getUser().map(UserInfo::getUsername);
	}

	public String requireUsername() {
		return getUsername().orElseThrow(CommonErrors::authRequiredError);
	}

	/**
	 * 当前登录用户的ID
	 */
	public Optional<Long> getUserId() {
		return getUser().map(UserInfo::getUserId);
	}

	public long requireUserId() {
		return getUserId().orElseThrow(CommonErrors::authRequiredError);
	}

	/**
	 * 当前登录用户的权限列表，用户会话不存在或者无权限返回空集合
	 */
	public Set<String> getAuthorities() {
		return getUser().map(UserInfo::getAuthorities).orElse(Collections.emptySet());
	}

	/**
	 * 以用户身份执行业务逻辑
	 * @param user 用户
	 * @param runnable 业务逻辑
	 */
	public static void act(@Nullable UserInfo user, RunAny runnable) throws WrappedException {
		final UserInfo pre = getUser().orElse(null);
		try {
			UserContextHolder.setUser(user);
			log.trace("Run as user: {}", user == null ? "null" : user.getUsername());
			runnable.run();
		}
		catch (Throwable throwable) {
			throw WrappedException.wrap(throwable);
		}
		finally {
			UserContextHolder.setUser(pre);
		}
	}

}
