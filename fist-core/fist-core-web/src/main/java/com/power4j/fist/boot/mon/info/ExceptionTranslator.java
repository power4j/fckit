package com.power4j.fist.boot.mon.info;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/23
 * @since 1.0
 */
public interface ExceptionTranslator {

	/**
	 * 异常转换
	 * @param e 异常
	 * @return 返回empty表示异常不可转换
	 */
	Optional<ApiResponseInfo> translateException(Throwable e);

}
