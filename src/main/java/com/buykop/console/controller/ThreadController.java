package com.buykop.console.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.ThreadService;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PServiceControllerMonitor;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.scan.ThreadExecRecord;
import com.buykop.framework.scan.ServerConfig;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;

@Module(display = "线程", sys = Constants.current_sys)
@RestController
@RequestMapping(ThreadController.CODE)
public class ThreadController extends BaseController{
	
	
	protected static final String CODE="/thread";
	
	private static Logger  logger=LoggerFactory.getLogger(ThreadController.class);
	
	
	@Autowired
	private ThreadService service;
	
	@Menu(js = "thread", name = "线程监控", trunk = "开发服务,开发管理")
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/list", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject list(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {



			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}
			
			HashMap<String,SelectBidding> sbHash=new HashMap<String,SelectBidding>();


			PThread search = json.getSearch(PThread.class,null,ut,this.service);
			
			QueryListInfo<PThread> list = this.service.getMgClient().getList(search,"sys,className",this.service);
			
			super.listToJson(list, json, BosConstants.getTable(PThread.class));
			
				
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	
	
	
	@Security(accessType = "1", displayName = "详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, CODE+"/info", PThread.class.getName(), null, true,this.getService());
			
			PThread obj=this.service.getById(id,PThread.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PThread.class,id);
				return json.jsonValue();
			}
			
			super.objToJson(obj, json);
			
			super.selectToJson(ServerConfig.getJsonForSelect(obj.getSys(),this.service), json, PThread.class, "serverId");

			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}	
	
	
	
	
	
	
	
	@Security(accessType = "1", displayName = "保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			//String id=json.getSelectedId(Constants.current_sys, CODE+"/info", PThread.class.getName(), null, true);
			
			PThread obj=json.getObj(PThread.class, null, this.service);
			this.service.save(obj, ut);
			
			super.objToJson(obj, json);
			
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
			
			List<PThread> list=json.getList(PThread.class, null,this.service);
			for(PThread x:list) {
				this.service.save(x, ut);
			}
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = true)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			
			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, CODE+"/delete",PThread.class.getName(),null,true,this.getService());
			
			PThread obj=this.service.getMgClient().getById(id, PThread.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PThread.class,id);
				return json.jsonValue();
			}
			
			
			PThread.delete(id,this.service);
			
			json.setSuccess("删除成功");
		} catch (Exception e) {
			json.setUnSuccess(e);
			
		}

		return json.jsonValue();
	}
	
	

	@Security(accessType = "1", displayName = "执行记录列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/execList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject execList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

	
			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			String id=json.getSelectedId(Constants.current_sys, CODE+"/execList", PThread.class.getName(), null, true,this.getService());
			PThread obj=this.service.getMgClient().getById(id, PThread.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PThread.class,id);
				return json.jsonValue();
			}
			
			
			ThreadExecRecord search=json.getSearch(ThreadExecRecord.class, null, ut,this.service);
			PageInfo page=json.getPageInfo(ThreadExecRecord.class);
			search.setClassName(id);
			QueryFetchInfo<PServiceControllerMonitor> list =this.service.getMgClient().getFetch(search,"!execTime",page.getCurrentPage(),page.getPageSize(),this.service);
			
			super.fetchToJson(list, json,BosConstants.getTable(ThreadExecRecord.class));
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	//@Menu(name = "线程日志", trunk = "统计查询,日志查询", js = "threadExec")
	@Security(accessType = "1", displayName = "执行记录列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/execFetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject execFetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			
			ThreadExecRecord search=json.getSearch(ThreadExecRecord.class, null, ut,this.service);
			PageInfo page=json.getPageInfo(ThreadExecRecord.class);
			QueryFetchInfo<ThreadExecRecord> list = this.service.getMgClient().getFetch(search,"!execTime,className",page.getCurrentPage(),page.getPageSize(),this.service);
			
			super.fetchToJson(list, json, BosConstants.getTable(ThreadExecRecord.class));
			
			
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "手工执行", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/manualExec", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject manualExec(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		try {


			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id = json.getSelectedId(Constants.current_sys, CODE+"/manualExec",PThread.class.getName(),null,true,this.getService());
			
			PThread obj=this.service.getMgClient().getById(id, PThread.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PThread.class);
				return json.jsonValue();
			}
			
			obj.remoteCall(json,this.service);

			
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

