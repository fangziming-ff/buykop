package com.buykop.console.controller;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.RoleService;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;


@Module(display = "角色", sys = Constants.current_sys)
@RestController
public class RoleController extends BaseController{
	
	
	protected static final String URI="/role";
	
	private static Logger  logger=LoggerFactory.getLogger(RoleController.class);
	
	@Autowired
	private RoleService service;
	
	
	
	

	@RequestMapping(value = URI+"/listForMember",method = RequestMethod.GET)
	@ResponseBody
    public JSONObject listForMember(HttpServletRequest request,String memberId,@RequestHeader String token) throws Exception{  
		
		
		HttpEntity json=new HttpEntity();
		json.setSys(Constants.current_sys);
		json.setUri(URI + "/listForMember");

		if (DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		try {
			
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			if(DataChange.isEmpty(ut.getUserId())) {
				json.setUnSuccess(20, "请登录后后操作");
				return json.jsonValue();
			}
			
			if(DataChange.isEmpty(ut.getMemberId())) {
				json.setUnSuccess(21, "当前身份有误");
				return json.jsonValue();
			}
			
			QueryListInfo<PRole> list=PRole.getListForMember(memberId,null,this.service);
			super.listToJson(list, json, BosConstants.getTable(PRole.class));
			

			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1,2", displayName = "角色列表(机构)", needLogin = true, isEntAdmin = false, isSysAdmin =false)
	@RequestMapping(value = URI+"/listForMember",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject listForMember(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{  
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String memberId=json.getSelectedId(Constants.current_sys,URI+"/listForMember",BosConstants.memberClassName,null,true,this.getService());
			
			
			QueryListInfo<PRole> list=PRole.getListForMember(memberId,null,this.service);
			super.listToJson(list, json, BosConstants.getTable(PRole.class));
			

			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	@Security(accessType = "1,2", displayName = "角色列表(当前机构)", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin+","+BosConstants.role_memberAdmin)
	@RequestMapping(value = URI+"/listForMyMember",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject listForMyMember(@RequestBody HttpEntity json,HttpServletRequest request, @RequestHeader String token) throws Exception{  
		
		
		
		json.setSys(Constants.current_sys);
		json.setUri(URI + "/listForMyMember");

		if (DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			QueryListInfo<PRole> list=PRole.getListForMember(ut.getMemberId(),null,this.service);
			super.listToJson(list, json, BosConstants.getTable(PRole.class));
			
			
			json.setSuccess();
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	
	@Security(accessType = "2,1", displayName = "角色列表(用户)", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_sysAdmin+","+BosConstants.role_memberAdmin)
	@RequestMapping(value = URI+"/listForUser",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject listForUser(@RequestBody HttpEntity json,HttpServletRequest request, @RequestHeader String token) throws Exception{  
		
		
		
		json.setSys(Constants.current_sys);
		json.setUri(URI + "/listForUser");

		if (DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			
			String userId=json.getSelectedId(Constants.current_sys,URI+"/listForUser",BosConstants.userClassName,null,true,this.getService());

			Vector<String> fv=new Vector<String>();
			
			PUserMember um=new PUserMember();
			um.setMemberId(ut.getMemberId());
			um.setUserId(userId);
			um.setStatus(1L);
			um=this.service.get(um, null);
			if(um!=null) {
				fv=Field.split(um.getRoles());
			}
			
			
			QueryListInfo<PRole> list=PRole.getListForMember(ut.getMemberId(),null,this.service);
			
			BosConstants.debug("当前机构角色数量:"+list.size());
			
			
			for(PRole su:list.getList()) {
				if(fv.contains(su.getPk())) {
					su.setSelected("checked");
				}
			}

			super.listToJson(list, json, BosConstants.getTable(PRole.class));
			

			json.setSuccess();
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "2,1", displayName = "角色列表(用户)", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_sysAdmin+","+BosConstants.role_memberAdmin)
	@RequestMapping(value = URI+"/listForUserSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject listForUserSave(@RequestBody HttpEntity json,HttpServletRequest request, @RequestHeader String token) throws Exception{  
		
		
		
		json.setSys(Constants.current_sys);
		json.setUri(URI + "/listForUserSave");

		if (DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String userId=json.getSelectedId(Constants.current_sys,URI+"/listForUser",BosConstants.userClassName,null,true,this.getService());

			
			//保存信息 ,分隔
			Vector<String> ids=json.showIds();
				
			PUserMember um=new PUserMember();
			um.setMemberId(ut.getMemberId());
			um.setUserId(userId);
			um.setStatus(1L);
			um.setRoles(MyString.CombinationBy(ids, ","));
			this.service.save(um, ut);
				
			
			
			json.setSuccess();
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	
	
	
	@Security(accessType = "1", displayName = "角色列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech+","+BosConstants.role_sysAdmin)
	@RequestMapping(value = URI+"/list",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject list(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{  
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			PRole search=json.getSearch(PRole.class,null,ut,this.service);
			QueryListInfo<PRole> fetch= this.service.getMgClient().getList(search, "sys,typeId,roleName",this.service);
			
			super.listToJson(fetch, json, BosConstants.getTable(PRole.class));
			

			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "保存角色列表", needLogin = true, isEntAdmin = false, isSysAdmin =false,roleId=BosConstants.role_tech+","+BosConstants.role_sysAdmin)
	@RequestMapping(value = URI+"/saveList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{  
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			List<PRole> list=json.getList(PRole.class, "roleId,roleName,roleType,status,!sys",this.service);
			for(PRole x:list) {
				this.service.save(x,ut);
			}
			
			
			
			json.setSuccess("保存成功");
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
		
	
	
	
	
	
	
	@Security(accessType = "1", displayName = "显示新增", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech+","+BosConstants.role_sysAdmin)
	@RequestMapping(value = URI+"/save",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject save(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{  
		

		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			PRole obj=json.getObj(PRole.class,null,this.service);
			if(obj!=null) {
				if(DataChange.isEmpty(obj.getRoleId())) {
					obj.setRoleId(PRole.next());
				}
				this.service.save(obj,ut);
			}
			
			json.setSuccess("保存成功");
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "角色详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech+","+BosConstants.role_sysAdmin)
	@RequestMapping(value = URI+"/info",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{  
		
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			PRole role=null;
			String id=json.getSelectedId(Constants.current_sys, URI+"/info",PRole.class.getName(),null,this.getService());
			if(!DataChange.isEmpty(id)) {
				role=this.service.getById(id, PRole.class);
			}
			
			if(role==null) {
				json.setUnSuccessForNoRecord(PRole.class);
				return json.jsonValue();
			}
			
			
			super.objToJson(role, json);

			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	

	@Security(accessType = "1", displayName = "删除角色", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech+","+BosConstants.role_sysAdmin)
	@RequestMapping(value = URI+"/delete",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{  
		
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/delete",PRole.class.getName(),null,true,this.getService());

			this.service.getMgClient().deleteByPK(id, PRole.class,ut,this.service);
			

			json.setSuccess("删除成功");
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}

	
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}
}
