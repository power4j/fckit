package com.power4j.fist.boot.autoconfigure.redisson.queue;

import com.power4j.fist.message.queue.QueueWriter;
import com.power4j.fist.redisson.common.DefaultNameGenerator;
import com.power4j.fist.redisson.common.NameGenerator;
import com.power4j.fist.redisson.queue.BlockQueueWriter;
import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.spring.starter.RedissonAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
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
@AutoConfigureAfter(RedissonAutoConfiguration.class)
@EnableConfigurationProperties(QueueProperties.class)
public class RedissonQueueAutoConfiguration {

	private final RedissonClient redisson;

	private final ObjectProvider<Codec> codecObjectProvider;

	private final ObjectProvider<NameGenerator> nameCustomizerObjectProvider;

	@Bean
	QueueWriter queueWriter() {
		NameGenerator customizer = nameCustomizerObjectProvider.getIfAvailable(DefaultNameGenerator::new);
		Codec codec = codecObjectProvider.getIfAvailable();
		return new BlockQueueWriter(customizer, redisson, codec);
	}

	@Bean
	QueueConsumerBeanProcessor queueConsumerBeanProcessor() {
		NameGenerator customizer = nameCustomizerObjectProvider.getIfAvailable(DefaultNameGenerator::new);
		Codec codec = codecObjectProvider.getIfAvailable();
		return new QueueConsumerBeanProcessor(customizer, redisson, codec);
	}

}
