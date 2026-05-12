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
import com.power4j.fist.redisson.common.NameGenerator;
import com.power4j.fist.redisson.common.RedisNamespace;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RBlockingQueue;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author CJ (power4j@outlook.com)
 * @since 4.0
 */
class QueueConsumerBeanProcessorTest {

	@Test
	@SuppressWarnings("unchecked")
	void shouldSubscribeQueueWithCompletionStageCallback() {
		RBlockingQueue<Object> queue = mock(RBlockingQueue.class);
		QueueConsumerBeanProcessor processor = new TestQueueConsumerBeanProcessor(queue);
		TestConsumer bean = new TestConsumer();

		processor.postProcessAfterInitialization(bean, "testConsumer");

		ArgumentCaptor<Function<Object, CompletionStage<Void>>> captor = ArgumentCaptor.forClass(Function.class);
		verify(queue).subscribeOnElements(captor.capture());

		CompletionStage<Void> stage = captor.getValue().apply("hello");

		assertThat(stage).isDone();
		assertThat(bean.value).isEqualTo("hello");
	}

	static class TestQueueConsumerBeanProcessor extends QueueConsumerBeanProcessor {

		private final RBlockingQueue<Object> queue;

		TestQueueConsumerBeanProcessor(RBlockingQueue<Object> queue) {
			super(new TestNameGenerator(), null, null);
			this.queue = queue;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected <T> RBlockingQueue<T> createQueue(String channel) {
			assertThat(channel).isEqualTo("queue:test");
			return (RBlockingQueue<T>) this.queue;
		}

	}

	static class TestNameGenerator implements NameGenerator {

		@Override
		public String generate(RedisNamespace type, String origin) {
			return type.getValue() + ":" + origin;
		}

	}

	public static class TestConsumer {

		private String value;

		@QueueConsumer("test")
		public void handle(String value) {
			this.value = value;
		}

	}

}
