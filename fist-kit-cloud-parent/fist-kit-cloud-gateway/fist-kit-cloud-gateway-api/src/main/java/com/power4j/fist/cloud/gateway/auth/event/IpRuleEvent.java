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

package com.power4j.fist.cloud.gateway.auth.event;

import com.power4j.fist.cloud.gateway.auth.enums.IpListEnum;
import lombok.Data;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/16
 * @since 1.0
 */
@Data
public class IpRuleEvent {

	private final String sourceIp;

	private final IpListEnum type;

	private final LocalDateTime time;

	@Nullable
	private final String details;

	public IpRuleEvent(String sourceIp, IpListEnum type, LocalDateTime time, @Nullable String details) {
		this.type = type;
		this.sourceIp = sourceIp;
		this.time = time;
		this.details = details;
	}

}
