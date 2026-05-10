/*
 * Copyright (c) 2024. ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.power4j.fist.jackson.support.obfuscation;

import com.power4j.fist.jackson.annotation.Obfuscation;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.deser.jdk.StringDeserializer;
import tools.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 * @see StringObfuscateRegistry
 */
public class ObfuscatedStringDeserializer extends StdDeserializer<String> {

	private final StringObfuscate obfuscate;

	private final ObfuscateProcessorProvider resolver;

	public ObfuscatedStringDeserializer() {
		super(String.class);
		this.obfuscate = SimpleStringObfuscate.ofDefault();
		this.resolver = StringObfuscateRegistry.INSTANCE;
	}

	protected ObfuscatedStringDeserializer(StringObfuscate obfuscate, ObfuscateProcessorProvider resolver) {
		super(String.class);
		this.obfuscate = obfuscate;
		this.resolver = resolver;
	}

	@Override
	public String deserialize(JsonParser p, DeserializationContext ctx) throws JacksonException {
		String value = p.getValueAsString();
		if (value == null) {
			return null;
		}
		else if (!value.startsWith(obfuscate.modeId() + StringObfuscate.HEAD)) {
			return value;
		}
		value = value.substring(obfuscate.modeId().length() + 1);
		try {
			return obfuscate.deobfuscate(value);
		}
		catch (Exception e) {
			throw new StreamReadException(p, e.getMessage(), e);
		}
	}

	@Override
	public ValueDeserializer<?> createContextual(DeserializationContext ctx, BeanProperty property) {
		if (property == null) {
			return StringDeserializer.instance;
		}
		Obfuscation annotation = property.getAnnotation(Obfuscation.class);
		if (annotation == null) {
			return StringDeserializer.instance;
		}
		String mode = annotation.mode();
		Optional<StringObfuscate> processor = resolver.getInstance(mode);
		if (!processor.isPresent()) {
			throw new IllegalStateException(String.format("Obfuscation processor not registered: %s", mode));
		}
		return new ObfuscatedStringDeserializer(processor.get(), resolver);
	}

}
