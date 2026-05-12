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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/12
 * @since 1.0
 */
class OpTemplateTest {

	private Book cppPrimer;

	private Book goPrimer;

	private Book rustPrimer;

	private CheckBan checkBan;

	private CheckStock checkStock;

	private Notify notify;

	@BeforeEach
	void setUp() {
		this.cppPrimer = new Book("Cpp", 20);
		this.goPrimer = new Book("Go", 68);
		this.rustPrimer = new Book("Rust", 88);
		this.checkBan = new CheckBan();
		this.checkStock = new CheckStock();
		this.notify = new Notify();
	}

	@Test
	void shouldRunOperation() {
		OpTemplate<Book> template = newTemplate();

		resetHandlers();
		template.run(this.rustPrimer, new Promo50Business());

		Assertions.assertEquals(44, this.rustPrimer.getPrice());
		Assertions.assertTrue(this.checkBan.seqChanged());
		Assertions.assertTrue(this.checkStock.seqChanged());
		Assertions.assertTrue(this.notify.seqChanged());
	}

	@Test
	void shouldSkipRemainingHandlersOnException() {
		OpTemplate<Book> template = newTemplate();

		resetHandlers();
		StockException stockException = Assertions.assertThrows(StockException.class,
				() -> template.run(this.goPrimer, new Promo50Business()));

		Assertions.assertEquals(CheckStock.REASON, stockException.getMessage());
		Assertions.assertTrue(this.checkBan.seqChanged());
		Assertions.assertTrue(this.checkStock.seqChanged());
		Assertions.assertFalse(this.notify.seqChanged());

		resetHandlers();
		StockException banException = Assertions.assertThrows(StockException.class,
				() -> template.run(this.cppPrimer, new Promo50Business()));

		Assertions.assertEquals(CheckBan.REASON, banException.getMessage());
		Assertions.assertTrue(this.checkBan.seqChanged());
		Assertions.assertFalse(this.checkStock.seqChanged());
		Assertions.assertFalse(this.notify.seqChanged());
	}

	@Test
	void shouldInvokeHandlersInOrder() {
		OpTemplate<Book> template = newTemplate();

		resetHandlers();
		template.run(this.rustPrimer, new Promo50Business());

		Assertions.assertTrue(this.checkBan.seq < this.checkStock.seq);
		Assertions.assertTrue(this.checkStock.seq < this.notify.seq);
	}

	private OpTemplate<Book> newTemplate() {
		HandlerCompose<Book> pre = new HandlerCompose<>(Arrays.asList(this.checkBan, this.checkStock));
		return new OpTemplate<>(BookOps.Promo50.getId(), pre, this.notify);
	}

	private void resetHandlers() {
		this.checkBan.reset();
		this.checkStock.reset();
		this.notify.reset();
	}

	private abstract static class BaseCase {

		private static final AtomicInteger SEQ = new AtomicInteger();

		protected int seq = Integer.MIN_VALUE;

		protected void reset() {
			this.seq = Integer.MIN_VALUE;
		}

		protected void incrSeq() {
			this.seq = SEQ.incrementAndGet();
		}

		protected boolean seqChanged() {
			return this.seq != Integer.MIN_VALUE;
		}

	}

	private static final class CheckStock extends BaseCase implements OpHandler<Book> {

		private static final String REASON = "Sold out";

		@Override
		public void handle(Book context) {
			incrSeq();
			if ("Go".equals(context.getTitle())) {
				throw new StockException(REASON);
			}
		}

	}

	private static final class CheckBan extends BaseCase implements OpHandler<Book> {

		private static final String REASON = "Ban";

		@Override
		public void handle(Book context) {
			incrSeq();
			if ("Cpp".equals(context.getTitle())) {
				throw new StockException(REASON);
			}
		}

	}

	private static final class Notify extends BaseCase implements OpHandler<Book> {

		@Override
		public void handle(Book context) {
			incrSeq();
		}

	}

	private static final class Promo50Business implements Consumer<Book> {

		@Override
		public void accept(Book book) {
			int newPrice = book.getPrice() / 2;
			book.setPrice(newPrice);
		}

	}

}
