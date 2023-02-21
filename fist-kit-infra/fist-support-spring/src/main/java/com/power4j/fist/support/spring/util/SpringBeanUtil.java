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

package com.power4j.fist.support.spring.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/2
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class SpringBeanUtil {

	public <T> T getBean(Class<T> type) {
		return ApplicationContextHolder.getContext().getBean(type);
	}

	public <T> T getBean(String name, Class<T> type) {
		return ApplicationContextHolder.getContext().getBean(name, type);
	}

	public <T> Optional<T> getBeanIfExist(String name, Class<T> type) {
		try {
			return Optional.of(ApplicationContextHolder.getContext().getBean(name, type));
		}
		catch (BeansException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage(), e);
			}
			return Optional.empty();
		}
	}

	public <T> Optional<T> getBeanIfExist(Class<T> type) {
		try {
			return Optional.of(ApplicationContextHolder.getContext().getBean(type));
		}
		catch (BeansException e) {
			if (log.isDebugEnabled()) {
				log.debug(e.getMessage(), e);
			}
			return Optional.empty();
		}
	}

}
