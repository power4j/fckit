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

package com.power4j.fist.boot.autoconfigure.i18n;

import com.power4j.fist.boot.common.error.Breaker;
import com.power4j.fist.boot.i18n.LocaleResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/15
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class MessageConfiguration implements InitializingBean {

	@Nullable
	private LocaleResolver localeResolver;

	@Nullable
	private MessageSource messageSource;

	@Autowired(required = false)
	public void setLocaleResolver(@Nullable LocaleResolver localeResolver) {
		this.localeResolver = localeResolver;
	}

	@Autowired(required = false)
	public void setMessageSource(@Nullable MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	private MessageSource buildInMessageSource() {
		ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
		messageSource.setBasenames("classpath:messages/messages");
		return messageSource;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		LocaleResolver resolver = localeResolver;
		if (Objects.isNull(resolver)) {
			resolver = LocaleContextHolder::getLocale;
		}

		MessageSource msgSource = messageSource;
		if (Objects.isNull(msgSource)) {
			msgSource = buildInMessageSource();
		}
		Breaker.setLocaleResolver(resolver);
		Breaker.setMessageSourceAccessor(new MessageSourceAccessor(msgSource));
	}

}
