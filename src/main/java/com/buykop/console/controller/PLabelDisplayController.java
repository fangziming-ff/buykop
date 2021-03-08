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
import com.buykop.console.service.PInputCheckService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.LableLanDisplay;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;


@Module(display = "中文标签", sys = Constants.current_sys)
@RestController
@RequestMapping(PLabelDisplayController.URI)
public class PLabelDisplayController extends BaseController{
	
	protected static final String URI="/labelDisplay";
	
	private static Logger  logger=LoggerFactory.getLogger(PLabelDisplayController.class);
	
	@Autowired
	private PInputCheckService service;
	
	
	
	@Menu(js = "labelDisplay", name = "标签管理", trunk = "基础信息,数据管理")
	@Security(accessType = "1", displayName = "中文标签列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech+","+BosConstants.role_sysAdmin)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {


			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}	
			

			LabelDisplay search=json.getSearch(LabelDisplay.class,"",ut,this.service);
			
			PageInfo page = json.getPageInfo(LabelDisplay.class);
			QueryFetchInfo<LabelDisplay> fetch = this.service.getMgClient().getFetch(search,"!createTime", page.getCurrentPage(),page.getPageSize(),this.service);
			
			super.fetchToJson(fetch, json, BosConstants.getTable(LabelDisplay.class));


			json.setSuccess();
			
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech+","+BosConstants.role_sysAdmin)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}	
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/delete", LabelDisplay.class.getName(), null, true,this.getService());
			
			this.service.getMgClient().deleteByPK(id, LabelDisplay.class,ut,this.service);
			
			
			LableLanDisplay lan=new LableLanDisplay();
			lan.setClassName(LabelDisplay.class.getName());
			lan.setId(id);
			this.service.getMgClient().delete(lan,ut,this.service);
			
	
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
