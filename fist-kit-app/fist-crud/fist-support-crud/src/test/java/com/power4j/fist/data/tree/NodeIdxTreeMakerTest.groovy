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


import com.fasterxml.jackson.databind.ObjectMapper
import com.power4j.fist.data.tree.domain.OrgNodeIdx
import com.power4j.fist.data.tree.domain.TreeNode
import org.tools4j.groovytables.GroovyTables
import spock.lang.Specification

import java.util.function.Predicate
/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/7
 * @since 1.0
 */
class NodeIdxTreeMakerTest extends Specification {

	//
	//        (0)
	//       /   \
	//     (1)   (2)
	//    /  \
	// (101) (102)
	//
	List<OrgNodeIdx> links = GroovyTables.createListOf(OrgNodeIdx.class).fromTable {
		ancestor   | descendant    | distance
		0L         | 0L            | 0
		1L         | 1L            | 0
		2L         | 2L            | 0
		101L       | 101L          | 0
		102L       | 102L          | 0
		0L         | 1L            | 1
		0L         | 2L            | 1
		1L         | 101L          | 1
		1L         | 102L          | 1
		0L         | 101L          | 2
		0L         | 102L          | 2
	}

	def "Test Make Tree"() {
		given:
		TreeMaker<Long,TreeNode<Long>> maker = TreeMaker.useIdx(links);

		when:
		Predicate<TreeNode<Long>> underZero = new Predicate<TreeNode<Long>>(){
			@Override
			boolean test(TreeNode<Long> tree) {
				return Objects.equals(0L,tree.getParentId());
			}
		}
		TreeNode<Long> root = maker.build(0L).orElse(null)

		then:
		//选择0作为根节点,那么一级节点的为1 和2
		root.getChildren().get(0).getId() == 1L
		root.getChildren().get(1).getId() == 2L

		when:
		TreeNode<Long> lv1 = maker.build(1L).orElse(null)

		then:
		//选择1作为根节点,那么一级节点的为101 和102
		new ObjectMapper()
		System.out.println(TestUtil.jsonPrettyString(lv1))
		List<TreeNode<Long>> lv2 = lv1.getChildren()
		TreeNode<Long> org101 = lv2.get(0)

		org101.getId() == 101

		TreeNode<Long> org102 = lv2.get(1)
		org102.getId() == 102
	}
}
