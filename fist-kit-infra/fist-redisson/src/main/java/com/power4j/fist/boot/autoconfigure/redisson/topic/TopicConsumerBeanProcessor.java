package com.power4j.fist.boot.autoconfigure.redisson.topic;

import com.power4j.fist.message.topic.TopicConsumer;
import com.power4j.fist.message.topic.TopicEvent;
import com.power4j.fist.redisson.common.NameGenerator;
import com.power4j.fist.redisson.common.RedisNamespace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.redisson.api.RTopic;
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
public class TopicConsumerBeanProcessor implements BeanPostProcessor {

	private final NameGenerator nameGenerator;

	private final RedissonClient redisson;

	@Nullable
	private final Codec codec;

	@Nullable
	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		final Class<?> cls = ClassUtils.getUserClass(bean);
		ReflectionUtils.doWithMethods(cls, method -> {
			final TopicConsumer annotation = AnnotationUtils.findAnnotation(method, TopicConsumer.class);
			if (Objects.nonNull(annotation)) {
				final String name = annotation.value();
				Validate.notEmpty(name, "Topic name is required");
				if (!method.canAccess(bean)) {
					method.setAccessible(true);
				}
				final int parameterCount = method.getParameterCount();
				if (parameterCount > 1) {
					throw new IllegalArgumentException(String.format(
							" Method(%s) argument check fail, @%s Method must have zero or one parameter,but %d found",
							method, TopicConsumer.class.getSimpleName(), parameterCount));
				}
				RTopic topic = createTopic(makeChannelName(name));
				topic.addListener(Object.class, (ch, msg) -> {
					if (log.isDebugEnabled()) {
						log.debug("[Topic {}] read message :{}", ch, msg);
					}
					if (parameterCount == 0) {
						ReflectionUtils.invokeMethod(method, bean);
					}
					else {
						ReflectionUtils.invokeMethod(method, bean, new TopicEvent<>(ch, msg));
					}
				});
			}
		}, ReflectionUtils.USER_DECLARED_METHODS);
		return bean;
	}

	protected String makeChannelName(String origin) {
		return nameGenerator.generate(RedisNamespace.TOPIC, origin);
	}

	protected RTopic createTopic(String channel) {
		if (null == codec) {
			return redisson.getTopic(channel);
		}
		else {
			return redisson.getTopic(channel, codec);
		}
	}

}
