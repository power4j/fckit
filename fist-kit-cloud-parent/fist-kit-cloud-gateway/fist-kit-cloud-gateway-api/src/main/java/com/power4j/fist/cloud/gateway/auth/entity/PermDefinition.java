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

package com.power4j.fist.cloud.gateway.auth.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.power4j.fist.boot.mybaits.entity.AuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/5
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fiamc_api_info")
public class PermDefinition extends AuditEntity {

	public static final String API_LEVEL_PLATFORM = "0";

	public static final String API_LEVEL_TENANT = "1";

	@TableId(type = IdType.ASSIGN_ID)

	private Long id;

	/** 权限编码 */
	private String code;

	/** 服务名 */
	private String serviceName;

	/** HTTP方法 */
	private String method;

	/** 路径 */
	private String path;

	/** 资源TAG */
	private String resourceTags;

	/** 资源名称 */
	private String resourceName;

	/** 执行动作 类方法名称 */
	private String action;

	/** API描述 */
	private String description;

	/** 接口级别 0 平台 1 租户 */
	private String level;

	/** 公开访问 0 否 1 是 */
	private String pubAccessFlag;

	/** 登录可访问 0 否 1 是 */
	private String userAccessFlag;

	/** 内部访问 0 否 1 是 */
	private String internalAccessFlag;

	/** 签名校验 0 无 1 系统默认 */
	private String signFlag;

}
