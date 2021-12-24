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

package com.power4j.fist.data.tree.domain;

/**
 * 需要存储树形结构信息时可以继承此类
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/9/23
 * @since 1.0
 */
public abstract class BaseNodeIdx<ID, C extends BaseNodeIdx<ID, C>> implements NodeIdx<ID, C> {

	private ID ancestor;

	private ID descendant;

	private Integer distance;

	public BaseNodeIdx() {

	}

	public BaseNodeIdx(ID ancestor, ID descendant, Integer distance) {
		this.ancestor = ancestor;
		this.descendant = descendant;
		this.distance = distance;
	}

	public void setAncestor(ID ancestor) {
		this.ancestor = ancestor;
	}

	public void setDescendant(ID descendant) {
		this.descendant = descendant;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	@Override
	public ID getAncestor() {
		return ancestor;
	}

	@Override
	public ID getDescendant() {
		return descendant;
	}

	@Override
	public int getDistance() {
		return distance;
	}

}
