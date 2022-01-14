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

package com.power4j.fist.data.tree;

import cn.hutool.core.lang.tree.Tree;
import org.springframework.lang.Nullable;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/14
 * @since 1.0
 */
public interface TreeNodeCustomizer<ID, T> {

	/**
	 * 进行个性化处理
	 * @param node 需要定制属性的Tree节点
	 * @param meta 该节点关联的业务数据,有可能为null
	 */
	void customize(Tree<ID> node, @Nullable T meta);

}
