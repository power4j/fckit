package com.power4j.fist.boot.autoconfigure.redisson.queue;

import com.power4j.fist.boot.common.utils.TypeValidator;
import com.power4j.fist.message.queue.QueueConsumer;
import com.power4j.fist.redisson.common.NameGenerator;
import com.power4j.fist.redisson.common.RedisNamespace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Slf4j
@RequiredArgsConstructor
public class QueueConsumerBeanProcessor implements BeanPostProcessor {

	private final NameGenerator nameGenerator;

	private final RedissonClient redisson;

	@Nullable
	private final Codec codec;

	@Nullable
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		final Class<?> cls = ClassUtils.getUserClass(bean);
		ReflectionUtils.doWithMethods(cls, method -> {
			final QueueConsumer annotation = AnnotationUtils.findAnnotation(method, QueueConsumer.class);
			if (Objects.nonNull(annotation)) {
				final String name = annotation.value();
				Validate.notEmpty(name, "Queue name is required");
				if (!method.canAccess(bean)) {
					method.setAccessible(true);
				}
				Class<?>[] paramTypes = method.getParameterTypes();
				if (paramTypes.length != 1) {
					throw new IllegalArgumentException(String.format(
							"Method(%s) argument check fail, @%s Method must have one parameter,but %d found", method,
							QueueConsumer.class.getSimpleName(), paramTypes.length));
				}
				final TypeValidator typeValidator = TypeValidator.of(paramTypes[0]);
				final String channel = makeChannelName(name);
				RBlockingQueue<Object> queue = createQueue(channel);
				queue.subscribeOnElements(obj -> {
					if (log.isDebugEnabled()) {
						log.debug("[Queue {}] read message :{}", channel, obj);
					}
					if (obj != null && !typeValidator.castCheck(obj)) {
						throw new IllegalArgumentException(
								String.format("Method(%s) argument type check fail, want %s,got %s", method,
										typeValidator.getTarget(), obj.getClass().getName()));
					}
					ReflectionUtils.invokeMethod(method, bean, obj);
				});
			}
		}, ReflectionUtils.USER_DECLARED_METHODS);
		return bean;
	}

	protected String makeChannelName(String origin) {
		return nameGenerator.generate(RedisNamespace.QUEUE, origin);
	}

	protected <T> RBlockingQueue<T> createQueue(String channel) {
		if (null == codec) {
			return redisson.getBlockingQueue(channel);
		}
		else {
			return redisson.getBlockingQueue(channel, codec);
		}
	}

}
