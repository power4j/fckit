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

import com.power4j.fist.data.tree.domain.OrgNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/5/7
 * @since 1.0
 */
class AdjacencyTreeMakerTest {

	private final List<OrgNode> orgList = Arrays.asList(node(1L, 0L, "org-1"), node(2L, 0L, "org-2"),
			node(101L, 1L, "org-101"), node(102L, 1L, "org-102"));

	@Test
	void shouldMakeTreeFromAdjacencyList() {
		TreeMaker<Long, OrgNode> treeMaker = TreeMaker.use(this.orgList);

		List<OrgNode> roots = treeMaker.build(tree -> Objects.equals(0L, tree.getParentId()));

		Assertions.assertEquals(2, roots.size());
		Assertions.assertEquals(1L, roots.get(0).getId());
		Assertions.assertEquals(2L, roots.get(1).getId());

		OrgNode org1 = roots.get(0);
		OrgNode org2 = roots.get(1);

		Assertions.assertEquals(1L, org1.getId());
		Assertions.assertEquals(2L, org2.getId());

		List<OrgNode> lv2 = org1.getChildren();

		Assertions.assertEquals(101L, lv2.get(0).getId());
		Assertions.assertEquals(102L, lv2.get(1).getId());

		roots = treeMaker.build(tree -> Objects.equals(0L, tree.getId()));

		Assertions.assertEquals(0, roots.size());

		roots = treeMaker.build();

		Assertions.assertEquals(2, roots.size());
		Assertions.assertEquals(1L, roots.get(0).getId());
		Assertions.assertEquals(2L, roots.get(1).getId());
	}

	private static OrgNode node(Long id, Long parentId, String name) {
		OrgNode node = new OrgNode();
		node.setId(id);
		node.setPid(parentId);
		node.setName(name);
		return node;
	}

}
