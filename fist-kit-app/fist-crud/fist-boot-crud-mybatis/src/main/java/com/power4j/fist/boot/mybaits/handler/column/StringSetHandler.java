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

package com.power4j.fist.boot.mybaits.handler.column;

import com.power4j.coca.kit.common.text.StringPool;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

/**
 * 支持{@code Set<String>} 类型，数据库中的 {@code NULL} 转换为空集合
 * <ul>
 * <li>入库:字符串Join</li>
 * <li>出库:字符串分隔</li>
 * </ul>
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/23
 * @since 1.0
 */
@MappedTypes(value = { Set.class })
@MappedJdbcTypes(value = JdbcType.VARCHAR, includeNullJdbcType = true)
public class StringSetHandler extends BaseTypeHandler<Set<String>> {

	private final static String SEPARATOR = StringPool.COMMA;

	@Override
	public void setNonNullParameter(PreparedStatement ps, int i, Set<String> parameter, JdbcType jdbcType)
			throws SQLException {
		ps.setString(i, StringEncode.encode(parameter, SEPARATOR));
	}

	@Override
	public Set<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
		String raw = rs.getString(columnName);
		return StringEncode.decodeSet(raw, SEPARATOR);
	}

	@Override
	public Set<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
		String raw = rs.getString(columnIndex);
		return StringEncode.decodeSet(raw, SEPARATOR);
	}

	@Override
	public Set<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
		String raw = cs.getString(columnIndex);
		return StringEncode.decodeSet(raw, SEPARATOR);
	}

}
