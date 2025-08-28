/*
 * Copyright 2025. ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
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

package com.power4j.fist.boot.autoconfigure;

import com.power4j.fist.mybatis.extension.meta.ValueSupplier;
import com.power4j.fist.mybatis.extension.meta.ValueSupplierResolver;
import org.springframework.beans.factory.BeanFactory;

import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @since 1.0
 */
public class ValueSupplierBeanResolver implements ValueSupplierResolver {

	private final BeanFactory beanFactory;

	public ValueSupplierBeanResolver(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Optional<ValueSupplier> resolve(Class<?> cls) {
		return Optional.of((ValueSupplier) beanFactory.getBean(cls));
	}

}
