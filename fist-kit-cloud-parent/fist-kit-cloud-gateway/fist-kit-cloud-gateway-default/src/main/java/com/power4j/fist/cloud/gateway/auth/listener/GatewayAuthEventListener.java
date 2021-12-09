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

package com.power4j.fist.cloud.gateway.auth.listener;

import com.power4j.fist.cloud.gateway.auth.event.GatewayAuthEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/8/11
 * @since 1.0
 */
@Slf4j
@Component
public class GatewayAuthEventListener {

	@EventListener
	public void handleAuthEvent(GatewayAuthEvent event) {
		if (!event.getPass()) {
			log.warn("Gateway auth fail,code = {}, msg = {} , request = {}", event.getCode(), event.getMsg(),
					event.getRequestUri());
		}
		else {
			if (log.isTraceEnabled()) {
				log.trace("Gateway auth success,code = {}, msg = {} , request = {}", event.getCode(), event.getMsg(),
						event.getRequestUri());
			}
		}
	}

}
