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

package com.power4j.fist.boot.mybaits.handler.column;

import com.power4j.coca.kit.common.text.StringPool;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/23
 * @since 1.0
 */
@UtilityClass
public class StringEncode {

	public String encode(Collection<String> list, String separator) {
		if (ObjectUtils.isEmpty(list)) {
			return StringPool.EMPTY;
		}
		for (String str : list) {
			if (str.contains(separator)) {
				throw new IllegalArgumentException(String.format("集合类型转换错误,入参数据不能包含分隔符: '%s', -> %s", separator, str));
			}
		}
		return StringUtils.join(list, separator);
	}

	public Set<String> decodeSet(String raw, String separator) {
		Set<String> ret = new HashSet<>();
		if (Objects.nonNull(raw) && !raw.isEmpty()) {
			Collections.addAll(ret, StringUtils.split(raw, separator));
		}
		return ret;
	}

	public List<String> decodeList(String raw, String separator) {
		List<String> ret = new ArrayList<>();
		if (Objects.nonNull(raw) && !raw.isEmpty()) {
			Collections.addAll(ret, StringUtils.split(raw, separator));
		}
		return ret;
	}

}
