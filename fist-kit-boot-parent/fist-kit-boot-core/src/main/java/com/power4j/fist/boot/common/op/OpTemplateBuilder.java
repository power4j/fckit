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
import org.apache.commons.lang3.Validate;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

	private final Map<String, List<HandlerInfo<T>>> preHandlerMap = new ConcurrentHashMap<>(2);

	private final Map<String, List<HandlerInfo<T>>> postHandlerMap = new ConcurrentHashMap<>(2);

	@Nullable
	private List<HandlerInfo<T>> entry;

	@Nullable
	private String currentId;

	/**
	 * 建议根据实际情况使用自定义ExecutorService
	 */
	private ExecutorService asyncExecutor = ThreadUtil.newExecutor(cpuCore * 2, cpuCore * 8, 256);

	public OpTemplateBuilder(List<OpHandler<T>> knownHandlers) {
		this.knownHandlers = Objects.requireNonNull(knownHandlers);
	}

	public OpTemplateBuilder() {
		this(Collections.emptyList());
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

	public OpTemplateBuilder<T> add(OpHandler<T> handler) {
		Validate.notNull(handler);
		checkoutCurrentEntry().add(HandlerInfo.of(handler, false));
		return this;
	}

	public OpTemplateBuilder<T> addAsync(OpHandler<T> handler) {
		Validate.notNull(handler);
		checkoutCurrentEntry().add(HandlerInfo.of(handler, true));
		return this;
	}

	public OpTemplateBuilder<T> add(Class<? extends OpHandler<T>> clazz) {
		Validate.notNull(clazz);
		checkoutCurrentEntry().add(HandlerInfo.of(clazz, false));
		return this;
	}

	public OpTemplateBuilder<T> addAsync(Class<? extends OpHandler<T>> clazz) {
		Validate.notNull(clazz);
		checkoutCurrentEntry().add(HandlerInfo.of(clazz, true));
		return this;
	}

	public OpTemplateBuilder<T> define(String id) {
		this.currentId = id;
		this.entry = null;
		preHandlerMap.putIfAbsent(id, new ArrayList<>(2));
		postHandlerMap.putIfAbsent(id, new ArrayList<>(2));
		return this;
	}

	public Map<String, OpTemplate<T>> build() {
		entry = null;
		currentId = null;
		Set<String> idSet = new HashSet<>(preHandlerMap.keySet());
		idSet.addAll(postHandlerMap.keySet());
		return idSet.stream().collect(Collectors.toMap(o -> o, this::buildTemplate));
	}

	private OpTemplate<T> buildTemplate(String id) {
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

	private String checkoutCurrentId() {
		if (Objects.isNull(currentId)) {
			throw new IllegalStateException("Please call define() first");
		}
		return currentId;
	}

	private List<OpHandler<T>> prepareHandlerList(List<HandlerInfo<T>> infoList) {
		return infoList.stream().map(info -> {
			OpHandler<T> handler = Optional.ofNullable(info.getHandler())
					.orElseGet(() -> findHandler(info.getHandlerClass()));
			if (null == handler) {
				throw new OpTemplateException("No handler of " + info.getHandlerClass());
			}
			if (info.isAsync()) {
				return (OpHandler<T>) (context) -> {
					asyncExecutor.execute(() -> handler.handle(context));
				};
			}
			return handler;
		}).collect(Collectors.toList());
	}

	@Nullable
	private OpHandler<T> findHandler(@Nullable Class<? extends OpHandler<T>> clazz) {
		if (null == clazz) {
			return null;
		}
		return knownHandlers.stream().filter(h -> clazz.isAssignableFrom(h.getClass())).findFirst().orElse(null);
	}

	static class HandlerInfo<T> {

		@Nullable
		private final Class<? extends OpHandler<T>> handlerClass;

		@Nullable
		private final OpHandler<T> handler;

		private final boolean async;

		static <T> HandlerInfo<T> of(Class<? extends OpHandler<T>> handlerClass, boolean async) {
			return new HandlerInfo<>(handlerClass, async);
		}

		static <T> HandlerInfo<T> of(OpHandler<T> handler, boolean async) {
			return new HandlerInfo<>(handler, async);
		}

		HandlerInfo(Class<? extends OpHandler<T>> handlerClass, boolean async) {
			this.handlerClass = handlerClass;
			this.handler = null;
			this.async = async;
		}

		HandlerInfo(OpHandler<T> handler, boolean async) {
			this.handlerClass = null;
			this.handler = handler;
			this.async = async;
		}

		@Nullable
		public OpHandler<T> getHandler() {
			return handler;
		}

		@Nullable
		public Class<? extends OpHandler<T>> getHandlerClass() {
			return handlerClass;
		}

		public boolean isAsync() {
			return async;
		}

	}

}
