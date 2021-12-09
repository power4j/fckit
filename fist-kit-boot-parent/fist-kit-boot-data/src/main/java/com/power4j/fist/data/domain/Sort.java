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

package com.power4j.fist.data.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/18
 * @since 1.0
 */
public class Sort {

	public static final Direction DEFAULT_DIRECTION = Direction.ASC;

	public static final Sort UNSORTED = Sort.of(Collections.emptyList());

	private List<Order> orders = new ArrayList<>(2);

	public static Sort of(Collection<Order> orders) {
		return new Sort().orders(new ArrayList<>(orders));
	}

	public static Sort of(String... props) {
		return of(DEFAULT_DIRECTION, props);
	}

	public static Sort of(Direction direction, String... props) {
		List<Order> orders = Stream.of(props).map(prop -> new Order(prop, direction)).collect(Collectors.toList());
		return of(orders);
	}

	public Sort order(Order order) {
		orders.add(order);
		return this;
	}

	public Sort orders(List<Order> orders) {
		this.orders = orders;
		return this;
	}

	public List<Order> getOrders() {
		return orders;
	}

	public boolean isEmpty() {
		return orders.isEmpty();
	}

	public boolean isSorted() {
		return !isEmpty();
	}

	public boolean isUnsorted() {
		return !isSorted();
	}

	public static class Order {

		private final String prop;

		private final Direction direction;

		public Order(String prop, Direction direction) {
			this.prop = prop;
			this.direction = direction;
		}

		public String getProp() {
			return prop;
		}

		public Direction getDirection() {
			return direction;
		}

		public boolean isAsc() {
			return Direction.ASC.equals(direction);
		}

	}

	/**
	 * Enumeration for sort directions.
	 *
	 * @author Oliver Gierke
	 */
	public static enum Direction {

		/** 升序 */
		ASC,
		/** 降序 */
		DESC;

		public boolean isAscending() {
			return this.equals(ASC);
		}

		public boolean isDescending() {
			return this.equals(DESC);
		}

		/**
		 * Returns the {@link Direction} enum for the given {@link String} value.
		 * @param value String value to parse
		 * @throws IllegalArgumentException in case the given value cannot be parsed into
		 * an enum value.
		 * @return Direction
		 */
		public static Direction fromString(String value) {

			try {
				return Direction.valueOf(value.toUpperCase(Locale.US));
			}
			catch (Exception e) {
				throw new IllegalArgumentException(String.format(
						"Invalid value '%s' for orders given! Has to be either 'desc' or 'asc' (case insensitive).",
						value), e);
			}
		}

		/**
		 * Returns the {@link Direction} enum for the given {@link String} or null if it
		 * cannot be parsed into an enum value.
		 * @param value String value to parse
		 * @return Optional of Direction
		 */
		public static Optional<Direction> fromOptionalString(String value) {
			try {
				return Optional.of(fromString(value));
			}
			catch (IllegalArgumentException e) {
				return Optional.empty();
			}
		}

	}

}
