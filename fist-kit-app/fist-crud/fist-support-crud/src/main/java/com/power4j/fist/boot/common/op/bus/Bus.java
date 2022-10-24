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

package com.power4j.fist.boot.common.op.bus;

import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/17
 * @since 1.0
 */
public class Bus<T, E extends OpEvent<T>> implements OpEventSource<T, E> {

	private List<OpEventSubscriber<T, E>> subscribers;

	@Nullable
	private ErrorHandler<E> errorHandler;

	public Bus() {
		this(new ArrayList<>(4), null);
	}

	public Bus(List<OpEventSubscriber<T, E>> subscribers, @Nullable ErrorHandler<E> errorHandler) {
		this.subscribers = Objects.requireNonNull(subscribers);
		this.errorHandler = errorHandler;
	}

	protected List<OpEventSubscriber<T, E>> getSubscribers() {
		return subscribers;
	}

	public void setSubscribers(List<OpEventSubscriber<T, E>> subscribers) {
		this.subscribers = subscribers;
	}

	public void setErrorHandler(@Nullable ErrorHandler<E> errorHandler) {
		this.errorHandler = errorHandler;
	}

	@Override
	public void fire(E event) {
		getSubscribers().forEach(observer -> doNotify(observer, event));
	}

	protected void doNotify(OpEventSubscriber<T, E> observer, E event) {
		try {
			observer.subscribe(event);
		}
		catch (Exception e) {
			if (Objects.nonNull(errorHandler)) {
				errorHandler.onError(e, event);
			}
			else {
				throw e;
			}
		}
	}

}
