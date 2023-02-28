package com.power4j.fist.support.spring.i18n;

import com.power4j.fist.support.spring.util.ApplicationContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @since 2022.1
 */
@Slf4j
public class MsgUtil {

	/**
	 * 解析消息
	 * @param msgKey 消息键
	 * @param args 消息参数
	 * @param locale 国际化区域
	 * @return 解析失败返回empty
	 */
	public static Optional<String> resolveMessage(String msgKey, @Nullable Object[] args, Locale locale) {
		return ApplicationContextHolder.getContextOptional().map(m -> m.getMessage(msgKey, args, null, locale));
	}

	/**
	 * 解析消息,使用默认的国际化区域
	 * @param msgKey 消息键
	 * @param args 消息参数
	 * @return 解析失败返回empty
	 */
	public static Optional<String> resolveMessage(String msgKey, @Nullable Object[] args) {
		return resolveMessage(msgKey, args, Locale.getDefault());
	}

	/**
	 * 获取消息文本
	 * @param msgKey 消息键
	 * @param args 消息参数
	 * @param locale 国际化区域
	 * @return 无消息文本返回msgKey
	 */
	public static String getMessage(String msgKey, @Nullable Object[] args, Locale locale) {
		return resolveMessage(msgKey, args, locale).orElse(msgKey);
	}

	/**
	 * 获取消息文本,使用默认的国际化区域
	 * @param msgKey 消息键
	 * @param args 消息参数
	 * @return 无消息文本返回 msgKey
	 */
	public static String getMessage(String msgKey, @Nullable Object[] args) {
		return resolveMessage(msgKey, args).orElse(msgKey);
	}

}
