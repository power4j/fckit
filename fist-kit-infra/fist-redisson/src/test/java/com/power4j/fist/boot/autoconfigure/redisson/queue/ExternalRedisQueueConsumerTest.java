/*
 * Copyright 2026. ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.power4j.fist.boot.autoconfigure.redisson.queue;

import com.power4j.fist.message.queue.QueueConsumer;
import com.power4j.fist.redisson.common.DefaultNameGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.util.StringUtils;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 真实 Redis 队列订阅验证。默认跳过，避免本地测试依赖外部 Redis。
 */
@EnabledIfEnvironmentVariable(named = "FIST_IT_REDIS_URL", matches = ".+")
class ExternalRedisQueueConsumerTest {

	private RedissonClient redisson;

	private String queueName;

	@AfterEach
	void cleanQueue() {
		if (this.redisson == null) {
			return;
		}
		if (this.queueName != null) {
			this.redisson.getBlockingQueue(this.queueName).delete();
		}
		this.redisson.shutdown();
	}

	@Test
	void shouldConsumeMessageFromExternalRedisBlockingQueue() throws Exception {
		this.redisson = createRedisson();
		String originName = "fist-it-" + UUID.randomUUID();
		this.queueName = "queue:" + originName;
		TestConsumer consumer = new TestConsumer();
		QueueConsumerBeanProcessor processor = new QueueConsumerBeanProcessor(new DefaultNameGenerator(), this.redisson,
				null);

		processor.postProcessAfterInitialization(consumer, "testConsumer");
		RBlockingQueue<String> queue = this.redisson.getBlockingQueue(this.queueName);
		queue.add("hello");

		Assertions.assertTrue(consumer.await(), "Redis 队列订阅未在超时时间内收到消息");
		Assertions.assertEquals("hello", consumer.value);
	}

	private static RedissonClient createRedisson() {
		Config config = new Config();
		config.setPassword(blankToNull(System.getenv("FIST_IT_REDIS_PASSWORD")));
		config.useSingleServer()
			.setAddress(System.getenv("FIST_IT_REDIS_URL"))
			.setDatabase(Integer.parseInt(defaultIfBlank(System.getenv("FIST_IT_REDIS_DATABASE"), "0")));
		return Redisson.create(config);
	}

	private static String blankToNull(String value) {
		return StringUtils.hasText(value) ? value : null;
	}

	private static String defaultIfBlank(String value, String defaultValue) {
		return StringUtils.hasText(value) ? value : defaultValue;
	}

	public static class TestConsumer {

		private final CountDownLatch latch = new CountDownLatch(1);

		private String value;

		@QueueConsumer("test")
		public void handle(String value) {
			this.value = value;
			this.latch.countDown();
		}

		boolean await() throws InterruptedException {
			return this.latch.await(5, TimeUnit.SECONDS);
		}

	}

}
