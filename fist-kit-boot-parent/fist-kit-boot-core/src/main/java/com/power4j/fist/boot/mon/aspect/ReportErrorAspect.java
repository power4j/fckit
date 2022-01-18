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

package com.power4j.fist.boot.mon.aspect;

import com.power4j.fist.boot.mon.EventUtils;
import com.power4j.fist.boot.mon.annotation.ReportError;
import com.power4j.fist.boot.util.SpringEventUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/18
 * @since 1.0
 */
@Slf4j
@Aspect
public class ReportErrorAspect {

	@Around("@annotation(reportError)")
	public Object around(ProceedingJoinPoint point, ReportError reportError) throws Throwable {
		Class<? extends Exception>[] exceptions = reportError.errors();
		try {
			return point.proceed();
		}
		catch (Exception e) {
			try {
				handleError(e, exceptions);
			}
			catch (Throwable any) {
				log.error(e.getMessage(), e);
			}
			throw e;
		}
	}

	void handleError(Exception e, Class<? extends Exception>[] targets) {
		if (ObjectUtils.isEmpty(targets)) {
			return;
		}
		for (Class<? extends Exception> clazz : targets) {
			if (clazz.isAssignableFrom(e.getClass())) {
				SpringEventUtil.publishEvent(EventUtils.createServerErrorEvent(e));
				return;
			}
		}
	}

}
