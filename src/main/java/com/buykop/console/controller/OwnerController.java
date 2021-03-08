package com.buykop.console.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.entity.PMember;
import com.buykop.console.entity.PPlaceInfo;
import com.buykop.console.entity.PUser;
import com.buykop.console.service.OwnerService;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.SMRecord;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MD5Encrypt;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;


@Module(display = "我的事务", sys = Constants.current_sys)
@RestController
public class OwnerController extends BaseController {
	
	
	private static Logger logger = LoggerFactory.getLogger(OwnerController.class);

	protected static final String URI = "/owner";

	@Autowired
	private OwnerService service;
	
	
	@Menu(name = "个人信息", trunk = "基础信息,我的事务", js = "profile")
	@Security(accessType = "0", displayName = "我的信息", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/profile",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject profile(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			PUser obj=this.service.getById(ut.getUserId(), PUser.class);
			
		
			super.objToJson(obj,json);
			
		
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		
		return json.jsonValue();
		
	}
	
	
	
	@Security(accessType = "0", displayName = "保存个人信息", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/profileSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject profileSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			PUser obj=json.getObj(PUser.class,null,this.service);
			obj.setUserId(ut.getUserId());
			

			
			PUser u=new PUser();
			u.setLoginName(obj.getLoginName());
			u.setUserId(ut.getUserId());
			u.addPropertyOperation("userId", 8);//<>
			u=this.service.get(u,null);
			if(u!=null) {
				json.setUnSuccess(-1, "登录名重复");
				return json.jsonValue();
			}
			
			this.service.save(obj,ut);
			
			
			obj=this.service.getById(ut.getUserId(), PUser.class);
			
			super.objToJson(obj,json);
			
			json.setSuccess("保存成功");
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
		
	}
	
	
    //@ParamIn(data = { @ParamData(cl = String.class, display = "原密码", help = "", key = "oldPwd"),@ParamData(cl = String.class, display = "新密码", help = "", key = "newPwd") }, obj = {  })
	@Security(accessType = "0", displayName = "修改密码", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/passwordChange",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject passwordChange(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
    	
		
		try {
			
			
			
			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String oldPwd=json.getSimpleData("oldPwd", "原密码", String.class, true,this.service);
			String newPwd=json.getSimpleData("newPwd","新密码", String.class, true,this.service);
			if(DataChange.isEmpty(oldPwd)) {
				json.setUnSuccess(-1, "原密码不能为空");
				return json.jsonValue();
			}
			if(DataChange.isEmpty(newPwd)) {
				json.setUnSuccess(-1, "新密码不能为空");
				return json.jsonValue();
			}
			
			
			Long minLen=PSysParam.paramLongValue("1", BosConstants.paramForceUserPwdLength);
			if(minLen==null) {
				minLen=6L;
			}
			
			if(newPwd.length()<minLen.intValue()) {
				json.setUnSuccess(-1, LabelDisplay.get("新密码长度不能低于", json.getLan()) +minLen,true);
				return json.jsonValue();
			}

			PUser user=this.service.getById(ut.getUserId(), PUser.class);
			if(user==null) {
				json.setUnSuccessForNoRecord(PUser.class);
				return json.jsonValue();
			}
			
			if(DataChange.isEmpty(user.getLoginPwd())) {
				json.setUnSuccess(-1, "您没有设置原密码，请联系管理员");
				return json.jsonValue();
			}
			
			if(!user.getLoginPwd().equals(MD5Encrypt.MD5Encode(oldPwd))){
				json.setUnSuccess(-1, "原密码不正确");
				return json.jsonValue();
			}
			
			
			user.setLoginPwd(newPwd);
			user.setPwdChangeTime(NetWorkTime.getCurrentDatetime());
			this.service.save(user,ut);
			
			json.setSuccess("更改密码成功");
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
		
	}


	
	
	
	//@Menu(name = "修改密码", trunk = "基础信息,我的事务", js = "password")
	@Security(accessType = "0", displayName = "显示修改密码", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/passwordInfo",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject passwordInfo(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			PUser user=this.service.getById(ut.getUserId(), PUser.class);
			
			
			super.objToJson(user,json);
			
			
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
		
	}
	
	
	@Security(accessType = "0", displayName = "更换自己手机(通过原密码)", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/mobileChangeByPwd",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject mobileChangeByPwd(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		

		
		try {
			
			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			SMRecord obj=json.getObj(SMRecord.class,"recordId,param,checkCode",this.service);
			if(obj==null) {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}
			
			
			if(DataChange.isEmpty(obj.getRecordId())) {
				json.setUnSuccess(-1,"未设置发送验证信息");
				return json.jsonValue();
			}
			
			
			SMRecord src=this.service.getMgClient().getById(obj.getRecordId(), SMRecord.class);
			if(src==null) {
				json.setUnSuccess(-1,"没有发送验证信息");
				return json.jsonValue();
			}
			
			
			
			
			PUser user=this.service.getById(ut.getUserId(), PUser.class);//
			if(DataChange.isEmpty(user.getLoginPwd())) {
				json.setUnSuccess(-1, "原登录密码为空，不能通过密码修改手机号");
				return json.jsonValue();
			}
			
		
			//直接核对密码进行修改
			if(DataChange.isEmpty(src.getMobile())) {
				json.setUnSuccess(-1,"新手机号码未设置");
				return json.jsonValue();
			}
			
			if(DataChange.isEmpty(obj.getParam())) {
				json.setUnSuccess(-1,"密码验证不能为空");
				return json.jsonValue();
			}
			
			if(DataChange.isEmpty(obj.getCheckCode())) {
				json.setUnSuccess(-1,"验证码不能为空");
				return json.jsonValue();
			}
			
			
			if(!user.getLoginPwd().equals(MD5Encrypt.MD5Encode(obj.getParam()))) {
				json.setUnSuccess(-1,"密码校验不正确");
				return json.jsonValue();
			}
			
			if(!obj.getCheckCode().equals(src.getCheckCode())) {
				json.setUnSuccess(-1,"验证有误");
				return json.jsonValue();
			}else {
				
			}
			
			
			
			user=new PUser();
			user.setUserId(ut.getUserId());
			user.setMobile(src.getMobile());
			user.setCurrentUserId(ut.getUserId());
			this.service.save(user,ut);
			
			json.setSuccess("更换密码成功");
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		
		return json.jsonValue();
		
	}
    
    
    
    
    
	@Menu(name = "机构信息", trunk = "基础信息,我的事务", js = "memberProfile")
	@Security(accessType = "2*,1*", displayName = "机构信息", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = URI+"/memberProfile",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject memberProfile(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		

		
		try {
			
			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			PMember obj=this.service.getById(ut.getMemberId(), PMember.class);
			
			PPlaceInfo place=new PPlaceInfo();
			place.setLevelType(0L);
			QueryListInfo<PPlaceInfo> pList=this.service.getList(place, "seq,placeName");
			super.selectToJson(pList.getSelectBidding(this.service), json, PMember.class.getSimpleName(), "countryId");
			

			if(obj!=null && !DataChange.isEmpty(obj.getCountryId())) {
				place=new PPlaceInfo();
				place.setParentId(obj.getCountryId());
				pList=this.service.getList(place,  "seq,placeName");
				super.selectToJson(pList.getSelectBidding(this.service), json, PMember.class.getSimpleName(), "provinceId");
			}
			
			
			/**if(obj!=null && !DataChange.isEmpty(obj.getProvinceId())) {
				place=new PPlaceInfo();
				place.setParentId(obj.getProvinceId());
				pList=this.service._getBaseDao().getList(place, "order by place_name asc");
				super.selectToJson(pList.getSelectBidding(conn), json, PMember.class.getSimpleName(), "cityId");
			}
			
		
			if(obj!=null && !DataChange.isEmpty(obj.getCityId())) {
				place=new PPlaceInfo();
				place.setParentId(obj.getCityId());
				pList=this.service._getBaseDao().getList(place, "order by place_name asc");
				super.selectToJson(pList.getSelectBidding(conn), json, PMember.class.getSimpleName(), "countyId");
			}*/
			
			super.objToJson(obj, json);
			
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		
		return json.jsonValue();
		
	}
	
	
	
	@Security(accessType = "2*,1*", displayName = "保存个人信息", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = URI+"/memberProfileSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject memberProfileSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		try {
			
			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			PMember obj=json.getObj(PMember.class,null,this.service);
			
			
			
			
			obj.setMemberId(ut.getMemberId());
			obj.setCurrentUserId(ut.getUserId());
			
			this.service.save(obj,ut);
			

			json.setSuccess("保存成功");
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
