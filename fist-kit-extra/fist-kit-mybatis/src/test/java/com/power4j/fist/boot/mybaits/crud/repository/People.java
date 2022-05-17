package com.power4j.fist.boot.mybaits.crud.repository;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.function.Function;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/17
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class People {

	public static String NAME_PREFIX = "P-";

	public static Function<Long, String> NAME_GEN = id -> NAME_PREFIX + id;

	@TableId(type = IdType.INPUT)
	private Long id;

	private String name;

	public static People of(long id) {
		return new People(id, NAME_GEN.apply(id));
	}

}
