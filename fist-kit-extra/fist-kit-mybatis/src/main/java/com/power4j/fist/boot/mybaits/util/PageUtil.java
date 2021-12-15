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

package com.power4j.fist.boot.mybaits.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.power4j.fist.boot.web.model.PageParameter;
import com.power4j.fist.data.domain.PageImpl;
import com.power4j.fist.data.domain.PageRequest;
import com.power4j.fist.data.domain.Pageable;
import com.power4j.fist.data.domain.Paged;
import com.power4j.fist.data.domain.Sort;
import lombok.experimental.UtilityClass;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/22
 * @since 1.0
 */
@UtilityClass
public class PageUtil {

	// ~ Web
	// ===================================================================================================

	public <T> Page<T> toPage(PageParameter parameter) {
		return new Page<>(parameter.getPageNumber() + 1, parameter.getPageSize());
	}

	// ~ Domain
	// ===================================================================================================

	public <T> Page<T> toPage(Pageable pageable) {
		Page<T> page = new Page<>(pageable.getPageNumber() + 1, pageable.getPageSize());
		pageable.getSort().getOrders().forEach(order -> {
			page.addOrder(toOrderItem(order));
		});
		return page;
	}

	public <T> Paged<T> toPaged(IPage<T> page) {
		return new PageImpl<>(page.getRecords(), toPageRequest(page), page.getTotal());
	}

	public PageRequest toPageRequest(IPage<?> page) {
		List<Sort.Order> orders = page.orders().stream().map(PageUtil::toOrder).collect(Collectors.toList());
		int pageNum = (int) (page.getCurrent() - 1);
		int size = (int) (page.getSize());
		return PageRequest.of(pageNum, size, Sort.of(orders));
	}

	public OrderItem toOrderItem(Sort.Order order) {
		return order.isAsc() ? OrderItem.asc(order.getProp()) : OrderItem.desc(order.getProp());
	}

	public Sort.Order toOrder(OrderItem item) {
		return new Sort.Order(item.getColumn(), item.isAsc() ? Sort.Direction.ASC : Sort.Direction.DESC);
	}

}
