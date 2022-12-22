package com.power4j.fist.boot.autoconfigure.redisson.topic;

import com.power4j.fist.message.topic.TopicPublisher;
import com.power4j.fist.redisson.common.DefaultNameGenerator;
import com.power4j.fist.redisson.common.NameGenerator;
import com.power4j.fist.redisson.topic.DefaultTopicPublisher;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@RequiredArgsConstructor
@AutoConfiguration
@ConditionalOnClass({ Redisson.class })
@ConditionalOnBean(RedissonClient.class)
@AutoConfigureAfter(RedissonAutoConfiguration.class)
@EnableConfigurationProperties(TopicProperties.class)
public class RedissonTopicAutoConfiguration {

	private final ObjectProvider<Codec> codecObjectProvider;

	private final ObjectProvider<NameGenerator> nameCustomizerObjectProvider;

	@Bean
	TopicListenerBeanProcessor topicConsumerBeanProcessor(RedissonClient redisson) {
		NameGenerator customizer = nameCustomizerObjectProvider.getIfAvailable(DefaultNameGenerator::new);
		Codec codec = codecObjectProvider.getIfAvailable();
		return new TopicListenerBeanProcessor(customizer, redisson, codec);
	}

	@Bean
	TopicPublisher topicPublisher(RedissonClient redisson) {
		NameGenerator customizer = nameCustomizerObjectProvider.getIfAvailable(DefaultNameGenerator::new);
		Codec codec = codecObjectProvider.getIfAvailable();
		return new DefaultTopicPublisher(customizer, redisson, codec);
	}

}
