package com.power4j.fist.boot.mon.info;

import com.power4j.fist.boot.security.core.UserInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/6/6
 * @since 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthInfo {

	@Nullable
	private String username;

	@Nullable
	private String name;

	public static AuthInfo from(@Nullable UserInfo src) {
		AuthInfo info = new AuthInfo();
		if (Objects.nonNull(src)) {
			info.setName(src.getUsername());
			info.setName(src.getNickName());
		}
		return info;
	}

}
