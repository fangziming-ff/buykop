package com.buykop.console.controller;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.SysLogService;
import com.buykop.console.entity.SysLog;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.BizFieldModifyTrack;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.office.excel.PSheet;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PFormAction;
import com.buykop.framework.scan.PFormMember;
import com.buykop.framework.scan.PFormRowAction;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PMapTJConfig;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.Calculation;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;
import com.buykop.console.util.Constants;


@Module(display = "数据库日志", sys = Constants.current_sys)
@RestController
@RequestMapping(SysLogController.URI)
public class SysLogController extends BaseController{
	
	private static Logger  logger=LoggerFactory.getLogger(SysLogController.class);
	
	protected static final String URI = "/sysLog";
	
	
	@Autowired
	private SysLogService service;
	
	
	
	@Menu(name = "系统日志", trunk = "开发服务,日志查询", js = "sysLog")
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "/fetch",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			
			SysLog search=json.getSearch(SysLog.class,null,ut,this.service);
			
			PageInfo page=json.getPageInfo(SysLog.class);
			
			QueryFetchInfo<SysLog> fetch=this.service.getMgClient().getFetch(search,"!logTime",  page.getCurrentPage(), page.getPageSize(),this.service);
			
			super.fetchToJson(fetch, json, BosConstants.getTable(SysLog.class));
			
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "/info",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			
			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys,URI+"/info", SysLog.class.getName(), "", true,this.getService());
			
			SysLog obj=this.service.getById(id, SysLog.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(SysLog.class);
				return json.jsonValue();
			}
			
			
			super.objToJson(obj, json);
			
			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	

	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}
}
