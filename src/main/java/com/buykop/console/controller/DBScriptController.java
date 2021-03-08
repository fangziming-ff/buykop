package com.buykop.console.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.DBScriptService;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.DBScript;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.ThreadExecRecord;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;



@Module(display = "数据库脚本", sys = Constants.current_sys)
@RestController
@RequestMapping(DBScriptController.CODE)
public class DBScriptController extends BaseController{
	
	private static Logger  logger=LoggerFactory.getLogger(DBScriptController.class);
	
	protected static final String CODE="/dbScript";

	
	@Autowired
	private DBScriptService service;
	
	
	
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}


			DBScript search = json.getSearch(DBScript.class,null,ut,this.service);
			PageInfo page=json.getPageInfo(DBScript.class);
			
			QueryFetchInfo<DBScript> list = this.service.getMgClient().getFetch(search,"sys,!execTime",page.getCurrentPage(),page.getPageSize(),this.service);
			super.fetchToJson(list, json,BosConstants.getTable(DBScript.class));
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, DBScript.class, "sys");
			
			

			//super.selectToJson(Field.getJsonForSelectDateRange(DBScript.class.getName(),null), json, DBScript.class.getSimpleName(), "queryDateProperty");
			//super.selectToJson(Field.getJsonForSelectNumRange(DBScript.class.getName(),null), json, DBScript.class.getSimpleName(), "queryNumProperty");
			//super.selectToJson(search.showTable().getFieldJsonForSelectCodeValue(json,null), json, DBScript.class.getSimpleName(), "queryCodeValueProperty");
			
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	
	
	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		
		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, CODE+"/delete",DBScript.class.getName(),null,true,this.getService());
			
			DBScript obj=this.service.getMgClient().getById(id, DBScript.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(DBScript.class,id);
				return json.jsonValue();
			}
			
			
			this.service.getMgClient().deleteByPK(id, DBScript.class,ut,this.service);
			
			ThreadExecRecord record=new ThreadExecRecord();
			record.setClassName(id);
			this.service.getMgClient().delete(record,ut,this.service);
			

			json.setSuccess("删除成功");
		} catch (Exception e) {
			json.setUnSuccess(e);
			
		}

		return json.jsonValue();
	}
	
	
	
	
	

	@Security(accessType = "1", displayName = "显示新增", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/showAdd", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject showAdd(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			DBScript obj=new DBScript();
			obj.setScriptId(DBScript.next());
			super.objToJson(obj, json);
			
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, DBScript.class, "sys");
			
	
		} catch (Exception e) {
			json.setUnSuccess(e);
			
		}

		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, CODE+"/info",DBScript.class.getName(),null,true,this.getService());
			
			DBScript obj=this.service.getMgClient().getById(id, DBScript.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(DBScript.class,id);
				return json.jsonValue();
			}
			super.objToJson(obj, json);
			
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, DBScript.class, "sys");
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			
		}

		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			DBScript obj=json.getObj(DBScript.class,null,this.service);
			
			if(obj.getNeedExec()==null) {
				obj.setNeedExec(0L);
			}
			obj.setUserId(ut.getUserId());
			

			if(obj.getNeedExec().intValue()==1) {
				obj.setExecTime(NetWorkTime.getCurrentDatetime());
				try {
					this.service.getBaseDao().runScript(obj.sqlScript());
					obj.setRemark(BosConstants.RET_SUCCESS);
				}catch(Exception e) {
					obj.setStatus(0L);
					obj.setRemark(e.getMessage());
				}
				obj.setEndTime(NetWorkTime.getCurrentDatetime());
			}
			
			this.service.save(obj,ut);
			json.setSuccess("保存成功");
			

		} catch (Exception e) {
			json.setUnSuccess(e);
			
		}

		return json.jsonValue();
	}
	

	@Security(accessType = "1", displayName = "执行", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/exec", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject exec(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			
			String id = json.getSelectedId(Constants.current_sys, CODE+"/exec",DBScript.class.getName(),null,true,this.getService());
			
			DBScript obj=this.service.getMgClient().getById(id, DBScript.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(DBScript.class,id);
				return json.jsonValue();
			}
			obj.setExecTime(NetWorkTime.getCurrentDatetime());
			try {
				this.service.getBaseDao().runScript(obj.sqlScript());
				obj.setRemark(BosConstants.RET_SUCCESS);
			}catch(Exception e) {
				obj.setStatus(0L);
				obj.setRemark(e.getMessage());
			}
			obj.setEndTime(NetWorkTime.getCurrentDatetime());
			this.service.save(obj,ut);
			

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
