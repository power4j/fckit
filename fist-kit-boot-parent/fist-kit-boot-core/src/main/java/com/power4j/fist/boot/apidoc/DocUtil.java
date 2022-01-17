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

package com.power4j.fist.boot.apidoc;

import cn.hutool.core.text.CharSequenceUtil;
import lombok.experimental.UtilityClass;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/16
 * @since 1.0
 */
@UtilityClass
public class DocUtil {

	public Optional<ApiDetails> buildApiDetails(ResourceDescribe describe, Supplier<ApiTrait> supplier) {
		ApiTrait apiTrait = supplier.get();
		if (Objects.nonNull(apiTrait)) {
			ApiDetails details = createDetails(apiTrait);
			if (CharSequenceUtil.isEmpty(details.getResourceName())) {
				details.setResourceName(describe.getResourceName());
			}
			return Optional.of(details);
		}
		return Optional.empty();
	}

	public ApiDetails createDetails(ApiTrait apiTrait) {
		ApiDetails apiDetails = new ApiDetails();
		apiDetails.setResourceId(apiTrait.resourceId());
		apiDetails.setLevel(apiTrait.level().getValue());
		apiDetails.setExpose(apiTrait.access().getValue());
		apiDetails.setSign(apiTrait.sign());
		return apiDetails;
	}

}
