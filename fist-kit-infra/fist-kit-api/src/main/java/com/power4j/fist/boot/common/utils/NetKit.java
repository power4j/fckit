package com.power4j.fist.boot.common.utils;

import com.google.common.net.InetAddresses;
import lombok.experimental.UtilityClass;

import java.net.InetAddress;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/9/11
 * @since 1.0
 */
@UtilityClass
public class NetKit {

	public InetAddress parse(String ip) {
		return InetAddresses.forString(ip);
	}

}
