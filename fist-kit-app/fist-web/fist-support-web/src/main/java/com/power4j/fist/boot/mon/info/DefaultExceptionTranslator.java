package com.power4j.fist.boot.mon.info;

import com.power4j.fist.boot.common.error.MsgBundleRejectedException;
import com.power4j.fist.boot.common.error.RejectedException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.lang.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/23
 * @since 1.0
 */
@RequiredArgsConstructor
public class DefaultExceptionTranslator implements ExceptionTranslator {

	@Nullable
	private final MessageSourceAccessor messageSourceAccessor;

	@Override
	public Optional<ApiResponseInfo> translateException(Throwable e) {
		if (e instanceof MsgBundleRejectedException) {
			MsgBundleRejectedException exception = (MsgBundleRejectedException) e;
			ApiResponseInfo responseInfo = new ApiResponseInfo();
			responseInfo.setCode(exception.getCode());
			responseInfo.setMessage(getMessage(exception));
			return Optional.of(responseInfo);
		}
		else if (e instanceof RejectedException) {
			RejectedException rejectedException = (RejectedException) e;
			ApiResponseInfo responseInfo = new ApiResponseInfo();
			responseInfo.setCode(rejectedException.getCode());
			responseInfo.setMessage(rejectedException.getMessage());
			return Optional.of(responseInfo);
		}
		return Optional.empty();
	}

	String getMessage(MsgBundleRejectedException exception) {
		if (Objects.nonNull(messageSourceAccessor)) {
			return messageSourceAccessor.getMessage(exception.getMsgKey(), exception.getMsgArg(), exception.getMsgKey(),
					Locale.CHINA);
		}
		return exception.getMsgKey();
	}

}
