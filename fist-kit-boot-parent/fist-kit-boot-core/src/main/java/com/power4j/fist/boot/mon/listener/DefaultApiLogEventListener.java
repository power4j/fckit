/*
 * Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.power4j.fist.boot.mon.listener;

import com.power4j.fist.boot.mon.event.ApiLogEvent;
import com.power4j.fist.boot.web.servlet.mvc.formatter.DateTimeParser;
import lombok.extern.slf4j.Slf4j;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/3/31
 * @since 1.0
 */
@Slf4j
public class DefaultApiLogEventListener extends AbstractEventListener<ApiLogEvent> {

	private final static String TAG = ApiLogEvent.class.getSimpleName();

	@Override
	protected void handeEvent(ApiLogEvent event) {
		// @formatter:off
		log.debug("[{}]:{} {},请求时间(UTC)={},cost = {} ms",
				TAG,
				event.getAppName(),
				event.getOperation(),
				DateTimeParser.DEFAULT_DATETIME_FORMATTER.format(event.getTime()),
				event.getDuration().toMillis()
		);
		// @formatter:on
	}

}
