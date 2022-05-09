/*
 * Copyright 2021 ChenJun (power4j@outlook.com & https://github.com/John-Chan)
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

package com.power4j.fist.data.tree

import cn.hutool.json.JSONUtil
import com.power4j.fist.data.tree.domain.OrgNode
import org.tools4j.groovytables.GroovyTables
import spock.lang.Specification

import java.util.function.Predicate
/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/7
 * @since 1.0
 */
class AdjacencyTreeMakerTest extends Specification {

	//
	//        (0)
	//       /   \
	//     (1)   (2)
	//    /  \
	// (101) (102)
	//
	List<OrgNode> orgList = GroovyTables.createListOf(OrgNode.class).fromTable {
		id         | pid           | name
		1L         | 0L            | "org-1"
		2L         | 0L            | "org-2"
		101L       | 1L            | "org-101"
		102L       | 1L            | "org-102"
	}

	
	def "Test Make Tree"() {
		given:
		TreeMaker<Long,OrgNode> treeMaker = TreeMaker.use(orgList);
		List<OrgNode> roots;

		when:
		Predicate<OrgNode> underZero = new Predicate<OrgNode>(){
			@Override
			boolean test(OrgNode tree) {
				return Objects.equals(0L,tree.getParentId());
			}
		}
		roots = treeMaker.build(underZero);

		then:
		System.out.println(JSONUtil.toJsonPrettyStr(roots))
		// 选择 0 下面的节点做作为根节点 那么得到的一级节点是 1和2
		roots.size() == 2
		roots.get(0).getId() == 1L
		roots.get(1).getId() == 2L

		then:
		OrgNode org1 = roots.get(0)
		OrgNode org2 = roots.get(1)

		org1.getId() == 1

		org2.getId() == 2

		then:
		List<OrgNode> lv2 = org1.getChildren()
		OrgNode org101 = lv2.get(0)

		org101.getId() == 101

		OrgNode org102 = lv2.get(1)
		org102.getId() == 102

		when:
		Predicate<OrgNode> useZero = new Predicate<OrgNode>(){
			@Override
			boolean test(OrgNode tree) {
				return Objects.equals(0L,tree.getId());
			}
		}
		roots = treeMaker.build(useZero);

		then:
		// 选择 0 点做作为根节点,但是数据源中没有ID为0的数据
		roots.size() == 0


		when:
		roots = treeMaker.build();

		then:
		// 推出出来的根节点是 1 和 2
		roots.size() == 2
		// 选择 0 点做作为根节点,但是数据源中没有ID为0的数据
		roots.get(0).getId() == 1
		roots.get(1).getId() == 2
	}
}
