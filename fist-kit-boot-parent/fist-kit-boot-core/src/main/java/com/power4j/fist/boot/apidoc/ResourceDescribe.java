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

package com.power4j.fist.boot.apidoc;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/16
 * @since 1.0
 */
public interface ResourceDescribe {

	/**
	 * 资源签名
	 * @return 字符串类型资源签名值
	 */
	String getResourceSign();

	/**
	 * 资源名称
	 * @return 字符串类型资源名称
	 */
	String getResourceName();

}
