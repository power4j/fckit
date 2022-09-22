/*
 *  Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
 *
 *  Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *  <p>
 *  http://www.gnu.org/licenses/lgpl.html
 *  <p>
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.power4j.fist.boot.common.op;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/12
 * @since 1.0
 * @param <T>
 */
public interface OpTemplateConfigure<T, B extends OpTemplateBuilder<T>> {

	/**
	 * 进行自定义配置
	 * @param builder OpTemplateConfigure
	 */
	void config(B builder);

}