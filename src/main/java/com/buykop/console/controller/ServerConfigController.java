package com.buykop.console.controller;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.ServerConfigService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.UrlUtil;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PServiceControllerMonitor;
import com.buykop.framework.scan.ServerConfig;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.JsonUtil;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;


@Module(display = "系统", sys = Constants.current_sys)
@RestController
@RequestMapping(ServerConfigController.CODE)
public class ServerConfigController extends BaseController{
	
	
	protected static final String CODE="/server";
	
	private static Logger  logger=LoggerFactory.getLogger(ServerConfigController.class);
	
	@Autowired
	private ServerConfigService service;
	
	
	
	
	@Menu(js = "server", name = "服务节点", trunk = "开发服务,开发管理")
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/list", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject list(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			
			if(true) {
				ServerConfig search = json.getSearch(ServerConfig.class,null,ut,this.service);
				QueryListInfo<ServerConfig> list = this.service.getMgClient().getList(search,"sys,ip,port",this.service);
				super.listToJson(list, json, BosConstants.getTable(ServerConfig.class));
				
			}
			
			
			if(true) {
				PServiceControllerMonitor search=json.getSearch(PServiceControllerMonitor.class, null, ut,this.service);
				QueryListInfo<PServiceControllerMonitor> list = this.service.getMgClient().getList(search,"sys,ip,port,className",this.service);
				super.listToJson(list, json, BosConstants.getTable(PServiceControllerMonitor.class));
			}
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	
	
	
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			String id=json.getSelectedId(Constants.current_sys, CODE+"/info", ServerConfig.class.getName(), null, true, service);
			
			
			ServerConfig obj=this.service.getById(id, ServerConfig.class);
			
			if(obj==null) {
				json.setUnSuccessForNoRecord(ServerConfig.class,id);
				return json.jsonValue();
			}
			
			
			JSONObject ret = UrlUtil.doPostJson("http://" + obj.getIp() + ":" + obj.getPort() + "/serverInfo",json.jsonValue().toJSONString());

			if (ret == null) {
				// PServiceUriMonitor.class);
				if (BosConstants.runTimeMode()) {
					json.setUnSuccess(-1, "远程调用失败");
				} else {
					json.setUnSuccess(-1, LabelDisplay.get("远程调用 ", json.getLan()) + obj.getPk()
							+ LabelDisplay.get("失败", json.getLan()));
				}
				return json.jsonValue();
			} else {
				// Pool.getInstance().getConn().save(monitor);
			}

			json.setData(ret.getJSONObject("data"));
			json.setErrorCode(ret.getIntValue("errorCode"));
			if (ret.containsKey("isSucc")) {
				json.setSucc(ret.getBooleanValue("isSucc"));
			} else if (ret.containsKey("succ")) {
				json.setSucc(ret.getBooleanValue("succ"));
			}
			json.setMsg(ret.getString("msg"));
			json.setTokenKey(ret.getString("tokenKey"));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}	
			
	
	
	@Security(accessType = "1", displayName = "保存列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			List<ServerConfig> list=json.getList(ServerConfig.class, null,this.service);
			for(ServerConfig x:list) {
				this.service.save(x, null);
			}
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}


	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/server/delete",ServerConfig.class.getName(),null,true,this.getService());
			
			ServerConfig obj=this.service.getMgClient().getById(id, ServerConfig.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(ServerConfig.class);
				return json.jsonValue();
			}
			
			
			this.service.getMgClient().deleteByPK(id, ServerConfig.class,ut,this.service);
		
			json.setSuccess("删除成功");
		} catch (Exception e) {
			json.setUnSuccess(e);
			
		}

		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "服务节点上数据缓存列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/cacheRemove", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject cacheRemove(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			String id=json.getSelectedId(Constants.current_sys, CODE+"/cacheList", ServerConfig.class.getName(), null, true,this.getService());
			ServerConfig obj=this.service.getMgClient().getById(id, ServerConfig.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(ServerConfig.class);
				return json.jsonValue();
			}
			
			String key=json.getSelectedId(Constants.current_sys, CODE+"/cacheRemove", ServerConfig.class.getName(), null, true,this.getService());

			HttpEntity req=json.clone(obj.getSys(), "/cache/remove");
			req.getData().put("key", key);
			
			
			
			JSONObject ret = UrlUtil.doPostJson("http://" + obj.getIp() + ":" + obj.getPort() + req.getUri(),JsonUtil.getInstance().object2String(req));

			if (ret == null) {
				json.setUnSuccess(-1, LabelDisplay.get("远程调用 ", json.getLan())  +obj.getPk()+ "    "+req.getUri() + LabelDisplay.get("失败", json.getLan()),true);
				return json.jsonValue();
			}
			
			String code=DataChange.replaceNull(ret.getString("errorCode"));
			json.setMsg(DataChange.replaceNull(ret.getString("msg")));
			if(!code.equals("0")) {
				json.setErrorCode(DataChange.StringToInteger(code));
				return json.jsonValue();
			}
			
			json.setData(ret.getJSONObject("data"));
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "服务节点上数据缓存清除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/cacheClear", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject cacheClear(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			String id=json.getSelectedId(Constants.current_sys, CODE+"/cacheClear", ServerConfig.class.getName(), null, true,this.getService());
			ServerConfig obj=this.service.getMgClient().getById(id, ServerConfig.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(ServerConfig.class);
				return json.jsonValue();
			}
			

			HttpEntity req=json.clone(obj.getSys(), "/cache/clear");
			
			
			JSONObject ret = UrlUtil.doPostJson("http://" + obj.getIp() + ":" + obj.getPort() + req.getUri(),JsonUtil.getInstance().object2String(req));

			if (ret == null) {
				json.setUnSuccess(-1,LabelDisplay.get("远程调用 ", json.getLan())  +obj.getPk()+ "    "+req.getUri() +LabelDisplay.get("失败", json.getLan())  );
				return json.jsonValue();
			}
			
			String code=DataChange.replaceNull(ret.getString("errorCode"));
			json.setMsg(DataChange.replaceNull(ret.getString("msg")));
			if(!code.equals("0")) {
				json.setErrorCode(DataChange.StringToInteger(code));
				return json.jsonValue();
			}
			
			json.setData(ret.getJSONObject("data"));
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "服务节点上数据缓存详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/cacheInfo", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject cacheInfo(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			String id=json.getSelectedId(Constants.current_sys, CODE+"/cacheList", ServerConfig.class.getName(), null, true,this.getService());
			ServerConfig obj=this.service.getMgClient().getById(id, ServerConfig.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(ServerConfig.class);
				return json.jsonValue();
			}
			
			String key=json.getSelectedId(Constants.current_sys, CODE+"/cacheInfo", ServerConfig.class.getName(), null, true,this.getService());

			
			HttpEntity req=json.clone(obj.getSys(), "/cache/info");
			req.getData().put("key", key);
			
			JSONObject ret = UrlUtil.doPostJson("http://" + obj.getIp() + ":" + obj.getPort() + req.getUri(),JsonUtil.getInstance().object2String(req));

			if (ret == null) {
				json.setUnSuccess(-1,LabelDisplay.get("远程调用 ", json.getLan())  +obj.getPk()+ "    "+req.getUri() + LabelDisplay.get("失败", json.getLan()) ,true);
				return json.jsonValue();
			}
			
			String code=DataChange.replaceNull(ret.getString("errorCode"));
			json.setMsg(DataChange.replaceNull(ret.getString("msg")));
			if(!code.equals("0")) {
				json.setErrorCode(DataChange.StringToInteger(code));
				return json.jsonValue();
			}
			
			json.setData(ret.getJSONObject("data"));
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "服务节点上数据缓存列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/cacheList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject cacheList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			String id=json.getSelectedId(Constants.current_sys, CODE+"/cacheList", ServerConfig.class.getName(), null, true,this.getService());
			ServerConfig obj=this.service.getMgClient().getById(id, ServerConfig.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(ServerConfig.class);
				return json.jsonValue();
			}
			
			
			HttpEntity req=json.clone(obj.getSys(), "/cache/list");
			
			JSONObject ret = UrlUtil.doPostJson("http://" + obj.getIp() + ":" + obj.getPort() + req.getUri(),JsonUtil.getInstance().object2String(req));

			if (ret == null) {
				json.setUnSuccess(-1,LabelDisplay.get("远程调用 ", json.getLan())  +obj.getPk()+ "    "+req.getUri() +LabelDisplay.get("失败", json.getLan()) ,true);
				return json.jsonValue();
			}
			
			String code=DataChange.replaceNull(ret.getString("errorCode"));
			json.setMsg(DataChange.replaceNull(ret.getString("msg")));
			if(!code.equals("0")) {
				json.setErrorCode(DataChange.StringToInteger(code));
				return json.jsonValue();
			}
			
			json.setData(ret.getJSONObject("data"));
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "服务节点上警示列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/altList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject altList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			String id=json.getSelectedId(Constants.current_sys, CODE+"/altList", ServerConfig.class.getName(), null, true,this.getService());
			ServerConfig obj=this.service.getMgClient().getById(id, ServerConfig.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(ServerConfig.class);
				return json.jsonValue();
			}
			
			
			PServiceControllerMonitor search=json.getSearch(PServiceControllerMonitor.class, null, ut,this.service);
			search.setIp(obj.getIp());
			search.setPort(obj.getPort());
			QueryListInfo<PServiceControllerMonitor> list = this.service.getMgClient().getList(search,"!checkTime,className",this.service);
			
			super.listToJson(list, json, BosConstants.getTable(PServiceControllerMonitor.class));
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}

}