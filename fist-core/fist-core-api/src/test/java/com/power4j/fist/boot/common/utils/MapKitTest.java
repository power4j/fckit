package com.power4j.fist.boot.common.utils;

import org.apache.commons.lang3.reflect.TypeLiteral;
import org.apache.commons.lang3.reflect.Typed;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/9/9
 * @since 1.0
 */
class MapKitTest {

	// @formatter:off
	private final static Typed<Collection<Object>> OBJ_COLL = new TypeLiteral<>() {};
	private final static Typed<Collection<Integer>> INT_COLL = new TypeLiteral<>() {};
	private final static Typed<List<?>> OBJ_LIST = new TypeLiteral<>() {};
	private final static Typed<List<Integer>> INT_LIST = new TypeLiteral<>() {};
	private final static Typed<Set<Integer>> INT_SET = new TypeLiteral<>() {};
	// @formatter:on
	private static Map<String, Object> map = new HashMap<>();

	@BeforeAll
	public static void prepare() {
		map.put("int-100", 100);
		map.put("int-list", Arrays.asList(1, 2));
		map.put("int-array", new int[] { 3, 4 });
		map.put("str-list", Arrays.asList("5", "6"));
		map.put("null", null);
	}

	@Test
	public void useValueTest() {
		// 完全匹配
		Optional<?> val = MapKit.useValue(map, "int-list", INT_LIST);
		Assertions.assertTrue(val.isPresent());

		// 容器类型可转换
		val = MapKit.useValue(map, "int-list", INT_COLL);
		Assertions.assertTrue(val.isPresent());

		// 元素类型可转换
		val = MapKit.useValue(map, "int-list", OBJ_LIST);
		Assertions.assertTrue(val.isPresent());

		// 元素类型不可转换
		val = MapKit.useValue(map, "str-list", INT_LIST);
		Assertions.assertTrue(val.isPresent());

		// 容器和元素类型可转换
		val = MapKit.useValue(map, "int-list", OBJ_COLL);
		Assertions.assertTrue(val.isPresent());

		// 不存在
		val = MapKit.useValue(map, "xx", INT_LIST);
		Assertions.assertTrue(val.isEmpty());

		// null值
		val = MapKit.useValue(map, "null", INT_LIST);
		Assertions.assertTrue(val.isEmpty());

		// 容器类型无法转换
		Assertions.assertThrows(ClassCastException.class, () -> MapKit.useValue(map, "int-array", INT_LIST));
		Assertions.assertThrows(ClassCastException.class, () -> MapKit.useValue(map, "int-100", INT_LIST));

	}

	@Test
	public void readListValueTest() {
		List<Integer> intList = MapKit.readAsList(map, "int-100", o -> (Integer) o);
		Assertions.assertEquals(1, intList.size());
		Assertions.assertEquals(100, intList.get(0));

		intList = MapKit.readAsList(map, "int-list", o -> (Integer) o);
		Assertions.assertEquals(2, intList.size());
		Assertions.assertEquals(1, intList.get(0));
		Assertions.assertEquals(2, intList.get(1));

		intList = MapKit.readAsList(map, "int-array", o -> (Integer) o);
		Assertions.assertEquals(2, intList.size());
		Assertions.assertEquals(3, intList.get(0));
		Assertions.assertEquals(4, intList.get(1));

		intList = MapKit.readAsList(map, "str-list", o -> Integer.valueOf((String) o));
		Assertions.assertEquals(2, intList.size());
		Assertions.assertEquals(5, intList.get(0));
		Assertions.assertEquals(6, intList.get(1));
	}

	@Test
	public void readSetValueTest() {
		Set<Integer> intSet = MapKit.readAsSet(map, "int-list", o -> (Integer) o);
		Assertions.assertEquals(2, intSet.size());
		Assertions.assertTrue(intSet.contains(1));
		Assertions.assertTrue(intSet.contains(2));
	}

}