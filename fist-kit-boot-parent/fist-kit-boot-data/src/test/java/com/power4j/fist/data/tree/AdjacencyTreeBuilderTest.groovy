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

package com.power4j.fist.data.tree

import cn.hutool.core.lang.tree.Tree
import cn.hutool.core.lang.tree.TreeNodeConfig
import cn.hutool.core.util.RandomUtil
import cn.hutool.json.JSONUtil
import com.power4j.fist.data.tree.domain.Org
import org.springframework.lang.Nullable
import org.tools4j.groovytables.GroovyTables
import spock.lang.Specification

import java.util.function.Function
import java.util.function.Predicate

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/27
 * @since 1.0
 */
class AdjacencyTreeBuilderTest extends Specification {

	//
	//        (0)
	//       /   \
	//     (1)   (2)
	//    /  \
	// (101) (102)
	//
	List<Org> orgList = GroovyTables.createListOf(Org.class).fromTable {
		id         | pid           | name
		1L         | 0L            | "org-1"
		2L         | 0L            | "org-2"
		101L       | 1L            | "org-101"
		102L       | 1L            | "org-102"
	}

	TreeNodeCustomizer<Long,Org> customizer = new TreeNodeCustomizer<Long,Org>(){

		@Override
		void customize(Tree<Long> node, @Nullable Org meta) {
			if(null == meta){
				return;
			}
			node.setName(String.format("ORG-%04d",node.getId()));
			node.setWeight(node.getId());
			node.putExtra("hot", RandomUtil.randomInt())
		}
	}
	Function<Org,Long> idFunc = new Function<Org,Long>() {
		@Override
		Long apply(Org org) {
			return org.getId();
		}
	}

	Function<Org,Long> pidFunc = new Function<Org,Long>() {

		@Override
		Long apply(Org org) {
			return org.getPid();
		}
	}
	TreeNodeConfig config = TreeNodeConfig.DEFAULT_CONFIG.setWeightKey("order")

	def "Test Make Tree"() {
		given:
		TreeBuilder builder = TreeBuilder.use(orgList,idFunc,pidFunc).nodeConfig(config).customizer(customizer);
		List<Tree<Long>> roots;

		when:
		Predicate<Tree<Long>> underZero = new Predicate<Tree<Long>>(){
			@Override
			boolean test(Tree<Long> tree) {
				return Objects.equals(0L,tree.getParentId());
			}
		}
		roots = builder.build(underZero);

		then:
		System.out.println(JSONUtil.toJsonPrettyStr(roots))
		// 选择 0 下面的节点做作为根节点 那么得到的一级节点是 1和2
		roots.size() == 2
		roots.get(0).getId() == 1L
		roots.get(1).getId() == 2L

		then:
		Tree<Long> org1 = roots.get(0)
		Tree<Long> org2 = roots.get(1)

		org1.getWeight() == 1
		org1.getId() == 1

		org2.getWeight() == 2
		org2.getId() == 2

		then:
		List<Tree<Long>> lv2 = org1.getChildren()
		Tree<Long> org101 = lv2.get(0)

		org101.getWeight() == 101
		org101.getId() == 101

		Tree<Long> org102 = lv2.get(1)
		org102.getWeight() == 102
		org102.getId() == 102

		when:
		Predicate<Tree<Long>> useZero = new Predicate<Tree<Long>>(){
			@Override
			boolean test(Tree<Long> tree) {
				return Objects.equals(0L,tree.getId());
			}
		}
		roots = builder.build(useZero);

		then:
		// 选择 0 点做作为根节点,但是数据源中没有ID为0的数据
		roots.size() == 0


		when:
		roots = builder.build();

		then:
		// 推出出来的根节点是 1 和 2
		roots.size() == 2
		// 选择 0 点做作为根节点,但是数据源中没有ID为0的数据
		roots.get(0).getId() == 1
		roots.get(1).getId() == 2
	}
}
