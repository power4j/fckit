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

import com.power4j.fist.boot.mon.event.ServerErrorEvent;
import com.power4j.fist.boot.mon.info.ExceptionInfo;
import com.power4j.fist.boot.web.servlet.mvc.formatter.DateTimeParser;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/18
 * @since 1.0
 */
@Slf4j
public class DefaultServerErrorEventListener extends AbstractEventListener<ServerErrorEvent> {

	private final static String TAG = ServerErrorEvent.class.getSimpleName();

	@Override
	protected void handeEvent(ServerErrorEvent event) {
		// @formatter:off
		log.warn("[{}]:时间(UTC)={},应用={},异常类型 = {},异常信息 = {}",
				TAG,
				DateTimeParser.DEFAULT_DATETIME_FORMATTER.format(event.getTime()),
				event.getAppName(),
				Optional.ofNullable(event.getError()).map(ExceptionInfo::getEx).orElse(null),
				Optional.ofNullable(event.getError()).map(ExceptionInfo::getExMsg).orElse(null)
		);
		// @formatter:on
	}

}
