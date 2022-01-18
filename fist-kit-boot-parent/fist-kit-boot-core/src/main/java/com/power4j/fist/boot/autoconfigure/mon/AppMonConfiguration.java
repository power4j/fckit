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

package com.power4j.fist.boot.autoconfigure.mon;

import com.power4j.fist.boot.mon.aspect.ReportErrorAspect;
import com.power4j.fist.boot.mon.listener.DefaultServerErrorEventListener;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.Aspects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/18
 * @since 1.0
 */
@RequiredArgsConstructor
@Configuration(proxyBeanMethods = false)
public class AppMonConfiguration {

	@Bean
	@ConditionalOnClass(Aspects.class)
	public ReportErrorAspect reportErrorAspect() {
		return new ReportErrorAspect();
	}

	@Bean
	@ConditionalOnMissingBean
	public DefaultServerErrorEventListener defaultServerErrorEventListener() {
		return new DefaultServerErrorEventListener();
	}

}
