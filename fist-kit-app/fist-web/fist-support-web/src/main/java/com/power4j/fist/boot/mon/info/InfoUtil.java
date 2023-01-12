package com.power4j.fist.boot.mon.info;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
@Slf4j
@UtilityClass
public class InfoUtil {

	private static final AtomicReference<EnvInfo> ENV_INFO_CACHE = new AtomicReference<>(null);

	static {
		CompletableFuture.runAsync(InfoUtil::getEnvInfo);
	}

	public EnvInfo getEnvInfo() {
		return ENV_INFO_CACHE.updateAndGet(o -> ObjectUtils.defaultIfNull(o, resolveEnvInfo()));
	}

	/**
	 * 解析环境信息,可能会阻塞线程,推荐使用 {@code getEnvInfo}
	 * @return EnvInfo
	 */
	public EnvInfo resolveEnvInfo() {

		EnvInfo info = new EnvInfo();
		info.setHostName(getLocalHostName().orElse(null));
		info.setOsName(System.getProperty("os.name"));
		info.setOsVersion(System.getProperty("os.version"));
		info.setJvmName(System.getProperty("java.vm.name"));
		info.setJvmVersion(System.getProperty("java.vm.version"));
		return info;
	}

	private static Optional<String> getLocalHostName() {
		try {
			return Optional.ofNullable(InetAddress.getLocalHost().getHostName());
		}
		catch (UnknownHostException e) {
			log.warn("get local hostname fail", e);
			return Optional.empty();
		}
	}

}
