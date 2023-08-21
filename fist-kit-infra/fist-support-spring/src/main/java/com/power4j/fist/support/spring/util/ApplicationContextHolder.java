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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.Nullable;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/2
 * @since 1.0
 */
@Slf4j
public class ApplicationContextHolder implements ApplicationContextAware, DisposableBean {

	private static final AtomicReference<ApplicationContext> CONTEXT_REF = new AtomicReference<>();

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (null != ApplicationContextHolder.CONTEXT_REF.getAndSet(applicationContext)) {
			log.info("Override ApplicationContext");
		}
	}

	public static Optional<ApplicationContext> getContextOptional() {
		return Optional.ofNullable(CONTEXT_REF.get());
	}

	public static ApplicationContext getContext() {
		return getContextOptional().orElseThrow(() -> new IllegalStateException("ApplicationContext is null"));
	}

	@Nullable
	public ApplicationContext peek() {
		return CONTEXT_REF.get();
	}

	@Override
	public void destroy() {
		log.info("Clear ApplicationContext");
		ApplicationContextHolder.CONTEXT_REF.set(null);
	}

}
