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

package com.power4j.fist.data.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/11
 * @since 1.0
 */
@Data
public class AuditDTO implements Serializable {

	/**
	 * 数据标记,大多数情况下都是用户数据,因此设置默认值
	 */
	@Schema(description = "数据标记", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Integer lowAttr;

	/**
	 * 创建人
	 */
	@Schema(description = "创建人", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Long createBy;

	/**
	 * 创建时间
	 */
	@Schema(description = "创建时间", accessMode = Schema.AccessMode.READ_ONLY, example = "2021-02-01 13:01:30")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime createAt;

	/**
	 * 更新人
	 */
	@Schema(description = "更新人", accessMode = Schema.AccessMode.READ_ONLY)
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private Long updateBy;

	/**
	 * 更新时间
	 */
	@Schema(description = "更新时间", accessMode = Schema.AccessMode.READ_ONLY, example = "2021-02-01 13:01:30")
	@JsonProperty(access = JsonProperty.Access.READ_ONLY)
	private LocalDateTime updateAt;

}
