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
import com.power4j.fist.boot.mybaits.entity.LogicDelEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/14
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("fplam_ip_rule")
public class IpRule extends LogicDelEntity {

	/**
	 * 主健
	 */
	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	private String ip;

	/**
	 * 0 白名单 1 黑名单
	 */
	private String type;

	/**
	 * 开始时间(包含)
	 */
	private LocalDateTime startTime;

	/**
	 * 结束时间(包含)
	 */
	private LocalDateTime endTime;

}
