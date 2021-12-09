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

package com.power4j.fist.boot.common.error;

import lombok.experimental.UtilityClass;

/**
 * @author CJ (power4j@outlook.com)
 * @date 2021/11/10
 * @since 1.0
 */
@UtilityClass
public class CommonErrors {

	public MsgBundleRejectedException requestParameterError(String msgKey, Object... msgArg) {
		return new MsgBundleRejectedException(ErrorCode.A0400, msgKey, msgArg);
	}

	public MsgBundleRejectedException requestParameterError() {
		return new MsgBundleRejectedException(ErrorCode.A0400, "common.bad-parameter");
	}

	public MsgBundleRejectedException resourceNotExistsError() throws RejectedException {
		return new MsgBundleRejectedException(ErrorCode.A0400, "common.resource.not-found");
	}

	public MsgBundleRejectedException permissionDeniedError() throws RejectedException {
		return new MsgBundleRejectedException(ErrorCode.A0301, "common.permission.denied");
	}

	public MsgBundleRejectedException authRequiredError() throws RejectedException {
		return new MsgBundleRejectedException(ErrorCode.A0401, "common.permission.no-auth-denied");
	}

}
