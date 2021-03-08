package com.buykop.console.controller;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.client.ServiceInstance;
//import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONArray;
import com.buykop.console.entity.product.Product;
import com.buykop.console.entity.product.ProductPage;
import com.buykop.console.entity.product.RPageInfBatch;
import com.buykop.console.service.MainService;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.check.ImageUtil;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.DiyInfBatch;
import com.buykop.framework.entity.DiyInfBatchItem;
import com.buykop.framework.entity.IpPools;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.UrlUtil;
import com.buykop.framework.io.VerifyCodeUtils;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRMemberRoot;
import com.buykop.framework.oauth2.PRMemberType;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.ChartForm;
import com.buykop.framework.scan.ConnectionInfo;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PFormMember;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PLanguage;
import com.buykop.framework.scan.PMapTJConfig;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.ServerConfig;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.ClientPool;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.ServiceUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.JsonUtil;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.query.BaseQuery;
import com.buykop.framework.util.sort.CommonComparator;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.Leaf;
import com.buykop.framework.util.type.ListData;
import com.buykop.framework.util.type.Menu;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;
import com.buykop.framework.util.type.TreadInf;
import com.buykop.console.util.Constants;

@Module(display = "主程序", sys = Constants.current_sys)
@RestController
@RequestMapping
public class MainController extends com.buykop.framework.util.type.MainController {

	private static Logger  logger=LoggerFactory.getLogger(MainController.class);

	@Autowired
	private MainService service;
	
	
	
	//获取连接列表  删除连接
	@Security(accessType = "1", displayName = "节点详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/serverInfo", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject serverInfo(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		
		json.setSys(Constants.current_sys);
		json.setUri("/serverInfo");
		
		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			
			
			ClientPool.initServerConfig();
			QueryListInfo<ConnectionInfo> list=new QueryListInfo<ConnectionInfo>();
			list.setList(ServiceUtil.currentConfig.getConnList());
			
			super.objToJson(ServiceUtil.currentConfig, json);
			
			super.listToJson(list, json, BosConstants.getTable(ConnectionInfo.class));

			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	

	@Security(accessType = "1", displayName = "系统初始化", needLogin = true, isEntAdmin = true, isSysAdmin = true)
	@RequestMapping(value = "/init", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject init(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {
		// TODO Auto-generated method stub
		try {
			this.service.init();
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}

	@Security(accessType = "0", displayName = "获取验证码链接地址", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/checkCode", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject checkCode(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		
		
		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			// Root root=this.service.getMgClient().getByPK(Constants.current_sys,
			// Root.class);

			json.getData().put("url", "/login/checkCode?token=" + ut.getTokenKey() + "&radom=" + Math.random());
			// json.getData().put("sys", root.getDisplayName());

			// 系统是否支持短信注册
			json.getData().put("sm", DataChange.replaceNull(PSysParam.paramValue("1", BosConstants.paramSM)));

			// 是否支持用户注册
			json.getData().put("register",
					DataChange.replaceNull(PSysParam.paramValue("1", BosConstants.paramUserRegister)));

			json.getData().put("webCheckCode",
					DataChange.replaceNull(PSysParam.paramValue("1", BosConstants.paramWebCheckCode)));

			Long fileUploadMax = PSysParam.paramLongValue("1", BosConstants.paramFileUploadMax);
			if (fileUploadMax == null) {
				fileUploadMax = 150L;
			}
			json.getData().put(BosConstants.paramFileUploadMax, fileUploadMax.toString());

			// BosEntity ois=service._getJsonById("1", Constants.memberClassName);
			String memberId = "1";
			if (!DataChange.isEmpty(json.getDomain())) {
				memberId = CacheTools.getDomainMemberId(json.getDomain());
			}
			if (DataChange.isEmpty(memberId)) {
				memberId = "1";
			}

			BosEntity member = null;

			try {
				member = this.service.getById(memberId, BosConstants.memberClassName);
				if (member == null) {
					member = this.service.getById("1", BosConstants.memberClassName);
				}
			} catch (Exception e) {
				e.printStackTrace();
				BosConstants.debug("class:" + BosConstants.memberClassName + " is not exist");
			}

			String copyRight = null;
			String diySysName = null;
			String indexPage = null;

			if (member != null) {
				copyRight = member.propertyValueString("shortName");
				if (DataChange.isEmpty(copyRight)) {
					copyRight = member.propertyValueString("name");
				}
				diySysName = member.propertyValueString("diySysName");
				indexPage = member.propertyValueString("indexPage");
			}

			if (DataChange.isEmpty(diySysName)) {
				diySysName = Constants.current_sys_name;
			}
			if (DataChange.isEmpty(copyRight)) {
				copyRight = BosConstants.copyright;
			}
			if (DataChange.isEmpty(indexPage)) {
				indexPage = BosConstants.indexPage;
			}

			json.getData().put("copyRight", copyRight);
			json.getData().put("sysName", diySysName);
			json.getData().put("indexPage", indexPage);

			BosConstants.debug("domain=" + json.getDomain() + "  memberId=" + memberId + "  indexPage=" + indexPage);

			PLanguage lan = new PLanguage();
			lan.setStatus(1L);
			QueryListInfo<PLanguage> list = this.service.getMgClient().getList(lan, "seq",this.service);
			super.listToJson(list, json, BosConstants.getTable(PLanguage.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	

	// @Security(accessType = "0", displayName = "图形验证码", isEntAdmin = false,
	// isSysAdmin = false, needLogin = false)
	@RequestMapping(value = "/login/checkCode", method = RequestMethod.GET)
	public void checkCode(HttpServletRequest request, HttpServletResponse response,String token) throws Exception {

		if (DataChange.isEmpty(token))
			return;

		response.setHeader("Pragma", "No-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setContentType("image/jpeg");

		

		Long checkCodeType = PSysParam.paramLongValue("1", "checkCodeType");
		if (checkCodeType == null) {
			checkCodeType = 0L;
		}

		String verifyCode = null;

		try {

			

			if (checkCodeType.intValue() == 1) {
				verifyCode = VerifyCodeUtils.generateVerifyCode(4);
				this.getService().getRdClient().setTokenParam(token, VerifyCodeUtils.class.getName(), "checkCode", verifyCode, 10 * 60);
			} else {
				String[] arr = VerifyCodeUtils.generateVerifyCode();
				this.service.getRdClient().setTokenParam(token, VerifyCodeUtils.class.getName(), "checkCode", arr[1], 10 * 60);
				verifyCode = arr[0];
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		BosConstants.debug("token=" + token + "  verifyCode=" + verifyCode + "   checkCodeType=" + checkCodeType);

		// 生成图片
		int w = 100, h = 30;
		OutputStream out = response.getOutputStream();
		VerifyCodeUtils.outputImage(w, h, out, verifyCode);

	}

	@Security(accessType = "1", displayName = "线程执行", needLogin = true, isEntAdmin = false, isSysAdmin = true)
	@RequestMapping(value = "/execThread", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject execThread(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/execThread", PThread.class.getName(), null,
					true,this.getService());
			PThread obj = this.service.getMgClient().getById(id, PThread.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PThread.class);
				return json.jsonValue();
			}

			TreadInf inf = (TreadInf) Class.forName(obj.getClassName()).newInstance();
			inf.invoke(ut.getUserId());

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1*,2*", displayName = "加载树形结构(字段选择)", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/loadTreeForFieldSelect", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject loadTreeForFieldSelect(@RequestBody HttpEntity json, HttpServletRequest request)
			throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String treeId = json.getSelectedId(Constants.current_sys, "/loadTreeForFieldSelect",
					PTreeForm.class.getName(), "", true,this.getService());

			PTreeForm tree = this.service.getMgClient().getById(treeId, PTreeForm.class);
			if (tree == null) {
				json.setUnSuccessForNoRecord(PTreeForm.class, null);
				return json.jsonValue();
			}

			String idValue = json.getSelectedId(Constants.current_sys, "/loadTreeForFieldSelect_idValue",
					tree.getClassName(), "", false,this.getService());

			Vector<String> selectedV = MyString.splitBy(idValue, ",");

			super.loadTree(tree, null, selectedV, null, json, ut);

			if (json.getErrorCode() != 0) {
				return json.jsonValue();
			}

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String test(HttpServletRequest request) {

		try {

			if (ServiceUtil.currentConfig != null) {
				ServiceUtil.currentConfig.initServerId();
				ServiceUtil.currentConfig.setProfile(BosConstants.runEnvironmental);
				this.service.save(ServiceUtil.currentConfig, null);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			return this.getIpAddr(request);
		} catch (Exception e) {
			return e.getLocalizedMessage();
		}
	}

	@Security(accessType = "0", displayName = "测试", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/test", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject test(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(UserToken.createToken(true));
		}
		
		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			if (ServiceUtil.currentConfig != null) {
				ServiceUtil.currentConfig.initServerId();
				ServiceUtil.currentConfig.setProfile(BosConstants.runEnvironmental);
				this.service.save(ServiceUtil.currentConfig, null);
			}

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "内部通知", needLogin = true, isEntAdmin = false, isSysAdmin = true)
	@RequestMapping(value = "/innerNotice", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject innerNotice(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			if(!BosConstants.inited) {
				//BosConstants.debug(ServiceUtil.currentConfig.getIp()+"   "+Constants.current_sys+" is not inited-----------------------");
				return json.jsonValue();
			}

			// if(!Application.init) return json.jsonValue();

			String ip = this.getIpAddr(request);
			super._innerNotice(json, ip);

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "0", displayName = "调用服务", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/call", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject call(HttpServletRequest request, HttpServletResponse response, @RequestBody HttpEntity json)
			throws Exception {

		long start = NetWorkTime.getCurrentDatetime().getTime();

		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(UserToken.createToken(true));
		}
		

		try {

			Date currentTime = NetWorkTime.getCurrentDatetime();

			if (json.getData() == null) {
				json.setData(new JSONObject(true));
			}

			json.getData().put("sysTime", DataChange.timeToString(currentTime));

			BosConstants.debug("call1(" + json.getUri() + ")  token=" + json.getTokenKey());

			UserToken ut = this.getService().getRdClient().getUserToken(json.getTokenKey());

			String ip = this.getIpAddr(request);

			try {

				if (ip.equals("127.0.0.1")) {
					ip = BosConstants.LOCAL_IP;
				}

				json.setIp(ip);

				PRoot root = this.service.getMgClient().getById(json.getSys(), PRoot.class);
				if (root == null) {
					json.setUnSuccess(-1, LabelDisplay.get("远程服务  ", json.getLan()) + json.getSys() + "|"
							+ json.getUri() + LabelDisplay.get(" 系统 不存在", json.getLan()));
					return json.jsonValue();
				}

				//// 0:自动 1:人工
				ServerConfig server = ServiceUtil.getServer(json.getSys());
				if (server == null && DataChange.getLongValueWithDefault(root.getRegType(), 1) == 1) {
					server = ServiceUtil.getServer(Constants.current_sys);
				}

				// ServerConfig server = ServiceUtil.getServer(json.getSys());

				if (server == null) {
					json.setUnSuccess(-1, "远程服务  " + json.getSys() + "|" + json.getUri() + " 不存在 活动的 服务节点");
					return json.jsonValue();
				}

				BosConstants.debug("远程服务  " + json.getSys() + "|" + json.getUri() + " 活动的 服务节点:" + server.getPk()
						+ "    " + server.getSys() + "    " + server.getProfile());

				server.setAccessTime(currentTime);
				this.service.save(server, null);

				JSONObject ret = UrlUtil.doPostJson("http://" + server.getIp() + ":" + server.getPort() + json.getUri(),
						JsonUtil.getInstance().object2String(json));

				if (ret == null) {
					// Pool.getInstance().getConn().deleteByPK(monitor.getPk(),
					// PServiceUriMonitor.class);
					if (BosConstants.runTimeMode()) {
						json.setUnSuccess(-1, "远程调用失败");
					} else {
						json.setUnSuccess(-1, "远程调用 " + server.getPk() + "失败");
					}
					return json.jsonValue();
				}

				json.setData(ret.getJSONObject("data"));
				json.setErrorCode(ret.getIntValue("errorCode"));
				json.setMsg(ret.getString("msg"));
				json.setCurrentUserType(ret.getInteger("currentUserType"));
				json.setIsLogin(ret.getInteger("isLogin"));
				json.setPermissionCheck(ret.getString("permissionCheck"));
				json.setTokenKey(ret.getString("tokenKey"));

			} catch (Exception e) {
				json.setUnSuccess(e);
			} finally {
				BosConstants.debug("执行时间:" + (NetWorkTime.getCurrentDatetime().getTime() - start) + "毫秒");
			}

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "0", displayName = "token验证", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/tokenCheck", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject tokenCheck(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken token = this.getService().getRdClient().getUserToken(json.getTokenKey());

			if (token == null) {
				json.setUnSuccess(-1, "令牌验证不存在");
				return json.jsonValue();
			}

			if (token.isOverTime(NetWorkTime.getCurrentDatetime())) {
				json.setUnSuccess(20, "令牌已过期");
				return json.jsonValue();
			}

			json.getData().put("UserToken", token.jsonValue());

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "0", displayName = "获取token", isEntAdmin = false, isSysAdmin = false, needLogin = false)
	@RequestMapping(value = "/getToken", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject getToken(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(UserToken.createToken(false));
		}
		
		try {

			UserToken ut = super.securityCheck(json, request);

			json.getData().put("UserToken", ut.jsonValue());

			json.getData().put("indexPage", BosConstants.indexPage);

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();

	}

	@Security(accessType = "0", displayName = "联动下拉", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/linkAge", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject linkAge(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(UserToken.createToken(false));
		}
		
		
		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			super._linkAge(json, this.service);

			json.getData().put("link", "1");

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();

	}

	@Security(accessType = "0", displayName = "", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(UserToken.createToken(false));
		}
		
		try {

			if (DataChange.isEmpty(json.getTokenKey())) {
				json.setTokenKey(UserToken.createToken(true));
			}

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSimpleData("sys", "所属系统", String.class, true,this.getService());
			String simpleName = json.getSimpleData("sys", "表对象简写", String.class, true,this.getService());
			String id = json.getSimpleData("id", "主键值", String.class, true,this.getService());

			Table table = Table.getSysTableBySimpleName(sys, simpleName, this.service);
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class, null);
				return json.jsonValue();
			}

			BosEntity obj = this.service.getById(id, table.getClassName());
			if (obj == null) {
				json.setUnSuccessForNoRecord(table.getClassName(), null);
				return json.jsonValue();
			}

			super.objToJson(obj, json);

			HashMap<String, Field> fieldHash = Table.getSlaverHash(table.getClassName());
			if (fieldHash != null && fieldHash.size() > 0) {

				Iterator<String> its = fieldHash.keySet().iterator();
				while (its.hasNext()) {
					String cl = its.next();
					BosEntity slave = new TableJson(cl);

					if (slave.showTable() == null)
						continue;

					slave.putValue(fieldHash.get(cl), id);

					QueryListInfo<BosEntity> sList = this.service.getList(slave, slave.showTable().getSortField());
					super.listToJson(sList, json, slave.showTable());
				}

			}

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();

	}

	
	// --------------------------------------
	@Security(accessType = "1", displayName = "数据缓存Map内对象列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/cacheDataListForMap", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject cacheDataListForMap(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String key = json.getSimpleData("key", "键值", String.class, true,this.getService());

			QueryListInfo<ListData> list = new QueryListInfo<ListData>();

			Vector<String> delV = new Vector<String>();

			Map<String, ListData> map = BosConstants.getExpireHash().getDataListMap(key);
			Iterator<String> its = map.keySet().iterator();
			while (its.hasNext()) {
				String k = its.next();
				ListData data = map.get(k);
				if (data == null || !data.check()) {
					delV.add(k);
					continue;
				}
				list.getList().add(data);
			}

			for (String x : delV) {
				map.remove(x);
			}

			super.listToJson(list, json, BosConstants.getTable(ListData.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据缓存Map内对象删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/delCacheDataListForMap", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delCacheDataListForMap(@RequestBody HttpEntity json, HttpServletRequest request)
			throws Exception {


		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String key = json.getSimpleData("key", "键值", String.class, true,this.getService());

			String id = json.getSimpleData("id", "删除id", String.class, true,this.getService());

			QueryListInfo<ListData> list = new QueryListInfo<ListData>();

			Vector<String> delV = new Vector<String>();

			Map<String, ListData> map = BosConstants.getExpireHash().getDataListMap(key);
			map.remove(id);

			Iterator<String> its = map.keySet().iterator();
			while (its.hasNext()) {
				String k = its.next();
				ListData data = map.get(k);
				if (data == null || !data.check()) {
					delV.add(k);
					continue;
				}
				list.getList().add(data);
			}

			super.listToJson(list, json, BosConstants.getTable(ListData.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	// @PathVariable("id") String id
	@Security(accessType = "0", displayName = "首页", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/index", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject index(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {

			String current_sys = json.getSelectedId(Constants.current_sys, "/index", PRoot.class.getName(), "", true,this.getService());
			if(DataChange.isEmpty(current_sys)) {
				current_sys=Constants.current_sys;
			}
			
			if (DataChange.isEmpty(json.getTokenKey())) {
				json.setTokenKey(UserToken.createToken(true));
			}

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			try {
				super.initIndex(json, ut, current_sys);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			
			
			
			
			json.getData().put("indexPage", BosConstants.indexPage);
			
			
			
			JSONObject  userInfo=new JSONObject();
			userInfo.put("email", ut.getMail());
			userInfo.put("userName", ut.getUserName());
			json.getData().put("userInfo", userInfo);

			json.setSuccess();

		} catch (Exception e) {
			e.printStackTrace();
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@RequestMapping(value = "/script", method = RequestMethod.GET)
	@ResponseBody
	public void script(HttpServletResponse response, String style, String js) throws Exception {

		response.setHeader("Pragma", "No-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setContentType("text/plain;charset=UTF-8");

		if (DataChange.isEmpty(js)) {
			response.getWriter().write("alert('参数为空');");
			return;
		}

		String sys = js.substring(0, js.indexOf("/"));
		String jsName = js.substring(js.indexOf("/") + 1, js.length());

		if (jsName.toLowerCase().endsWith(".js")) {
			jsName = jsName.substring(0, jsName.length() - 3);
		}

		// String script = JedisPool.getInstance().getConn().getJSScript(sys, jsName);

		// if (DataChange.isEmpty(script)) {
		// response.getWriter().write("alert('JS脚本不存在');");
		response.getWriter().write("/console/js/" + style + "/module/" + js);
		return;
		// }

		// response.getWriter().write(script);
	}

	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}

	@Override
	public JSONObject execShell(HttpEntity json, HttpServletRequest request) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	protected JSONObject getSummaryForIndex(UserToken token, ServiceInf service) throws Exception {

		JSONObject msgJson = new JSONObject(true);

		msgJson.put("userTask", "0");
		msgJson.put("wfAgency", "0");
		msgJson.put("wfCC", "0");
		msgJson.put("sysNotice", "0");
		msgJson.put("memberNotice", "0");

		/**
		 * Long wfSupport=PSysParam.paramLongValue("1", Constants.paramWFSupport);
		 * if(wfSupport==null || wfSupport.intValue()!=0) { wfSupport=1L; }
		 * 
		 * 
		 * PRMemberType type=new PRMemberType(); type.setMemberId(token.getMemberId());
		 * type.setStatus(2L); Vector<String>
		 * memberTypeV=this.service.getMgClient().getVector(type, "typeId");
		 * 
		 * if(!memberTypeV.contains(Constants.memberType_workflow)) { wfSupport=0L; }
		 * 
		 * 
		 * 
		 * 
		 * 
		 * if(wfSupport.intValue()==1) {
		 * 
		 * //工作流 BosEntity wf=new TableJson(Constants.workflowCaseClassName);
		 * wf.setCurrentUserId(token.getUserId()); wf=service.getBaseDao().queryObj(wf,
		 * "getList_count"); if(wf!=null) {//代办 msgJson.put("wfAgency", wf.get_count());
		 * } //抄送的任务 wf=new TableJson(Constants.workflowCaseClassName);
		 * wf.setCurrentUserId(token.getUserId()); wf=service.getBaseDao().queryObj(wf,
		 * "getCCList_count"); if(wf!=null) {//代办 msgJson.put("wfCC", wf.get_count()); }
		 * }
		 * 
		 * 
		 * BosEntity wf=new TableJson(Constants.noticeClassName); wf.putValue("userId",
		 * token.getUserId()); wf.putValue("isRead", 0L); wf.putValue("status", 1L);
		 * wf.putValue("noticeType", 0L); wf=service.getBaseDao().queryObj(wf,
		 * "getMyList_count"); if(wf!=null) {//系统消息 msgJson.put("sysNotice",
		 * wf.get_count()); }else { msgJson.put("sysNotice", "0"); }
		 * 
		 * wf=new TableJson(Constants.noticeClassName); wf.putValue("userId",
		 * token.getUserId()); wf.putValue("isRead", 0L); wf.putValue("noticeType", 1L);
		 * wf.putValue("status", 1L); wf.putValue("memberId", token.getMemberId());
		 * //wf.putValue("orgId", token.getOrgId());
		 * wf=service.getBaseDao().queryObj(wf, "getMyList_count"); if(wf!=null) {//机构消息
		 * msgJson.put("memberNotice", wf.get_count()); }else {
		 * msgJson.put("memberNotice", "0"); }
		 * 
		 * 
		 * if(true) { BosEntity search = new TableJson(Constants.taskClassName);
		 * search.putValue("status", "0"); search.setOwnerUserId(token.getUserId());
		 * long count=service.getBaseDao().getCount(search); msgJson.put("userTask",
		 * String.valueOf(count)); }
		 */

		return msgJson;
	}

}
