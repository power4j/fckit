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

package com.power4j.fist.data.tree.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/23
 * @since 1.0
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class OrgNodeIdx extends BaseNodeIdx<Long, OrgNodeIdx> {

	@TableId(type = IdType.ASSIGN_ID)
	private Long id;

	public OrgNodeIdx() {
	}

	public OrgNodeIdx(long ancestor, long descendant, int distance) {
		super(ancestor, descendant, distance);
	}

}
