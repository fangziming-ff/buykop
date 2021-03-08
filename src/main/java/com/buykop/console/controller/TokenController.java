package com.buykop.console.controller;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.entity.InvokeLog;
import com.buykop.console.entity.PUser;
import com.buykop.console.service.TokenService;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.io.VerifyCodeUtils;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.DateUtil;
import com.buykop.framework.util.query.BaseQuery;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;

@Module(display = "会话管理", sys = Constants.current_sys)
@RestController
@RequestMapping(TokenController.URI)
public class TokenController extends BaseController {

	protected static final String URI = "/token";

	private static Logger logger = LoggerFactory.getLogger(TokenController.class);

	@Autowired
	private TokenService service;

	

	@Security(accessType = "1", displayName = "强制下线", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_oisAdmin)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/delete", UserToken.class.getName(), "", true,
					this.getService());

			BosConstants.debug("delete token=" + id + "   current=" + ut.getTokenKey());

			if (id.startsWith("TK")) {
				this.service.getRdClient().deleteUserToken(id, null);
			} else {
				this.service.getRdClient().getJedis().del(id);
			}

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@RequestMapping(value = "/send", method = RequestMethod.GET)
	public void send() throws Exception {

		JSONObject json = new JSONObject(true);
		json.put("id", "1");
		json.put("userId", "1");
		json.put("memberId", "1");
		json.put("loginType", "0");// 0:PC 1:微信 2:App
		json.put("ip", "192.168.1.1");

		System.out.println("Sender : " + json.toJSONString());

		this.getService().getRabbitTemplate().convertAndSend("loginLog", json);

		// 只要数据库设计了浏览量的表，都可以用这个mq
		json = new JSONObject(true);
		json.put("userId", "1");
		json.put("className", "buykop.Product");// 产品
		json.put("idValue", "1");// 产品id
		json.put("ipAddress", "192.168.1.1");
		this.getService().getRabbitTemplate().convertAndSend("bizView", json);

		json = new JSONObject(true);
		json.put("id", "1");
		json.put("className", "buykop.Product");
		json.put("init", "status=${-1}");// 初始化脚本 只有status=-1的数据才能真正删除
		getService().getRabbitTemplate().convertAndSend("delay.data.delete.exchange", "delay.data.delete.routingkey", json);

		json = new JSONObject(true);
		json.put("id", "1");
		json.put("className", "buykop.PlaceInfo");
		// json.put("init", "status=${-1}");//初始化脚本 只有status=-1的数据才能真正删除
		getService().getRabbitTemplate().convertAndSend("delay.data.delete.exchange", "delay.data.delete.routingkey", json);

	}

	

	@Security(accessType = "0", displayName = "用户登录", isEntAdmin = false, isSysAdmin = false, needLogin = false)
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject login(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		if (DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(UserToken.createToken(true));
		}

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			

			String user = json.getSimpleData("user", "手机号或邮件", String.class, true, service);
			String password = json.getSimpleData("password", "密码", String.class, true, service);

			String ip = this.getIpAddr(request);

			ut = this.service.login(user, password, json.getType(), ip, json.getLan());

			if (ut != null) {
				json.setTokenKey(ut.getTokenKey());
				json.getData().put("User", ut.getUserInfoForCookie());
				json.setIsLogin(1);
				json.judgeUserType(ut);
			} else {
				json.setUnSuccess(-1, LabelDisplay.get("登录验证失败", json.getLan()));
				return json.jsonValue();
			}

			json.judgeUserType(ut);

			this.service.getRdClient().putUserToken(ut, json.getType());

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();

	}
	
	
	
	
	
	@Security(accessType = "0", displayName = "机构登录", isEntAdmin = false, isSysAdmin = false, needLogin = false)
	@RequestMapping(value = "/memberLogin", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject memberLogin(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		if (DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(UserToken.createToken(true));
		}

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			
			String memberId = json.getSimpleData("memberId", "商户", String.class, true, service);
			String user = json.getSimpleData("user", "手机号或邮件", String.class, true, service);
			String password = json.getSimpleData("password", "密码", String.class, true, service);

			String ip = this.getIpAddr(request);

			ut = this.service.memberLogin(memberId,user, password, json.getType(), ip, json.getLan());

			if (ut != null) {
				json.setTokenKey(ut.getTokenKey());
				json.getData().put("User", ut.getUserInfoForCookie());
				json.setIsLogin(1);
				json.judgeUserType(ut);
			} else {
				json.setUnSuccess(-1, LabelDisplay.get("登录验证失败", json.getLan()));
				return json.jsonValue();
			}

			json.judgeUserType(ut);

			this.service.getRdClient().putUserToken(ut, json.getType());

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();

	}


	@Security(accessType = "0", displayName = "管理员登录", isEntAdmin = false, isSysAdmin = false, needLogin = false)
	@RequestMapping(value = "/adminLogin", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody
	public JSONObject adminLogin(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		if (DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(UserToken.createToken(true));
		}

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			PUser user = json.getObj(PUser.class, "loginName,loginPwd", this.service);

			if (json.getType() == 0) {// type 0:WEB 1:WX 2:APP 3:DEVICE 4:SSO

				Long webCheck = PSysParam.paramLongValue("1", BosConstants.paramWebCheckCode);
				if (webCheck == null) {
					webCheck = 1L;
				}

				String src = this.service.getRdClient().getTokenParam(json.getTokenKey(),
						VerifyCodeUtils.class.getName(), "checkCode");

				// SysConstants.debug("devMode="+SysConstants.devMode()+"
				// token="+json.getTokenKey()+" src="+src+" checkCode="+user.getCheckCode()+"
				// springProfilesActive="+BosConstants.runEnvironmental);

				if (user.getCheckCode() != null && webCheck.intValue() == 1
						&& !BosConstants.devMode() && !DataChange.isEmpty(src)) {

					if (DataChange.isEmpty(user.getCheckCode())) {
						json.setUnSuccess(-1, "验证码不存在或超时,请重新登录");
						return json.jsonValue();
					} else if (!src.equalsIgnoreCase(user.getCheckCode())) {
						json.setUnSuccess(-1, "输入的验证码有误,请重新登录");
						return json.jsonValue();
					}
				}

			}

			String ip = this.getIpAddr(request);

			ut = this.service.adminLogin(user.getLoginName(), user.getLoginPwd(),json.getType(), ip, json.getLan());

			if (ut != null) {
				json.setTokenKey(ut.getTokenKey());
				json.getData().put("User", ut.getUserInfoForCookie());
				json.setIsLogin(1);
				json.judgeUserType(ut);
			} else {
				json.setUnSuccess(-1, "登录验证失败");
				return json.jsonValue();
			}

			json.judgeUserType(ut);

			BosConstants.debug("1USERID=" + ut.getUserId() + "  token=" + json.getTokenKey() + "     " + ut.getPers());

			this.service.getRdClient().putUserToken(ut, json.getType());

			json.getData().put("UserToken", ut.jsonValue());
			json.getData().put("indexPage", BosConstants.indexPage);

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();

	}
	
	
	
	@RequestMapping(value = "/signOut", method = RequestMethod.GET)
	public JSONObject signOut(HttpServletRequest request, @RequestHeader String token) throws Exception {
		
		
		String lan = request.getHeader("LAN");// 语种 EN CN 等
		
		if(DataChange.isEmpty(lan)) {
			lan=BosConstants.defaultLan;
		}
		
		JSONObject json = new JSONObject(true);
		json.put("errorCode", 0);
		json.put("isSucc", true);
		json.put("msg", LabelDisplay.get("成功", lan));
		
		if (DataChange.isEmpty(token)) {
			return json;
		}
		
		
		
		try {
			
			UserToken ut = this.service.getRdClient().getUserToken(token);
			
			
			if (ut != null && !DataChange.isEmpty(ut.getUserId())) {

				this.service.getRdClient().deleteUserToken(token, ut.getUserId());
				// conn.deleteUser(ut.getUserId());
				CacheTools.removeJson("U" + ut.getUserId() + RdClient.splitChar + ut.getMemberId() + RdClient.splitChar
						+ Constants.current_sys + RdClient.splitChar + lan + RdClient.splitChar
						+ "BOSDYMENU");
				CacheTools.removeJson("U" + ut.getUserId() + RdClient.splitChar + ut.getMemberId() + RdClient.splitChar
						+ Constants.current_sys + RdClient.splitChar + lan + RdClient.splitChar
						+ "BOSDYNCHART");
				//CacheTools.removeJson("U" + ut.getUserId() + RdClient.splitChar + ut.getMemberId() + RdClient.splitChar
						//+ Constants.current_sys + RdClient.splitChar +lan + RdClient.splitChar + "MENU");
				
				
				PRoot search=new PRoot();
				search.setIsValid(1);
				QueryListInfo<PRoot> rootList=this.getService().getMgClient().getList(search, "sort",this.service);
				for(PRoot x:rootList.getList()) {
					CacheTools.removeJson("U" + ut.getUserId() + RdClient.splitChar + ut.getMemberId() + RdClient.splitChar
							+ x.getCode() + RdClient.splitChar +lan + RdClient.splitChar + "MENU");
				}
				
				
			}
			
			
			

			this.service.getRdClient().getJedis().del(lan);
			
			
			
		}catch(Exception e) {
			json.put("errorCode", -1);
			json.put("isSucc", false);
			json.put("msg", LabelDisplay.get(e.getMessage(), lan));
			
		}
		
		
		
		return json;
		
		
	}
	

	@Security(accessType = "0", displayName = "登出", isEntAdmin = false, isSysAdmin = false, needLogin = false)
	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject logout(@RequestBody HttpEntity json, HttpServletRequest request, @RequestHeader String token)
			throws Exception {

		// System.out.println("@RequestHeader token="+token);

		String tokenKey = json.getTokenKey();
		if (DataChange.isEmpty(tokenKey)) {
			json.setSuccess();
			return json.jsonValue();
		}

		try {

			UserToken ut = this.service.getRdClient().getUserToken(json.getTokenKey());

			// System.out.println("tokenKey="+ut.getTokenKey()+"
			// memberId="+ut.getMemberId());

			if (ut != null && !DataChange.isEmpty(ut.getUserId())) {

				this.service.getRdClient().deleteUserToken(tokenKey, ut.getUserId());
				// conn.deleteUser(ut.getUserId());
				CacheTools.removeJson("U" + ut.getUserId() + RdClient.splitChar + ut.getMemberId() + RdClient.splitChar
						+ Constants.current_sys + RdClient.splitChar + json.getLan() + RdClient.splitChar
						+ "BOSDYMENU");
				CacheTools.removeJson("U" + ut.getUserId() + RdClient.splitChar + ut.getMemberId() + RdClient.splitChar
						+ Constants.current_sys + RdClient.splitChar + json.getLan() + RdClient.splitChar
						+ "BOSDYNCHART");
				CacheTools.removeJson("U" + ut.getUserId() + RdClient.splitChar + ut.getMemberId() + RdClient.splitChar
						+ Constants.current_sys + RdClient.splitChar + json.getLan() + RdClient.splitChar + "MENU");
			}

			this.service.getRdClient().getJedis().del(tokenKey);
			if (ut == null) {
				ut = new UserToken();
			}
			ut.initForNew(NetWorkTime.getCurrentDate(), json.getType());
			json.setTokenKey(ut.getTokenKey());
			// ut=conn.getUserToken(json.getTokenKey());
			// System.out.println("memberId="+ut.getMemberId());

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	
	

	@Override
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}
}
