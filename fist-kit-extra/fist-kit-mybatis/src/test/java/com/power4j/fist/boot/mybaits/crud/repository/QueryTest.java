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

package com.power4j.fist.boot.mybaits.crud.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/27
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureTestDatabase
public class QueryTest {

	long ID90001 = 90001L;

	@Autowired
	private BookRepository bookRepository;

	@Test
	void findOneById() {
		Book book90001 = bookRepository.findOneById(ID90001).orElse(null);
		Assertions.assertNotNull(book90001);
	}

	@Test
	void existsById() {
		boolean exists1 = bookRepository.existsById(ID90001);
		Assertions.assertTrue(exists1);
		boolean exists2 = bookRepository.existsById(-1L);
		Assertions.assertFalse(exists2);
	}

	@Test
	void findAll() {
		List<Book> list = bookRepository.findAll();
		Assertions.assertEquals(5, list.size());
	}

	@Test
	void findAllBy() {
		LambdaQueryWrapper<Book> wrapper1 = bookRepository.lambdaWrapper().like(Book::getTitle, Book.TITLE_PREFIX);
		List<Book> list1 = bookRepository.findAllBy(wrapper1);
		Assertions.assertEquals(5, list1.size());

		LambdaQueryWrapper<Book> wrapper2 = bookRepository.lambdaWrapper().eq(Book::getTitle, Book.TITLE_PREFIX);
		List<Book> list2 = bookRepository.findAllBy(wrapper2);
		Assertions.assertEquals(0, list2.size());

		LambdaQueryWrapper<Book> wrapper3 = bookRepository.lambdaWrapper().in(Book::getId,
				Arrays.asList(ID90001, ID90001 + 1));
		List<Book> list3 = bookRepository.findAllBy(wrapper3);
		Assertions.assertEquals(2, list3.size());
	}

	@Test
	void findAllById() {
		List<Long> ids1 = Arrays.asList(ID90001, ID90001 + 1);
		List<Book> list1 = bookRepository.findAllById(ids1);
		Assertions.assertEquals(2, list1.size());

		List<Long> ids2 = Arrays.asList(ID90001, ID90001 + 10);
		List<Book> list2 = bookRepository.findAllById(ids2);
		Assertions.assertEquals(1, list2.size());
	}

	@Test
	void countAll() {
		long size = bookRepository.countAll();
		Assertions.assertEquals(5L, size);
	}

	@Test
	void findOneBy() {
		LambdaQueryWrapper<Book> wrapper1 = bookRepository.lambdaWrapper().eq(Book::getId, ID90001);
		Book book1 = bookRepository.findOneBy(wrapper1).orElse(null);
		Assertions.assertNotNull(book1);

		LambdaQueryWrapper<Book> wrapper2 = bookRepository.lambdaWrapper().eq(Book::getTitle, book1.getTitle());
		Book book2 = bookRepository.findOneBy(wrapper1).orElse(null);
		Assertions.assertNotNull(book2);
		Assertions.assertEquals(book1.getTitle(), book2.getTitle());
	}

}
