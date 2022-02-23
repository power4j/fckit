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

package com.power4j.fist.boot.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ClassUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/2/22
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class ReflectUtil {

	/**
	 * 查找注解,优先级:
	 * <ul>
	 * <li>子类方法上的注解</li>
	 * <li>父类方法上的注解</li>
	 * <li>Class上的注解</li>
	 * </ul>
	 * @param method 方法
	 * @param targetClass 类
	 * @param annotationClass 注解类
	 * @param <A> 注解类型
	 * @return Optional<A>
	 */
	public <A extends Annotation> Optional<A> findMostSpecificAnnotation(Method method, Class<?> targetClass,
			Class<A> annotationClass) {
		// The method may be on an interface, but we need attributes from the target
		// class.
		// If the target class is null, the method will be unchanged.
		Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
		A annotation = AnnotationUtils.findAnnotation(specificMethod, annotationClass);
		if (annotation != null) {
			if (log.isDebugEnabled()) {
				log.debug("{} found on specific method: {}", annotation, specificMethod);
			}
			return Optional.of(annotation);
		}
		// Check the original (e.g. interface) method
		if (specificMethod != method) {
			annotation = AnnotationUtils.findAnnotation(method, annotationClass);
			if (annotation != null) {
				if (log.isDebugEnabled()) {
					log.debug("{} found on: {}", annotation, method);
				}
				return Optional.of(annotation);
			}
		}
		// Check the class-level (note declaringClass, not targetClass, which may not
		// actually implement the method)
		annotation = AnnotationUtils.findAnnotation(specificMethod.getDeclaringClass(), annotationClass);
		if (annotation != null) {
			if (log.isDebugEnabled()) {
				log.debug("{} found on: {}", annotation, specificMethod.getDeclaringClass().getName());
			}
			return Optional.of(annotation);
		}
		return Optional.empty();
	}

}
