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

/**
 * 常用错误代码，共 5 位，分成两个部分：错误产生来源+四位数字编号。特殊值五个0表示成功。
 * <p/>
 * 错误来源定义
 * <ul>
 * <li>A 表示错误来源于用户，比如参数错误，用户安装版本过低，用户支付超时等问题</li>
 * <li>B 表示错误来源于当前系统，往往是业务逻辑出错，或程序健壮性差等问题</li>
 * <li>C 表示错误来源于第三方服务，比如 CDN 服务出错，消息投递超时等问题</li>
 * </ul>
 * <b><i>参照阿里JAVA开发规范</i></b>
 *
 * @author CJ (power4j@outlook.com)
 * @date 2021/6/6
 * @since 1.0
 */
public interface ErrorCode {

	/**
	 * 成功
	 */
	String OK = "00000";

	// ~ A类
	// ========================================================================================================

	/**
	 * 用户端错误 <br/>
	 * <b>一级宏观错误码</b>
	 */
	String A0001 = "A0001";

	/**
	 * 用户名校验失败
	 */
	String A0110 = "A0110";

	/**
	 * 用户名已存在
	 */
	String A0111 = "A0111";

	/**
	 * 用户名包含特殊字符
	 */
	String A0113 = "A0113";

	/**
	 * 密码校验失败
	 */
	String A0120 = "A0120";

	/**
	 * 密码长度不够
	 */
	String A0121 = "A0121";

	/**
	 * 用户登录异常 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String A0200 = "A0200";

	/**
	 * 访问权限异常 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String A0300 = "A0300";

	/**
	 * 访问未授权
	 */
	String A0301 = "A0301";

	/**
	 * 授权已过期
	 */
	String A0311 = "A0311";

	/**
	 * API Token 无效
	 */
	String A0321 = "A0321";

	/**
	 * 用户请求参数错误 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String A0400 = "A0400";

	/**
	 * 用户未登录
	 */
	String A0401 = "A0401";

	/**
	 * 用户请求服务异常 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String A0500 = "A0500";

	/**
	 * 资源不存在 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String A9900 = "A9900";

	// ~ B类
	// ========================================================================================================

	/**
	 * 系统执行出错 <br/>
	 * <b>一级宏观错误码</b>
	 */
	String B0001 = "B0001";

	/**
	 * 系统执行超时 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String B0100 = "B0100";

	/**
	 * 系统订单处理超时
	 */
	String B0101 = "B0101";

	/**
	 * 系统容灾功能被触发 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String B0200 = "B0200";

	/**
	 * 系统限流
	 */
	String B0210 = "B0210";

	/**
	 * 系统功能降级
	 */
	String B0220 = "B0220";

	/**
	 * 系统资源异常 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String B0300 = "B0300";

	/**
	 * 系统资源访问异常
	 */
	String B0320 = "B0320";

	// ~ C类
	// ========================================================================================================

	/**
	 * 调用第三方服务出错 一级宏观错误码 <br/>
	 * <b>一级宏观错误码</b>
	 */
	String C0001 = "C0001";

	/**
	 * 中间件服务出错 二级宏观错误码 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String C0100 = "C0100";

	/**
	 * RPC 调用出错
	 */
	String C0110 = "C0110";

	/**
	 * RPC 服务未找到
	 */
	String C0111 = "C0111";

	/**
	 * RPC 服务未注册
	 */
	String C0112 = "C0112";

	/**
	 * 接口不存在
	 */
	String C0113 = "C0113";

	/**
	 * 消息服务出错
	 */
	String C0120 = "C0120";

	/**
	 * 消息投递出错
	 */
	String C0121 = "C0121";

	/**
	 * 消息消费出错
	 */
	String C0122 = "C0122";

	/**
	 * 消息订阅出错
	 */
	String C0123 = "C0123";

	/**
	 * 消息分组未查到
	 */
	String C0124 = "C0124";

	/**
	 * 第三方系统执行超时 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String C0200 = "C0200";

	/**
	 * 数据库服务出错 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String C0300 = "C0300";

	/**
	 * SQL执行异常 <br/>
	 * <b>二级宏观错误码</b>
	 */
	String C0301 = "C0301";

}
