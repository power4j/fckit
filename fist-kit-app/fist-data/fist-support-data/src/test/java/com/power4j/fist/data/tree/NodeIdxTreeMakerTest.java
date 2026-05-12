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

import com.power4j.fist.data.tree.domain.OrgNodeIdx;
import com.power4j.fist.data.tree.domain.TreeNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/7
 * @since 1.0
 */
class NodeIdxTreeMakerTest {

	private final List<OrgNodeIdx> links = Arrays.asList(link(0L, 0L, 0), link(1L, 1L, 0), link(2L, 2L, 0),
			link(101L, 101L, 0), link(102L, 102L, 0), link(0L, 1L, 1), link(0L, 2L, 1), link(1L, 101L, 1),
			link(1L, 102L, 1), link(0L, 101L, 2), link(0L, 102L, 2));

	@Test
	void shouldMakeTreeFromNodeIndex() {
		TreeMaker<Long, TreeNode<Long>> maker = TreeMaker.useIdx(this.links);

		TreeNode<Long> root = maker.build(0L).orElseThrow();

		Assertions.assertEquals(1L, root.getChildren().get(0).getId());
		Assertions.assertEquals(2L, root.getChildren().get(1).getId());

		TreeNode<Long> lv1 = maker.build(1L).orElseThrow();
		List<TreeNode<Long>> lv2 = lv1.getChildren();

		Assertions.assertEquals(101L, lv2.get(0).getId());
		Assertions.assertEquals(102L, lv2.get(1).getId());
	}

	private static OrgNodeIdx link(Long ancestor, Long descendant, int distance) {
		OrgNodeIdx link = new OrgNodeIdx();
		link.setAncestor(ancestor);
		link.setDescendant(descendant);
		link.setDistance(distance);
		return link;
	}

}
