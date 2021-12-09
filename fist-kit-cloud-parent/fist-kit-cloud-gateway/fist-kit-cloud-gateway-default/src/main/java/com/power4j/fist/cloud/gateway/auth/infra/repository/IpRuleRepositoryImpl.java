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

package com.power4j.fist.cloud.gateway.auth.infra.repository;

import com.power4j.fist.boot.mybaits.crud.repository.BaseRepository;
import com.power4j.fist.cloud.gateway.auth.entity.IpRule;
import com.power4j.fist.cloud.gateway.auth.infra.mapper.IpRuleMapper;
import com.power4j.fist.cloud.gateway.auth.repository.IpRuleRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/14
 * @since 1.0
 */
@Component
public class IpRuleRepositoryImpl extends BaseRepository<IpRuleMapper, IpRule, Long> implements IpRuleRepository {

	@Override
	public List<IpRule> listByType(String type) {
		return lambdaQuery().eq(IpRule::getType, type).list();
	}

}
