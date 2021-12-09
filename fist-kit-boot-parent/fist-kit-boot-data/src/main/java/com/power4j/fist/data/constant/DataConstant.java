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

package com.power4j.fist.data.constant;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/7/1
 * @since 1.0
 */
public interface DataConstant {

	String FIELD_LOGIC_DEL = "delFlag";

	String FIELD_CREATE_AT = "createAt";

	String FIELD_UPDATE_AT = "updateAt";

	String FIELD_CREATE_BY = "createBy";

	String FIELD_UPDATE_BY = "updateBy";

	String FIELD_LOW_ATTR = "lowAttr";

	String COLUMN_LOGIC_DEL = "del_flag";

	String FLAG_NOT_DELETED = "0";

	String FLAG_DELETED = "1";

	/**
	 * 数据标记 系统级
	 */
	int LOW_ATTR_VALUE_SYSTEM = 0;

	/**
	 * 数据标记 用户级
	 */
	int LOW_ATTR_VALUE_USER = 1;

	/**
	 * 状态: 有效
	 */
	String STATUS_VALUE_ENABLED = "0";

	/**
	 * 状态: 禁用
	 */
	String STATUS_VALUE_DISABLED = "1";

}
