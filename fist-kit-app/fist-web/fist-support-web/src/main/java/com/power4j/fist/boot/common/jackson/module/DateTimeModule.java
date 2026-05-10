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

package com.power4j.fist.boot.common.jackson.module;

import com.power4j.coca.kit.common.datetime.DateTimePattern;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.MonthDayDeserializer;
import tools.jackson.databind.ext.javatime.deser.YearDeserializer;
import tools.jackson.databind.ext.javatime.deser.YearMonthDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.MonthDaySerializer;
import tools.jackson.databind.ext.javatime.ser.YearMonthSerializer;
import tools.jackson.databind.ext.javatime.ser.YearSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/12/16
 * @since 1.0
 */
public class DateTimeModule extends SimpleModule {

	public DateTimeModule() {
		super(DateTimeModule.class.getName());

		addSerializer(Year.class, new YearSerializer(DateTimeFormatter.ofPattern(DateTimePattern.YEAR)));
		addDeserializer(Year.class, new YearDeserializer(DateTimeFormatter.ofPattern(DateTimePattern.YEAR)));

		addSerializer(LocalDate.class, new LocalDateSerializer(DateTimeFormatter.ofPattern(DateTimePattern.DATE)));
		addDeserializer(LocalDate.class, new LocalDateDeserializer(DateTimeFormatter.ofPattern(DateTimePattern.DATE)));

		addSerializer(LocalTime.class, new LocalTimeSerializer(DateTimeFormatter.ofPattern(DateTimePattern.TIME)));
		addDeserializer(LocalTime.class, new LocalTimeDeserializer(DateTimeFormatter.ofPattern(DateTimePattern.TIME)));

		addSerializer(LocalDateTime.class,
				new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DateTimePattern.DATETIME)));
		addDeserializer(LocalDateTime.class,
				new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DateTimePattern.DATETIME)));

		addSerializer(YearMonth.class,
				new YearMonthSerializer(DateTimeFormatter.ofPattern(DateTimePattern.YEAR_MONTH)));
		addDeserializer(YearMonth.class,
				new YearMonthDeserializer(DateTimeFormatter.ofPattern(DateTimePattern.YEAR_MONTH)));

		addSerializer(MonthDay.class, new MonthDaySerializer(DateTimeFormatter.ofPattern(DateTimePattern.MONTH_DAY)));
		addDeserializer(MonthDay.class,
				new MonthDayDeserializer(DateTimeFormatter.ofPattern(DateTimePattern.MONTH_DAY)));
	}

}
