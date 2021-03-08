package com.buykop.console.controller;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.entity.LoginLog;
import com.buykop.console.entity.PMember;
import com.buykop.console.entity.PUser;
import com.buykop.console.service.UserService;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.entity.SynTableDataConfig;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.DBScript;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.DateUtil;
import com.buykop.framework.util.data.MD5Encrypt;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.Entity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;





@Module(display = "用户",sys = Constants.current_sys)
@RestController
@RequestMapping(UserController.URI)
public class UserController extends BaseController{
	
	
	private static Logger  logger=LoggerFactory.getLogger(UserController.class);
	
	protected static final String URI="/user";
	
	
	@Autowired
    private UserService service;
	
	
	
	@Menu(js = "admin", name = "用户管理", trunk = "基础信息,我的事务")
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			PUserMember search=json.getSearch(PUserMember.class, "", ut, service);
			search.setMemberId(ut.getMemberId());
			PageInfo page=json.getPageInfo(PUserMember.class);
			QueryFetchInfo<PUserMember> fetch = this.service.getFetch(search, null, page.getCurrentPage(),page.getPageSize());
			for(PUserMember x:fetch.getList()) {
				JSONObject jo=CacheTools.getEntityDisplayJson(x.getUserId(), PUser.class.getName(),null);
				if(jo!=null) {
					x.setUserName(jo.getString("userName"));
					x.setMobile(jo.getString("mobile"));
				}
			}
			super.fetchToJson(fetch, json,BosConstants.getTable(PUserMember.class));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			List<PUserMember> list=json.getList(PUserMember.class, null, service);
			for(PUserMember x:list) {
				x.setMemberId(ut.getMemberId());
				this.service.save(x, ut);
			}
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "infoForCurrent", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject infoForCurrent(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			String id=json.getSelectedId(Constants.current_sys, URI+"/infoForCurrent",PUser.class.getName(), null,true,this.getService());
			
			
			PUser obj = this.service.getById(id, PUser.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PUser.class,id);
				return json.jsonValue();
			}
			super.objToJson(obj, json);
			
			
			Vector<String> v=new Vector<String>();
			if(true) {
				PUserMember um=new PUserMember();
				um.setUserId(id);
				um.setMemberId(ut.getMemberId());
				um=this.service.get(um, null);
				if(um!=null) {
					v=Field.split(um.getRoles());
				}
			}
			
			//显示角色类型
			PRole role=new PRole();
			QueryListInfo<PRole> list=this.service.getMgClient().getList(role, "sys,typeId,roleName",this.service);
			for(PRole x:list.getList()) {
				if(v.contains(x.getRoleId())) {
					x.setSelected();
				}
			}
			
			
			super.listToJson(list, json, BosConstants.getTable(PRole.class));
			
			//查询所属公司
			PUserMember um=new PUserMember();
			um.setUserId(id);
			um.setStatus(1L);
			QueryListInfo<PUserMember> umList=this.service.getList(um, null);
			if(umList.size()>0) {
				super.listToJson(umList, json, BosConstants.getTable(PUserMember.class));
			}
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			String id=json.getSelectedId(Constants.current_sys, URI+"/delete", PUserMember.class.getName(), null,true,this.getService());
			
			
			PUserMember obj = this.service.getById(id, PUserMember.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PUser.class,id);
				return json.jsonValue();
			}
			
			//只是删除关联关系
			if(obj.getUserId().equals("1")) {
				json.setUnSuccess(-1, "不能删除管理员");
				return json.jsonValue();
			}else if(obj.getUserId().equals(ut.getUserId())) {
				json.setUnSuccess(-1, "不能删除自己");
				return json.jsonValue();
			}else {
				this.service.deleteById(id, PUserMember.class.getName(), ut);
			}
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/infoForCurrent", PUser.class.getName(), null,false,this.getService());
			if(DataChange.isEmpty(id)) {
				id=BosEntity.next();
			}
			
			//保存
			PUser obj = json.getObj(PUser.class, null,this.service);
			obj.setPk(id);
			this.service.save(obj, ut);
			
			
			
			//保存角色
			Vector<String> ids=json.showIds();
			
			PUserMember um=new PUserMember();
			um.setMemberId(ut.getMemberId());
			um.setUserId(id);
			um.setIsValid(1);
			um.setRoles(MyString.CombinationBy(ids, ","));
			this.service.save(um, ut);
			
			
			//是否需要同步
			
			
			
			
			//super.objToJson(obj, json, conn);
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	
	//----------------------------------------------------------------------------------------------------
	
	
	@Menu(js = "user", name = "用户查询", trunk = "基础信息,机构及用户")
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "/query", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject query(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}
			
			PUser search=json.getSearch(PUser.class, null, ut, service);
			PageInfo page=json.getPageInfo(PUser.class);
			QueryFetchInfo<PUser> list = this.service.getFetch(search, null, page.getCurrentPage(),page.getPageSize());
			super.fetchToJson(list, json,BosConstants.getTable(PUser.class));
			

			//BosEntity search = json.getSearch(BosConstants.userClassName,null,ut,this.service);
			//PageInfo page=json.getPageInfo(BosConstants.userClassName);
			//QueryFetchInfo<BosEntity> list = this.service.getFetch(search, null, page.getCurrentPage(),page.getPageSize());
			//super.fetchToJson(list, json,BosConstants.getTable(BosConstants.userClassName));
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "/fetchForMember", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetchForMember(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/fetchForMember", PMember.class.getName(), "",true, service);
			PUserMember search = json.getSearch(PUserMember.class,null,ut,this.service);
			search.setMemberId(id);
			PageInfo page=json.getPageInfo(PUserMember.class);
			QueryFetchInfo<PUserMember> list = this.service.getFetch(search, null, page.getCurrentPage(),page.getPageSize());
			super.fetchToJson(list, json,BosConstants.getTable(PUserMember.class.getName()));
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	@Security(accessType = "1", displayName = "显示新增", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "/showAdd", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject showAdd(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			BosEntity obj = new TableJson(BosConstants.userClassName);
			super.objToJson(obj, json);
			
			
			PRole role=new PRole();
			QueryListInfo<PRole> list=this.service.getMgClient().getList(role, "roleName",this.service);
			super.listToJson(list, json, BosConstants.getTable(PRole.class));
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	
	
	
	@Security(accessType = "1", displayName = "详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			String id=json.getSelectedId(Constants.current_sys, URI+"/info", BosConstants.userClassName, null,true,this.getService());
			
			
			PUser obj = this.service.getById(id, PUser.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PUser.class,id);
				return json.jsonValue();
			}
			super.objToJson(obj, json);
			
			
			//查询所属公司
			PUserMember um=new PUserMember();
			um.setUserId(id);
			QueryListInfo<PUserMember> umList=this.service.getList(um, null);
			if(umList.size()>0) {
				super.listToJson(umList, json, BosConstants.getTable(PUserMember.class));
			}
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	
	
	
	@Security(accessType = "0", displayName = "详情", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/profile",method = RequestMethod.POST,consumes = "application/json")
	@ResponseBody
    public JSONObject profile(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{  
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			PUser obj=this.service.getById(ut.getUserId(), PUser.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PUser.class,ut.getUserId());
				return json.jsonValue();
			}
			obj.setMemberId(ut.getMemberId());
			
			
			if(true) {
				PUserMember uo=new PUserMember();
				uo.setUserId(obj.getUserId());
				uo.setMemberId(obj.getMemberId());
				uo=this.service.get(uo,null);
				if(uo!=null) {
					obj.setJob(uo.getJob());
					obj.setOrgId(uo.getOrgId());
				}
			}
			
			super.objToJson(obj,json);
			
			
			
			//查询我所属单位
			if(true) {
				PUserMember uo=new PUserMember();
				uo.setUserId(ut.getUserId());
				QueryListInfo<PUserMember> upList=this.service.getList(uo,"!isCurrent,!switchDate");
				super.listToJson(upList, json, BosConstants.getTable(PUserMember.class));
			}
			
			
			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "0", displayName = "保存自己信息", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/saveProfile",method = RequestMethod.POST,consumes = "application/json")
	@ResponseBody
    public JSONObject saveProfile(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{  
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			
			PUser obj=json.getObj(PUser.class,null,this.service);
			if(obj==null) {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}
			obj.removeMust("loginPwd");
			
			obj.setUserId(ut.getUserId());
			obj.setMemberId(ut.getMemberId());
			
			PUserMember uo=new PUserMember();
			uo.setUserId(ut.getUserId());
			uo.setMemberId(ut.getMemberId());
			uo.setOrgId(obj.getOrgId());
			uo.setJob(obj.getJob());
			this.service.save(obj,ut);
			
			json.setSuccess("保存成功");
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "0", displayName = "重置密码初始化", isEntAdmin = false, isSysAdmin = false, needLogin = false)
	@RequestMapping(value = "/forgetPwdInit",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject forgetPwdInit(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{  
		
		try{


			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			super.objToJson(new PUser(),json);

			json.setSuccess();

		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "0", displayName = "重置密码", isEntAdmin = false, isSysAdmin = false, needLogin = false)
	@RequestMapping(value = "/forgetPwdSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject forgetPwdSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{  
		

		try{


			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			PUser obj=json.getObj(PUser.class,null,this.service);

			
			if(DataChange.isEmpty(obj.getLoginPwd())) {
				json.setUnSuccess(-1, "登录密码未设置");
				return json.jsonValue();
			}
			
			obj.setLoginPwd(MD5Encrypt.MD5Encode(obj.getLoginPwd()));
		
			obj.setUserId(ut.getUserId());
			this.service.save(obj,ut);
			
			
			json.setSuccess();

		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "解除登录锁定", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "/unlock", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject unlock(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id=json.getSelectedId(Constants.current_sys, URI+"/unlock", PUser.class.getName(), null,true,this.getService());
			
			this.service.getRdClient().remove(BosConstants.getUserLoginLockKey(id));
			
			this.service.getRdClient().loginSuccess(id);
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}	
	
	
	
	
	
	@Security(accessType = "1", displayName = "强制锁定用户", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_oisAdmin)
	@RequestMapping(value = "/lock",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject lock(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/lock", PUser.class.getName(), null,true,this.getService());
			
			
			if(id.equals("1")) {
				json.setUnSuccess(-1, "不能被登录锁定超级管理");
				return json.jsonValue();
			}
			
			if(id.equals(ut.getUserId())) {
				json.setUnSuccess(-1, "不能被登录锁定当前用户");
				return json.jsonValue();
			}
			
	
			this.service.getRdClient().put(BosConstants.getUserLoginLockKey(id), String.valueOf(true),PSysParam.paramIntValue("1", BosConstants.paramAutoUnlockingLogin, 10)*60);
			

			//查找该永不所有的token(1天内的)
			LoginLog ls=new LoginLog();
			ls.setUserId(id);
			ls.setQueryDateProperty(LoginLog.class.getName()+Entity.keySplit+"logTime");
			ls.setQueryDateMin(DateUtil.addDay(NetWorkTime.getCurrentDate(), -7));
			ls.setQueryDateMax(DateUtil.addDay(NetWorkTime.getCurrentDate(), 1));
			QueryListInfo<LoginLog> list=this.service.getEsClient().getList(ls, "!logTime",this.service);
			for(LoginLog x:list.getList()) {
				if(DataChange.isEmpty(x.getToken())) continue;
				if(x.getToken().startsWith("TK")) {
					this.service.getRdClient().deleteUserToken(x.getToken(), null);
				}else {
					this.service.getRdClient().getJedis().del(x.getToken());
				}
			}
			

			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	
	
	
	@Security(accessType = "1", displayName = "保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_sysAdmin)
	@RequestMapping(value = "/saveForMember", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveForMember(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);


			if (ut == null) {
				return json.jsonValue();
			}

			
			String id=json.getSelectedId(Constants.current_sys, URI+"/info", PUser.class.getName(), null,false,this.getService());
			if(DataChange.isEmpty(id)) {
				id=BosEntity.next();
			}
			
			//保存
			PUser obj = json.getObj(PUser.class, null,this.service);
			obj.setPk(id);
			this.service.save(obj, ut);
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	

	@Override
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}
	
	
	
}
