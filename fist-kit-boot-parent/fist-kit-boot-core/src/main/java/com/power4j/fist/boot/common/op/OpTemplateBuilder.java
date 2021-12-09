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

package com.power4j.fist.boot.common.op;

import cn.hutool.core.thread.ThreadUtil;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/12
 * @since 1.0
 */
public class OpTemplateBuilder<T> {

	private final int cpuCore = Runtime.getRuntime().availableProcessors();

	private final List<OpHandler<T>> knownHandlers;

	private final Map<TemplateId, List<HandlerInfo<T>>> preHandlerMap = new ConcurrentHashMap<>(2);

	private final Map<TemplateId, List<HandlerInfo<T>>> postHandlerMap = new ConcurrentHashMap<>(2);

	@Nullable
	private List<HandlerInfo<T>> entry;

	@Nullable
	private TemplateId currentId;

	/**
	 * 建议根据实际情况使用自定义ExecutorService
	 */
	private ExecutorService asyncExecutor = ThreadUtil.newExecutor(cpuCore * 2, cpuCore * 8, 256);

	public OpTemplateBuilder(List<OpHandler<T>> knownHandlers) {
		this.knownHandlers = Objects.requireNonNull(knownHandlers);
	}

	public OpTemplateBuilder<T> asyncExecutor(ExecutorService executorService) {
		this.asyncExecutor = executorService;
		return this;
	}

	public OpTemplateBuilder<T> pre() {
		entry = preHandlerMap.get(checkoutCurrentId());
		return this;
	}

	public OpTemplateBuilder<T> post() {
		entry = postHandlerMap.get(checkoutCurrentId());
		return this;
	}

	public OpTemplateBuilder<T> add(Class<? extends OpHandler<T>> clazz) {
		checkoutCurrentEntry().add(HandlerInfo.of(clazz, false));
		return this;
	}

	public OpTemplateBuilder<T> addAsync(Class<? extends OpHandler<T>> clazz) {
		checkoutCurrentEntry().add(HandlerInfo.of(clazz, true));
		return this;
	}

	public OpTemplateBuilder<T> define(TemplateId id) {
		this.currentId = id;
		this.entry = null;
		preHandlerMap.putIfAbsent(id, new ArrayList<>(2));
		postHandlerMap.putIfAbsent(id, new ArrayList<>(2));
		return this;
	}

	public Map<TemplateId, OpTemplate<T>> build() {
		entry = null;
		currentId = null;
		Set<TemplateId> idSet = new HashSet<>(preHandlerMap.keySet());
		idSet.addAll(postHandlerMap.keySet());
		return idSet.stream().collect(Collectors.toMap(o -> o, this::buildTemplate));
	}

	private OpTemplate<T> buildTemplate(TemplateId id) {
		HandlerCompose<T> pre = new HandlerCompose<>(
				prepareHandlerList(preHandlerMap.getOrDefault(id, Collections.emptyList())));
		HandlerCompose<T> post = new HandlerCompose<>(
				prepareHandlerList(postHandlerMap.getOrDefault(id, Collections.emptyList())));
		return new OpTemplate<>(Objects.requireNonNull(currentId), pre, post);
	}

	private List<HandlerInfo<T>> checkoutCurrentEntry() {
		if (Objects.isNull(entry)) {
			throw new IllegalStateException("Please call pre() or post() first");
		}
		return entry;
	}

	private TemplateId checkoutCurrentId() {
		if (Objects.isNull(currentId)) {
			throw new IllegalStateException("Please call define() first");
		}
		return currentId;
	}

	private List<OpHandler<T>> prepareHandlerList(List<HandlerInfo<T>> infoList) {
		return infoList.stream().map(info -> {
			final Class<? extends OpHandler<T>> target = info.getHandlerClass();
			OpHandler<T> handler = knownHandlers.stream().filter(h -> target.isAssignableFrom(h.getClass())).findFirst()
					.orElseThrow(() -> new OpTemplateException("No instance for " + info.getHandlerClass()));
			if (info.isAsync()) {
				return (OpHandler<T>) (context) -> {
					asyncExecutor.execute(() -> handler.handle(context));
				};
			}
			return handler;
		}).collect(Collectors.toList());
	}

	static class HandlerInfo<T> {

		private final Class<? extends OpHandler<T>> handlerClass;

		private final boolean async;

		static <T> HandlerInfo<T> of(Class<? extends OpHandler<T>> handlerClass, boolean async) {
			return new HandlerInfo<>(handlerClass, async);
		}

		HandlerInfo(Class<? extends OpHandler<T>> handlerClass, boolean async) {
			this.handlerClass = handlerClass;
			this.async = async;
		}

		public Class<? extends OpHandler<T>> getHandlerClass() {
			return handlerClass;
		}

		public boolean isAsync() {
			return async;
		}

	}

}
