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

package com.power4j.fist.boot.common.op


import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/12
 * @since 1.0
 */
class OpTemplateTest extends Specification {

	class BaseCase {
		static AtomicInteger SEQ = new AtomicInteger()
		int seq = Integer.MIN_VALUE
		void reset(){
			seq = Integer.MIN_VALUE
		}
		void incrSeq(){
			this.seq = SEQ.incrementAndGet()
		}
		boolean seqChanged(){
			return seq != Integer.MIN_VALUE
		}
	}

	class CheckStock extends BaseCase implements OpHandler<Book> {
		static String reason = "Sold out"
		@Override
		void handle(Book context) {
			incrSeq()
			if("Go" == context.getTitle()){
				System.out.println(reason + " : " + context.getTitle())
				throw new StockException(reason)
			}
		}
	}

	class CheckBan extends BaseCase implements OpHandler<Book> {
		static String reason = "Ban"
		@Override
		void handle(Book context) {
			incrSeq()
			if("Cpp" == context.getTitle()){
				System.out.println(reason + " : " + context.getTitle())
				throw new StockException(reason)
			}
		}
	}

	class Notify extends BaseCase implements OpHandler<Book> {
		@Override
		void handle(Book context) {
			incrSeq()
			String msg = String.format("%s price = %d",context.getTitle(),context.getPrice())
			System.out.println(msg)
		}
	}

	class Promo50Business implements Consumer<Book> {
		@Override
		void accept(Book book) {
			int newPrice = (book.getPrice()/2).intValue()
			book.setPrice(newPrice)
		}
	}

	Book cppPrimer = new Book("Cpp",20)
	Book goPrimer = new Book("Go",68)
	Book rustPrimer = new Book("Rust",88)
	CheckBan checkBan = new CheckBan()
	CheckStock checkStock = new CheckStock()
	Notify notify = new Notify()

	def "OpTemplate Test"(){
		given:
		HandlerCompose<Book> pre = new HandlerCompose<>(Arrays.asList(checkBan,checkStock))
		OpTemplate<Book> template = new OpTemplate<>(BookOps.Promo50,pre,notify)

		when:
		checkBan.reset()
		checkStock.reset()
		notify.reset()
		template.run(rustPrimer,new Promo50Business())

		then:
		rustPrimer.getPrice() == 44
		checkBan.seqChanged()
		checkStock.seqChanged()
		notify.seqChanged()

	}

	def "Skip invoke on exception"(){
		given:
		HandlerCompose<Book> pre = new HandlerCompose<>(Arrays.asList(checkBan,checkStock))
		OpTemplate<Book> template = new OpTemplate<>(BookOps.Promo50,pre,notify)

		when:
		checkBan.reset()
		checkStock.reset()
		notify.reset()
		template.run(goPrimer,new Promo50Business())

		then:
		def e1 = thrown(StockException)
		e1.message == CheckStock.reason
		checkBan.seqChanged()
		checkStock.seqChanged()
		!notify.seqChanged()

		when:
		checkBan.reset()
		checkStock.reset()
		notify.reset()
		template.run(cppPrimer,new Promo50Business())

		then:
		def e2 = thrown(StockException)
		e2.message == CheckBan.reason
		checkBan.seqChanged()
		!checkStock.seqChanged()
		!notify.seqChanged()
	}

	def "Invoke in order"(){

		given:
		HandlerCompose<Book> pre = new HandlerCompose<>(Arrays.asList(checkBan,checkStock))
		OpTemplate<Book> template = new OpTemplate<>(BookOps.Promo50,pre,notify)

		when:
		checkBan.reset()
		checkStock.reset()
		notify.reset()
		template.run(rustPrimer,new Promo50Business())

		then:
		checkBan.seq < checkStock.seq
		checkStock.seq < notify.seq
	}
}
