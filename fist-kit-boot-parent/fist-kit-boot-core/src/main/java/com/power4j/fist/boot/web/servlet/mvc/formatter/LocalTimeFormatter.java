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

package com.power4j.fist.boot.web.servlet.mvc.formatter;

import org.springframework.format.Formatter;

import java.text.ParseException;
import java.time.LocalTime;
import java.util.Locale;

/**
 * LocalTime 解析
 * <p>
 *
 * @author CJ (power4j@outlook.com)
 * @date 2020-11-17
 * @since 1.0
 */
public class LocalTimeFormatter implements Formatter<LocalTime> {

	@Override
	public LocalTime parse(String text, Locale locale) throws ParseException {
		return DateTimeParser.parseTime(text).orElseThrow(() -> new ParseException(text, 0));
	}

	@Override
	public String print(LocalTime object, Locale locale) {
		return DateTimeParser.DEFAULT_TIME_FORMATTER.format(object);
	}

}
