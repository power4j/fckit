package com.power4j.fist.jasypt.boot.autoconfigure;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import com.power4j.fist.jasypt.core.GmConfigCryptoException;
import com.power4j.fist.jasypt.core.GmEncryptedValue;
import org.jasypt.encryption.StringEncryptor;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginLookup;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.SystemEnvironmentPropertySource;

final class FistJasyptPropertySources {

	private FistJasyptPropertySources() {
	}

	static void wrap(ConfigurableEnvironment environment, Supplier<StringEncryptor> encryptor) {
		String prefix = environment.getProperty(FistJasyptEnvironmentPostProcessor.JASYPT_PREFIX,
				environment.getProperty("fist.jasypt.cipher-prefix", GmEncryptedValue.PREFIX));
		String suffix = environment.getProperty(FistJasyptEnvironmentPostProcessor.JASYPT_SUFFIX,
				environment.getProperty("fist.jasypt.cipher-suffix", GmEncryptedValue.SUFFIX));
		String masterKeyName = environment.getProperty("fist.jasypt.master-key-env", "FIST_JASYPT_MASTER_KEY");
		Decryptor decryptor = new Decryptor(prefix, suffix, masterKeyName, encryptor, environment,
				ThreadLocal.withInitial(HashSet::new));
		MutablePropertySources sources = environment.getPropertySources();
		for (PropertySource<?> source : sources) {
			if (ConfigurationPropertySources.isAttachedConfigurationPropertySource(source)
					|| source instanceof PropertySource.StubPropertySource) {
				continue;
			}
			if (source instanceof SystemEnvironmentPropertySource system) {
				sources.replace(source.getName(), new EncryptableSystemEnvironment(system, decryptor));
			}
			else if (source instanceof EnumerablePropertySource<?> enumerable) {
				sources.replace(source.getName(), new EncryptableEnumerableSource(enumerable, decryptor));
			}
			else {
				sources.replace(source.getName(), new EncryptableSource(source, decryptor));
			}
		}
	}

	private record Decryptor(String prefix, String suffix, String masterKeyName, Supplier<StringEncryptor> encryptor,
			ConfigurableEnvironment environment, ThreadLocal<Set<String>> resolving) {

		@Nullable Object read(PropertySource<?> source, String name) {
			Object value = source.getProperty(name);
			// 加密器初始化时直接绑定密钥配置，避免递归查找加密器。
			if (value instanceof String text && !name.startsWith("fist.jasypt.") && !name.startsWith("FIST_JASYPT_")
					&& !name.equals(this.masterKeyName)) {
				String cipher = text.trim();
				if (cipher.length() >= this.prefix.length() + this.suffix.length() && cipher.startsWith(this.prefix)
						&& cipher.endsWith(this.suffix)) {
					String payload = cipher.substring(this.prefix.length(), cipher.length() - this.suffix.length());
					return this.encryptor.get().decrypt(resolvePayload(name, payload));
				}
			}
			return value;
		}

		private String resolvePayload(String name, String payload) {
			if (!payload.contains("${")) {
				return payload;
			}
			// 占位符可能再次读取加密属性，跨嵌套解析检测循环引用。
			Set<String> active = this.resolving.get();
			if (!active.add(name)) {
				throw new IllegalArgumentException("Circular GMENC payload reference for property '" + name + "'");
			}
			try {
				return this.environment.resolvePlaceholders(payload);
			}
			catch (RuntimeException ex) {
				// 解析异常的消息和原因链可能包含配置值。
				throw new GmConfigCryptoException("Failed to resolve GMENC payload placeholders");
			}
			finally {
				active.remove(name);
				if (active.isEmpty()) {
					this.resolving.remove();
				}
			}
		}

	}

	private static final class EncryptableSource extends PropertySource<PropertySource<?>>
			implements OriginLookup<String> {

		private final Decryptor decryptor;

		EncryptableSource(PropertySource<?> source, Decryptor decryptor) {
			super(source.getName(), source);
			this.decryptor = decryptor;
		}

		@Override
		public @Nullable Object getProperty(String name) {
			return this.decryptor.read(this.source, name);
		}

		@Override
		public boolean containsProperty(String name) {
			return this.source.containsProperty(name);
		}

		@Override
		public @Nullable Origin getOrigin(String name) {
			return OriginLookup.getOrigin(this.source, name);
		}

	}

	private static final class EncryptableEnumerableSource extends EnumerablePropertySource<EnumerablePropertySource<?>>
			implements OriginLookup<String> {

		private final Decryptor decryptor;

		EncryptableEnumerableSource(EnumerablePropertySource<?> source, Decryptor decryptor) {
			super(source.getName(), source);
			this.decryptor = decryptor;
		}

		@Override
		public @Nullable Object getProperty(String name) {
			return this.decryptor.read(this.source, name);
		}

		@Override
		public boolean containsProperty(String name) {
			return this.source.containsProperty(name);
		}

		@Override
		public String[] getPropertyNames() {
			return this.source.getPropertyNames();
		}

		@Override
		public @Nullable Origin getOrigin(String name) {
			return OriginLookup.getOrigin(this.source, name);
		}

	}

	private static final class EncryptableSystemEnvironment extends SystemEnvironmentPropertySource
			implements OriginLookup<String> {

		private final SystemEnvironmentPropertySource delegate;

		private final Decryptor decryptor;

		EncryptableSystemEnvironment(SystemEnvironmentPropertySource source, Decryptor decryptor) {
			super(source.getName(), source.getSource());
			this.delegate = source;
			this.decryptor = decryptor;
		}

		@Override
		public @Nullable Object getProperty(String name) {
			return this.decryptor.read(this.delegate, name);
		}

		@Override
		public boolean containsProperty(String name) {
			return this.delegate.containsProperty(name);
		}

		@Override
		public @Nullable Origin getOrigin(String name) {
			return OriginLookup.getOrigin(this.delegate, name);
		}

	}

}
