package com.power4j.fist.jackson.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.power4j.fist.jackson.support.obfuscation.ObfuscatedStringDeserializer;
import com.power4j.fist.jackson.support.obfuscation.ObfuscatedStringSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Obfuscated value
 *
 * @author CJ (power4j@outlook.com)
 * @since 2022.1
 */
@JacksonAnnotationsInside
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
@JsonSerialize(using = ObfuscatedStringSerializer.class)
@JsonDeserialize(using = ObfuscatedStringDeserializer.class)
public @interface Obfuscation {

	/**
	 * Obfuscation mode, default value {@code "noop"}
	 * @return mode name to determine witch obfuscation processor should be used
	 */
	String mode() default "noop";

}
