package com.power4j.fist.message.topic;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the method consume topic message, for example: <pre>
 *     &#064;Service
 *     class Foo {
 *         &#064;TopicConsumer("my-topic")
 *         public void handleMessage(TopicEvent event){
 *             System.out.println(event.getPayload());
 *         }
 *     }
 * </pre> or <pre>
 *     &#064;Service
 *     class Foo {
 *         &#064;TopicConsumer("my-topic")
 *         public void handleMessage(){
 *             System.out.println("got event");
 *         }
 *     }
 * </pre>
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TopicConsumer {

	/**
	 * The topic name
	 */
	String value() default "";

}
