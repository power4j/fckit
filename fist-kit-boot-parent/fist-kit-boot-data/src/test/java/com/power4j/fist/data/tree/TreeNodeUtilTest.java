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

package com.power4j.fist.data.tree;

import com.power4j.fist.data.tree.domain.TreeNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/9
 * @since 2022
 */
class TreeNodeUtilTest {

	static TreeNode<Integer> makeNode(int id) {
		return TreeNode.of(id, null);
	}

	@Test
	void sortNodes() {

		// @formatter:off

		//
		//          (1)       (0)
		//         / |  \
		//       /   |   \
		//     (3) (100) (2)
		//    /  \
		// (30)  (20)
		//

		// @formatter:on

		List<TreeNode<Integer>> lv1 = Arrays.asList(makeNode(1), makeNode(0));
		List<TreeNode<Integer>> lv2 = Arrays.asList(makeNode(3), makeNode(100), makeNode(2));
		List<TreeNode<Integer>> lv3 = Arrays.asList(makeNode(30), makeNode(20));

		lv1.get(0).appendChildren(lv2);
		lv2.get(0).appendChildren(lv3);

		TreeNodeUtil.sortNodes(lv1, TreeNode::getId);

		TreeNode<Integer> nod_0 = lv1.get(0);
		Assertions.assertEquals(0, nod_0.getId());

		TreeNode<Integer> nod_1 = lv1.get(1);
		Assertions.assertEquals(1, nod_1.getId());

		TreeNode<Integer> nod_3 = nod_1.getChildren().get(1);
		Assertions.assertEquals(3, nod_3.getId());

		TreeNode<Integer> nod_100 = nod_1.getChildren().get(2);
		Assertions.assertEquals(100, nod_100.getId());

		TreeNode<Integer> nod_20 = nod_3.getChildren().get(0);
		Assertions.assertEquals(20, nod_20.getId());

		TreeNode<Integer> nod_30 = nod_3.getChildren().get(1);
		Assertions.assertEquals(30, nod_30.getId());

	}

}