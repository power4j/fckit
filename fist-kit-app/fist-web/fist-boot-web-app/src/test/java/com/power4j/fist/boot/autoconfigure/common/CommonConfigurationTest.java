package com.power4j.fist.boot.autoconfigure.common;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class CommonConfigurationTest {

	@Test
	void shouldImportMessageConfiguration() {
		Import imports = CommonConfiguration.class.getAnnotation(Import.class);

		assertThat(imports).isNotNull();
		assertThat(Arrays.stream(imports.value()).map(Class::getName))
			.contains("com.power4j.fist.boot.autoconfigure.i18n.MessageConfiguration");
	}

}
