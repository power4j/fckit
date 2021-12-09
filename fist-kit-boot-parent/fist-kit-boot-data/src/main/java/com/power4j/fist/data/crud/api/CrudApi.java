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

package com.power4j.fist.data.crud.api;

import com.power4j.coca.kit.common.lang.Result;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/9
 * @since 1.0
 * @param <C> 创建模型
 * @param <R> 读模型
 * @param <U> 更新模型
 * @param <ID> 主键类型
 */
public interface CrudApi<C, R, U, ID> {

	/**
	 * 创建对象
	 * @param obj 业务对象
	 * @return 返回操作结果中包含创建的对象
	 */
	Result<C> createObject(C obj);

	/**
	 * 删除对象
	 * @param id 对象ID
	 * @return 返回操作结果,对象不存在忽略
	 */
	Result<Void> deleteObject(ID id);

	/**
	 * 更新对象(根据ID)
	 * @param obj 业务对象
	 * @return 返回操作结果,对象不存在忽略
	 */
	Result<Void> updateObject(U obj);

	/**
	 * 读取对象(根据ID)
	 * @param id 对象ID
	 * @return 返回操作结果,对象不存在视为错误
	 */
	Result<R> readObject(ID id);

}
