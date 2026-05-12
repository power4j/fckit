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

package com.power4j.fist.boot.common.jackson;

import com.power4j.fist.boot.common.jackson.module.DateTimeModule;
import com.power4j.fist.boot.common.jackson.module.NumberStrModule;
import com.power4j.fist.jackson.support.obfuscation.NoopStringObfuscate;
import com.power4j.fist.jackson.support.obfuscation.ObfuscateProcessorProvider;
import com.power4j.fist.jackson.support.obfuscation.ObfuscatedAnnotationIntrospector;
import com.power4j.fist.jackson.support.obfuscation.SimpleStringObfuscate;
import com.power4j.fist.jackson.support.obfuscation.StringObfuscate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.introspect.AnnotationIntrospectorPair;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/12/16
 * @since 1.0
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JsonMapper.class)
@ConditionalOnProperty(prefix = JacksonCustomizeProperties.PROP_PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(JacksonCustomizeProperties.class)
@AutoConfigureBefore(JacksonAutoConfiguration.class)
@RequiredArgsConstructor
public class JacksonConfig {

	private final JacksonCustomizeProperties jacksonCustomizeProperties;

	private final static Map<JacksonCustomizeProperties.ModuleName, JacksonModule> MODULE_MAP = new HashMap<>(2);
	static {
		MODULE_MAP.put(JacksonCustomizeProperties.ModuleName.Date, new DateTimeModule());
		MODULE_MAP.put(JacksonCustomizeProperties.ModuleName.NumberToStr, new NumberStrModule());
	}

	@Order
	@Bean
	@ConditionalOnMissingBean
	NoopStringObfuscate noopStringObfuscate() {
		return new NoopStringObfuscate();
	}

	@Order
	@Bean
	@ConditionalOnMissingBean
	SimpleStringObfuscate simpleStringObfuscate() {
		return SimpleStringObfuscate.ofDefault();
	}

	@Bean
	@ConditionalOnMissingBean
	ObfuscateProcessorProvider obfuscateProcessorProvider(List<StringObfuscate> processors) {
		return new ObfuscateProcessorBeanRegistry(processors);
	}

	@Bean
	@Order(10_000)
	public JsonMapperBuilderCustomizer customizer(ObfuscateProcessorProvider obfuscateProcessorProvider) {
		return builder -> {
			builder.defaultLocale(Locale.CHINA);
			applyTimeZone(builder);
			applySimpleDateFormat(builder);
			applyModules(builder);
			if (jacksonCustomizeProperties.isObfuscatedSupport()) {
				applyExtra(builder, obfuscateProcessorProvider);
			}
		};
	}

	private void applyTimeZone(JsonMapper.Builder builder) {
		final String timeZoneId = jacksonCustomizeProperties.getTimeZoneId();
		if (StringUtils.isEmpty(timeZoneId)) {
			return;
		}
		log.info("Set time zone :{}", timeZoneId);
		if (Strings.CI.equals(JacksonCustomizeProperties.TZ_SYSTEM, timeZoneId)) {
			builder.defaultTimeZone(TimeZone.getTimeZone(ZoneId.systemDefault()));
			return;
		}
		builder.defaultTimeZone(TimeZone.getTimeZone(ZoneId.of(timeZoneId)));
	}

	private void applySimpleDateFormat(JsonMapper.Builder builder) {
		final String format = jacksonCustomizeProperties.getSimpleDateFormat();
		if (!StringUtils.isEmpty(format)) {
			log.info("Set simple date format :{}", format);
			builder.defaultDateFormat(new SimpleDateFormat(format));
		}
	}

	private void applyModules(JsonMapper.Builder builder) {
		List<JacksonCustomizeProperties.ModuleName> excludeModules = ObjectUtils
			.getIfNull(jacksonCustomizeProperties.getExcludeModules(), Collections.emptyList());
		List<JacksonModule> modules = MODULE_MAP.entrySet()
			.stream()
			.filter(kv -> !excludeModules.contains(kv.getKey()))
			.map(Map.Entry::getValue)
			.toList();
		if (!modules.isEmpty()) {
			List<String> names = modules.stream().map(JacksonModule::getModuleName).collect(Collectors.toList());
			log.info("Install modules: {}", names);
			builder.addModules(modules);
		}
	}

	private void applyExtra(JsonMapper.Builder builder, ObfuscateProcessorProvider obfuscateProcessorProvider) {
		log.info("Install extra Serializer/Deserializer");
		builder.annotationIntrospector(AnnotationIntrospectorPair.create(builder.annotationIntrospector(),
				new ObfuscatedAnnotationIntrospector(obfuscateProcessorProvider)));
	}

	static class ObfuscateProcessorBeanRegistry implements ObfuscateProcessorProvider {

		private final List<StringObfuscate> processors;

		ObfuscateProcessorBeanRegistry(List<StringObfuscate> processors) {
			this.processors = processors;
		}

		@Override
		public Optional<StringObfuscate> getInstance(String mode) {
			for (StringObfuscate processor : processors) {
				if (mode.equals(processor.modeId())) {
					return Optional.of(processor);
				}
			}
			return Optional.empty();
		}

	}

}
