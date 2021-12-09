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

package com.power4j.fist.security.core.authorization.domain;

import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.map.MapUtil;

import java.util.Map;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/26
 * @since 1.0
 */
public interface AuthenticatedUser {

	/**
	 * 用户名
	 * @return 用户名
	 */
	String getUsername();

	/**
	 * 所属租户
	 * @return 租户ID
	 */
	String getTenantId();

	/**
	 * 权限集
	 * @return 权限集,不会返回null
	 */
	Map<String, GrantedPermission> getPermissions();

	/**
	 * 扩展信息
	 * @return 返回扩展信息键值对,不会返回null
	 */
	Map<String, Object> getAdditionalInfo();

	/**
	 * 取扩展信息值
	 * @param key 属性的键
	 * @param defaultValue 默认值
	 * @param clazz 类型
	 * @return 值不存在返回传入的默认值
	 * @throws ClassCastException – 类型转换失败.
	 */
	default <T> T getInfo(String key, T defaultValue, Class<T> clazz) {
		if (!getAdditionalInfo().containsKey(key)) {
			return defaultValue;
		}
		return clazz.cast(getAdditionalInfo().get(key));
	}

	/**
	 * 取扩展信息值
	 * @param key 属性的键
	 * @param defaultValue 默认值
	 * @param type 类型
	 * @return 值不存在/类型转换失败,返回传入的默认值
	 */
	default <T> T getInfo(String key, T defaultValue, TypeReference<T> type) {
		return MapUtil.getQuietly(getAdditionalInfo(), key, type, defaultValue);
	}

	/**
	 * 是否匿名用户
	 * @return true 表示是匿名用户
	 */
	default boolean isAnonymous() {
		return false;
	}

}
