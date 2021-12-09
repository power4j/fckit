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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.power4j.fist.data.constant.DataConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 简单逻辑删除支持
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/7/1
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class LogicDelEntity extends VersionedEntity {

	@TableLogic(value = DataConstant.FLAG_NOT_DELETED, delval = DataConstant.FLAG_DELETED)
	@TableField(DataConstant.COLUMN_LOGIC_DEL)
	private String delFlag;

}
