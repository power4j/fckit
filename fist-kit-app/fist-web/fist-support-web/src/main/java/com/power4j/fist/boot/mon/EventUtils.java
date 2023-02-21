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

package com.power4j.fist.boot.mon;

import com.power4j.coca.kit.common.datetime.DateTimeKit;
import com.power4j.fist.boot.mon.event.ServerErrorEvent;
import com.power4j.fist.boot.mon.info.ExceptionInfo;
import com.power4j.fist.boot.mon.info.InfoUtil;
import com.power4j.fist.boot.mon.info.TraceInfo;
import com.power4j.fist.support.spring.util.ApplicationContextHolder;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.lang.Nullable;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/18
 * @since 1.0
 */
@UtilityClass
public class EventUtils {

	private final static String KEY_APP_NAME = "spring.application.name";

	public ServerErrorEvent createServerErrorEvent(@Nullable String description, Throwable e) {
		String appName = getAppName().orElse("Unknown");
		ServerErrorEvent errorEvent = new ServerErrorEvent();
		errorEvent.setAppName(appName);
		errorEvent.setDescription(ObjectUtils.defaultIfNull(description, ""));
		errorEvent.setTime(DateTimeKit.utcNow());
		errorEvent.setEnvInfo(InfoUtil.getEnvInfo());
		errorEvent.setError(ExceptionInfo.from(e));
		errorEvent.setTraceInfo(new TraceInfo());

		return errorEvent;
	}

	public Optional<String> getAppName() {
		return ApplicationContextHolder.getContextOptional().map(o -> o.getEnvironment().getProperty(KEY_APP_NAME));
	}

}
