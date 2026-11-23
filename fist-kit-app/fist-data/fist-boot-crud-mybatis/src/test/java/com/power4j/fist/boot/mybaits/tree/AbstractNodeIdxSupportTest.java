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

package com.power4j.fist.boot.mybaits.tree;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2022/1/24
 * @since 1.0
 */
@SpringBootTest
@AutoConfigureTestDatabase
class AbstractNodeIdxSupportTest {

	@Autowired
	private OrgIdxMapper orgIdxMapper;

	@Autowired
	private OrgTreeService orgTreeService;

	void makeSingleRoot() {
		// @formatter:off

		//
		//        (0)
		//        / \
		//     (10) (20)
		//     /      \
		// (101)     (201)
		//          /   \
		//      (2011) (2012)
		//

		// @formatter:on

		orgTreeService.generatePath(0L, null);
		orgTreeService.generatePath(10L, 0L);
		orgTreeService.generatePath(20L, 0L);
		orgTreeService.generatePath(101L, 10L);
		orgTreeService.generatePath(201L, 20L);
		orgTreeService.generatePath(2011L, 201L);
		orgTreeService.generatePath(2012L, 201L);
	}

	void makeMultiRoot() {
		// @formatter:off

		//     (10)    (20)     (30)
		//      /       /
		//  (101)     (201)
		//           /   \
		//      (2011)   (2012)
		//

		// @formatter:on

		orgTreeService.generatePath(10L, null);
		orgTreeService.generatePath(20L, null);
		orgTreeService.generatePath(30L, null);
		orgTreeService.generatePath(101L, 10L);
		orgTreeService.generatePath(201L, 20L);
		orgTreeService.generatePath(2011L, 201L);
		orgTreeService.generatePath(2012L, 201L);
	}

	@AfterEach
	void tearDown() {
		orgTreeService.getRepository().deleteAll();
	}

	@Test
	void getAll() {
		makeSingleRoot();
		List<OrgIdx> list = orgTreeService.getAll(0, 0);
		Assertions.assertEquals(7, list.size());
	}

	@Test
	void subTreeNodes() {
		makeSingleRoot();
		List<Long> idList1 = Arrays.asList(101L, 201L);
		Set<Long> sub1 = orgTreeService.subTreeNodes(idList1);
		Set<Long> expected1 = new HashSet<>(Arrays.asList(101L, 201L, 2011L, 2012L));
		Assertions.assertEquals(expected1, sub1);

		List<Long> idList2 = Arrays.asList(20L, 201L);
		Set<Long> sub2 = orgTreeService.subTreeNodes(idList2);
		Set<Long> expected2 = new HashSet<>(Arrays.asList(20L, 201L, 2011L, 2012L));
		Assertions.assertEquals(expected2, sub2);
	}

	@Test
	void moveSubtreeReplacesExternalPathsAndPreservesInternalPaths() {
		makeSingleRoot();
		List<String> internalPaths = paths(orgTreeService.findAllDescendant(20L, null, null));

		orgTreeService.moveSubtree(20L, 10L);

		assertPaths(orgTreeService.findAllDescendant(10L, null, null), "10>10:0", "10>101:1", "10>20:1", "10>201:2",
				"10>2011:3", "10>2012:3");
		assertPaths(orgTreeService.findAllAncestor(2011L, null, null), "0>2011:4", "10>2011:3", "20>2011:2",
				"201>2011:1", "2011>2011:0");
		Assertions.assertEquals(internalPaths, paths(orgTreeService.findAllDescendant(20L, null, null)));
	}

	@Test
	void movingSubtreeToAnAncestorUpdatesPathsForLaterInserts() {
		makeSingleRoot();

		orgTreeService.moveSubtree(201L, 0L);
		orgTreeService.generatePath(2013L, 201L);

		assertPaths(orgTreeService.findAllAncestor(2013L, null, null), "0>2013:2", "201>2013:1", "2013>2013:0");
		assertPaths(orgTreeService.findAllDescendant(20L, null, null), "20>20:0");
		assertPaths(orgTreeService.findAllAncestor(2011L, null, null), "0>2011:2", "201>2011:1", "2011>2011:0");
	}

	@Test
	void moveSubtreeToTopLevelRemovesExternalPaths() {
		makeSingleRoot();

		orgTreeService.moveSubtree(201L, null);

		assertPaths(orgTreeService.findAllAncestor(2011L, null, null), "201>2011:1", "2011>2011:0");
		assertPaths(orgTreeService.findAllDescendant(0L, null, null), "0>0:0", "0>10:1", "0>101:2", "0>20:1");
	}

	@Test
	void moveSubtreeRejectsMissingNodesAndCyclesWithoutChangingPaths() {
		makeSingleRoot();
		List<String> before = paths(orgTreeService.getAll(null, null));

		Assertions.assertThrows(IllegalArgumentException.class, () -> orgTreeService.moveSubtree(99L, 10L));
		Assertions.assertThrows(IllegalArgumentException.class, () -> orgTreeService.moveSubtree(20L, 99L));
		Assertions.assertThrows(IllegalArgumentException.class, () -> orgTreeService.moveSubtree(20L, 20L));
		Assertions.assertThrows(IllegalArgumentException.class, () -> orgTreeService.moveSubtree(20L, 2011L));

		Assertions.assertEquals(before, paths(orgTreeService.getAll(null, null)));
	}

	private void assertPaths(List<OrgIdx> indexes, String... expected) {
		Assertions.assertEquals(Arrays.stream(expected).sorted().toList(), paths(indexes));
	}

	private List<String> paths(List<OrgIdx> indexes) {
		return indexes.stream()
			.map(index -> index.getAncestor() + ">" + index.getDescendant() + ":" + index.getDistance())
			.sorted()
			.toList();
	}

	@Test
	void findSharedRoot() {
		makeMultiRoot();
		List<Long> idList1 = Arrays.asList(101L, 2011L);
		Set<Long> roots1 = orgTreeService.findSharedRoot(idList1);
		Set<Long> expected1 = new HashSet<>(Arrays.asList(10L, 20L));
		Assertions.assertEquals(expected1, roots1);

		List<Long> idList2 = Arrays.asList(20L, 2012L);
		Set<Long> roots21 = orgTreeService.findSharedRoot(idList2);
		Set<Long> expected21 = new HashSet<>(Collections.singletonList(20L));
		Assertions.assertEquals(expected21, roots21);

		List<Long> idList3 = Arrays.asList(201L, 2011L, 2012L);
		Set<Long> roots3 = orgTreeService.findSharedRoot(idList3);
		Set<Long> expected3 = new HashSet<>(Collections.singletonList(20L));
		Assertions.assertEquals(expected3, roots3);
	}

}
