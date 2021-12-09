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

package com.power4j.fist.boot.web.reactive.log;

import com.power4j.fist.boot.common.log.LogConstant;
import com.power4j.fist.boot.web.reactive.constant.ContextConstant;
import org.reactivestreams.Subscription;
import org.slf4j.MDC;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/21
 * @since 1.0
 */
public class MdcContextLifter<T> implements CoreSubscriber<T> {

	private final CoreSubscriber<T> coreSubscriber;

	public MdcContextLifter(CoreSubscriber<T> coreSubscriber) {
		this.coreSubscriber = coreSubscriber;
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		coreSubscriber.onSubscribe(subscription);
	}

	@Override
	public void onNext(T obj) {
		withMdc(() -> coreSubscriber.onNext(obj));
	}

	@Override
	public void onError(Throwable t) {
		withMdc(() -> coreSubscriber.onError(t));
	}

	@Override
	public void onComplete() {
		withMdc(coreSubscriber::onComplete);
	}

	@Override
	public Context currentContext() {
		return coreSubscriber.currentContext();
	}

	private void withMdc(Runnable runnable) {
		final Object requestIdVal = coreSubscriber.currentContext().getOrDefault(ContextConstant.KEY_MDC, null);
		if (Objects.nonNull(requestIdVal)) {
			try (MDC.MDCCloseable ignored = MDC.putCloseable(LogConstant.MDC_REQUEST_ID, requestIdVal.toString())) {
				runnable.run();
			}
		}
		else {
			runnable.run();
		}
	}

}
