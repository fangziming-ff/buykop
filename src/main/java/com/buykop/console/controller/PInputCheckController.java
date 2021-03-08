package com.buykop.console.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.entity.product.Product;
import com.buykop.console.service.PInputCheckService;
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
import com.buykop.framework.scan.InputCheck;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;

@Module(display = "检查", sys = Constants.current_sys)
@RestController
@RequestMapping("/inputcheck")
public class PInputCheckController extends BaseController{
	
	
	private static Logger  logger=LoggerFactory.getLogger(PInputCheckController.class);
	
	@Autowired
	private PInputCheckService service;
	
	@Menu(js = "inputCheck", name = "格式检查", trunk = "基础信息,数据管理")
	@Security(accessType = "1", displayName = "检查列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}	
			

			InputCheck search=json.getSearch(InputCheck.class,"",ut,this.service);
			
			PageInfo page = json.getPageInfo(InputCheck.class);
			QueryFetchInfo<InputCheck> fetch = this.service.getMgClient().getFetch(search, page.getCurrentPage(),page.getPageSize(),this.service);
			
			super.fetchToJson(fetch, json, BosConstants.getTable(InputCheck.class));


			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}


	@Security(accessType = "1", displayName = "检查列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/saveInputCheckList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveInputCheckList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			List<InputCheck> list=json.getList(InputCheck.class,"checkId,name,check",this.service);
			for (InputCheck p : list) {
				BosConstants.debug(p.getPk());
			}
			BosConstants.debug("----------------------------------");
			for(InputCheck s:list) {
				this.service.save(s,ut);
				BosConstants.getExpireHash().remove(InputCheck.class, s.getCheckId());
				new ClassInnerNotice().invoke(InputCheck.class.getSimpleName(),s.getCheckId());
			}
			
			json.setSuccess();
			
			
			return this.fetch(json,request);
		} catch (Exception e) {
			json.setUnSuccess(e);
			
		}

		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "数据检查删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = "/inputCheckDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject inputCheckDelete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			

			String id=json.getSelectedId(Constants.current_sys, "/inputcheck/inputCheckDelete",InputCheck.class.getName(),null,true,this.getService());

			BosConstants.debug("------------"+id);
			
			InputCheck obj=this.service.getMgClient().getById(id, InputCheck.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(InputCheck.class);
				return json.jsonValue();
			}
			Field field=new Field();
			field.setCheck(obj.getCheckId());
			if(this.service.getMgClient().getCount(field,this.service)>0) {
				json.setUnSuccess(-1, "该数据字典被使用,不能删除");
				return json.jsonValue();
			}
			
			this.service.getMgClient().deleteByPK(id, InputCheck.class,ut,this.service);
			
			
			BosConstants.getExpireHash().remove(InputCheck.class, id);
			new ClassInnerNotice().invoke(InputCheck.class.getSimpleName(),id);
			
			json.setSuccess("删除成功");
			
			return this.fetch(json,request);
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
