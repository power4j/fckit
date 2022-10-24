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

package com.power4j.fist.boot.mon.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.scheduling.annotation.Async;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/18
 * @since 1.0
 */
@Slf4j
public abstract class AbstractEventListener<T> implements ApplicationListener<PayloadApplicationEvent<T>> {

	@Async
	@Override
	public void onApplicationEvent(PayloadApplicationEvent<T> event) {
		try {
			handeEvent(event.getPayload());
		}
		catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * 处理事件消息
	 * @param event 事件对象
	 */
	protected abstract void handeEvent(T event);

}
