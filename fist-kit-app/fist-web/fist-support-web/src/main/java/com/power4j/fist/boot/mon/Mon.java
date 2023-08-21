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

package com.power4j.fist.boot.mon;

import com.power4j.coca.kit.common.exception.WrappedException;
import com.power4j.fist.support.spring.util.SpringEventUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.function.FailableRunnable;
import org.apache.commons.lang3.function.FailableSupplier;
import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/18
 * @since 1.0
 */
public class Mon {

	private final List<Class<? extends Exception>> caught;

	@Nullable
	private final String description;

	/**
	 * 构造方法
	 * @param exceptions 需要捕获的异常类型
	 * @return Mon
	 */
	public static Mon guard(List<Class<? extends Exception>> exceptions) {
		return new Mon(Objects.requireNonNull(exceptions), null);
	}

	Mon(List<Class<? extends Exception>> caught, @Nullable String description) {
		this.caught = caught;
		this.description = description;
	}

	/**
	 * 设置场景描述信息
	 * @param info 描述信息
	 * @return 返回<b>新的Mon对象</b>
	 */
	public Mon description(String info) {
		return new Mon(this.caught, info);
	}

	/**
	 * 执行业务逻辑
	 * @param runnable 业务逻辑
	 * @throws RuntimeException 业务代码抛出RuntimeException时,继续抛出
	 * @throws WrappedException 业务代码抛出非RuntimeException时,转换为WrappedException后抛出
	 */
	public void run(FailableRunnable<Throwable> runnable) {
		try {
			runnable.run();
		}
		catch (RuntimeException e) {
			handleError(e);
			throw e;
		}
		catch (Exception e) {
			handleError(e);
			throw WrappedException.wrap(e);
		}
		catch (Throwable e) {
			throw WrappedException.wrap(e);
		}
	}

	/**
	 * 执行业务逻辑
	 * @param supplier 业务逻辑
	 * @param <T> 返回值类型
	 * @return T
	 * @throws RuntimeException 业务代码抛出RuntimeException时,继续抛出
	 * @throws WrappedException 业务代码抛出非RuntimeException时,转换为WrappedException后抛出
	 */
	public <T> T apply(FailableSupplier<T, Throwable> supplier) {
		try {
			return supplier.get();
		}
		catch (RuntimeException e) {
			handleError(e);
			throw e;
		}
		catch (Exception e) {
			handleError(e);
			throw WrappedException.wrap(e);
		}
		catch (Throwable e) {
			throw WrappedException.wrap(e);
		}
	}

	private void handleError(Exception e) {
		if (ObjectUtils.isEmpty(caught)) {
			return;
		}
		for (Class<? extends Exception> clazz : caught) {
			if (clazz.isAssignableFrom(e.getClass())) {
				SpringEventUtil.publishEvent(EventUtils.createServerErrorEvent(description, e));
				return;
			}
		}
	}

}
