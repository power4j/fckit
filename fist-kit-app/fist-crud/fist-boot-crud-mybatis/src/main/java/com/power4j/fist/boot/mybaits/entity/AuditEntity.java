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

package com.power4j.fist.boot.mybaits.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.power4j.fist.data.constant.DataConstant;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 包含常用审计字段,根据实际需要继承此类
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/16
 * @since 1.0
 */
@Getter
@Setter
public class AuditEntity {

	public final static String FIELD_CREATE_AT = DataConstant.FIELD_CREATE_AT;

	public final static String FIELD_UPDATE_AT = DataConstant.FIELD_UPDATE_AT;

	public final static String FIELD_CREATE_BY = DataConstant.FIELD_CREATE_BY;

	public final static String FIELD_UPDATE_BY = DataConstant.FIELD_UPDATE_BY;

	public final static String FIELD_LOW_ATTR = DataConstant.FIELD_LOW_ATTR;

	/**
	 * 数据标记
	 */
	@TableField(value = "low_attr", fill = FieldFill.INSERT)
	private Integer lowAttr;

	/**
	 * 创建人
	 */
	@TableField(value = "create_by", fill = FieldFill.INSERT)
	private Long createBy;

	/**
	 * 创建时间
	 */
	@TableField(value = "create_at", fill = FieldFill.INSERT)
	private LocalDateTime createAt;

	/**
	 * 更新人
	 */
	@TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
	private Long updateBy;

	/**
	 * 更新时间
	 */
	@TableField(value = "update_at", fill = FieldFill.INSERT_UPDATE)
	private LocalDateTime updateAt;

	/**
	 * 标记为系统数据
	 */
	public void markSysAttr() {
		lowAttr = DataConstant.LOW_ATTR_VALUE_SYSTEM;
	}

	/**
	 * 标记为用户数据
	 */
	public void markUserAttr() {
		lowAttr = DataConstant.LOW_ATTR_VALUE_USER;
	}

	/**
	 * 检测是否系统数据
	 * @return true 表示是系统数据
	 */
	public boolean checkSysAttr() {
		return Objects.nonNull(lowAttr) && DataConstant.LOW_ATTR_VALUE_SYSTEM == lowAttr;
	}

	/**
	 * 检测是否用户数据
	 * @return true 表示是用户数据
	 */
	public boolean checkUserAttr() {
		return Objects.nonNull(lowAttr) && DataConstant.LOW_ATTR_VALUE_USER == lowAttr;
	}

}
