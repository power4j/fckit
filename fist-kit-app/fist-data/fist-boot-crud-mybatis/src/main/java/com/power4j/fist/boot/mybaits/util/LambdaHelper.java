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

package com.power4j.fist.boot.mybaits.util;

import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.LambdaUtils;
import com.baomidou.mybatisplus.core.toolkit.support.ColumnCache;
import com.baomidou.mybatisplus.core.toolkit.support.LambdaMeta;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.reflection.property.PropertyNamer;

import java.util.Map;
import java.util.Optional;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/10/13
 * @since 1.0
 */
public class LambdaHelper<T> {

	private final Class<T> entityClass;

	private Map<String, ColumnCache> columnMap = null;

	private boolean initColumnMap = false;

	public LambdaHelper(Class<T> entityClass) {
		this.entityClass = entityClass;
	}

	public Class<T> getEntityClass() {
		return entityClass;
	}

	/**
	 * 实体类信息
	 * @return TableInfo
	 */
	public Optional<TableInfo> getTableInfo() {
		return Optional.ofNullable(TableInfoHelper.getTableInfo(entityClass));
	}

	/**
	 * 获取列名
	 * @param colFunc 字段求值函数,比如 {@code User::getName}
	 * @param onlyColumn 是否只包含列名称
	 * @return 列名称
	 */
	public String colToStr(SFunction<T, ?> colFunc, boolean onlyColumn) {
		return getColumn(LambdaUtils.extract(colFunc), onlyColumn);
	}

	/**
	 * 获取 SerializedLambda 对应的列信息，从 lambda 表达式中推测实体类
	 * @param lambda lambda 表达式
	 * @param onlyColumn 是否只包含列名称
	 * @return 列
	 */
	protected String getColumn(LambdaMeta lambda, boolean onlyColumn) {
		Class<?> aClass = lambda.getInstantiatedClass();
		tryInitCache(aClass);
		String fieldName = PropertyNamer.methodToProperty(lambda.getImplMethodName());
		ColumnCache columnCache = getColumnCache(fieldName, aClass);
		return onlyColumn ? columnCache.getColumn() : columnCache.getColumnSelect();
	}

	private void tryInitCache(Class<?> lambdaClass) {
		synchronized (this) {
			if (!initColumnMap) {
				final Class<T> entityClass = getEntityClass();
				if (entityClass != null) {
					lambdaClass = entityClass;
				}
				columnMap = LambdaUtils.getColumnMap(lambdaClass);
				initColumnMap = true;
			}
		}
		Assert.notNull(columnMap, "can not find lambda cache for this entity [%s]", lambdaClass.getName());
	}

	private ColumnCache getColumnCache(String fieldName, Class<?> lambdaClass) {
		ColumnCache columnCache = columnMap.get(LambdaUtils.formatKey(fieldName));
		Assert.notNull(columnCache, "can not find lambda cache for this property [%s] of entity [%s]", fieldName,
				lambdaClass.getName());
		return columnCache;
	}

}
