package com.power4j.fist.message.queue;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the method consume object form queue, for example: <pre>
 *     &#064;Service
 *     class Foo {
 *         &#064;QueueConsumer("my-queue")
 *         public void handleMessage(MyData data){
 *             System.out.println(data);
 *         }
 *     }
 * </pre>
 *
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueueConsumer {

	/**
	 * The queue name
	 */
	String value() default "";

}
