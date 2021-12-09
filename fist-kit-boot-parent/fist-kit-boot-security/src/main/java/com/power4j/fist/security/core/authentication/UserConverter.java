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

package com.power4j.fist.security.core.authentication;

import com.power4j.fist.security.core.authorization.domain.AuthenticatedUser;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/12/1
 * @since 1.0
 */
public interface UserConverter<U extends AuthenticatedUser> {

	/**
	 * 转换为应用层用户
	 * @param authentication Authentication object
	 * @return 无用户信息返回 null
	 */
	@Nullable
	U convert(Authentication authentication);

}
