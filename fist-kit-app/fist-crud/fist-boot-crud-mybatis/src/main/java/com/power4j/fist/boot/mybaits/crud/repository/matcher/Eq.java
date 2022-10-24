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

package com.power4j.fist.boot.mybaits.crud.repository.matcher;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.lang.Nullable;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/18
 * @since 1.0
 */
@Data
@AllArgsConstructor
public class Eq<T> {

	private final SFunction<T, ?> column;

	@Nullable
	private final Object value;

	/**
	 * 辅助构造方法
	 * @param column 数据库字段求值函数,比如 {@code User::getName}
	 * @param value 值
	 * @param <T> 实体类型
	 * @return 返回新的Eq对象
	 */
	public static <T> Eq<T> of(SFunction<T, ?> column, @Nullable Object value) {
		return new Eq<>(column, value);
	}

}
