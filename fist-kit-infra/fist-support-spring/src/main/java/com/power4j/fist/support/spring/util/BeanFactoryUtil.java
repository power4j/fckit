package com.power4j.fist.support.spring.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

import java.util.function.Supplier;

/**
 * @author CJ (power4j@outlook.com)
 * @since 2022.1
 */
@Slf4j
@UtilityClass
public class BeanFactoryUtil {

	/**
	 * 注册Bean
	 * @param beanFactory
	 * @param type
	 * @param beanName
	 * @param beanSupplier
	 * @param <T>
	 * @throws BeansException
	 */
	public <T> void registerPrimaryBean(DefaultListableBeanFactory beanFactory, Class<T> type, String beanName,
			Supplier<T> beanSupplier) throws BeansException {
		String[] names = beanFactory.getBeanNamesForType(type);
		for (String name : names) {
			BeanDefinition definition = beanFactory.getBeanDefinition(name);
			if (definition.isPrimary()) {
				log.debug("unset primary for bean {}, bean class :{}", name, definition.getBeanClassName());
				definition.setPrimary(false);
			}
		}
		BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(type, beanSupplier)
			.getBeanDefinition();
		beanDefinition.setPrimary(true);
		String name = beanName;

		log.debug("register bean :{}", name);
		beanFactory.registerBeanDefinition(name, beanDefinition);

	}

}
