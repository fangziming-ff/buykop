package com.buykop.console.service.impl;

import java.util.Vector;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.entity.LoginLog;
import com.buykop.console.entity.PMember;
import com.buykop.console.entity.PUser;
import com.buykop.framework.annotation.util.DataCheck;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.oauth2.PMemberType;
import com.buykop.framework.oauth2.PRMemberType;
import com.buykop.framework.oauth2.PRRoleFun;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.DateUtil;
import com.buykop.framework.util.data.MD5Encrypt;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseService;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.TableJson;
import com.buykop.console.util.Constants;


@Service
@Component
public class TokenService extends BaseService implements com.buykop.console.service.TokenService{

	@Autowired
	private AmqpTemplate rabbitTemplate;

	public AmqpTemplate getRabbitTemplate() {
		return rabbitTemplate;
	}
	
	
	
	public UserToken login(String loginName,String password,int isMobile,String ip,String lan) throws Exception{
		
		//BosConstants.debug("-------------loginName="+loginName+"   loginPwd="+loginPwd+"   x="+x+"   y="+y);
		
		if(DataChange.isEmpty(loginName)) {
			throw new Exception("登录信息为空");
		}
		
		if(DataChange.isEmpty(password)) {
			throw new Exception("登录密码为空");
		}
		
		loginName=loginName.trim();
		password=password.trim();
		
		PUser user=new PUser();
		
		if(loginName.indexOf("@")!=-1) {//邮件登录  email
			user.setMail(loginName);
		}else if(DataCheck.checkNum(loginName)) {
			user.setMobile(loginName);
		}else {
			user.setLoginName(loginName);
		}
		user=this.get(user, null);
		if(user==null) {
			throw new Exception("登录信息不正确");
		}
		
		
		if(user.getIsValid()==null) {
			user.setIsValid(0);
		}
		
		if(user.getIsValid().longValue()!=1) {
			throw new Exception("用户已被删除");
		}
		
		
		Long status=DataChange.getLongValueWithDefault(user.getStatus(), 0);
		if(status.intValue()!=1) {
			throw new Exception("用户未启用");
		}
		
		String lock=this.getRdClient().get(BosConstants.getUserLoginLockKey(user.getPk()));
		if(!DataChange.isEmpty(lock)) {
			throw new Exception("您已被登录锁定"+PSysParam.paramLongValue("1", BosConstants.paramAutoUnlockingLogin, 10)+"分钟,请稍后再试");
		}
		
		
		int faildNum=this.getRdClient().getLoginFaild(user.getPk(),this);
		if(faildNum>0) {
			//如果六次登录失败,就锁定20分钟
			throw new Exception("多次登录失败,账号锁定"+PSysParam.paramLongValue("1", BosConstants.paramAutoUnlockingLogin, 10)+"分钟");
		}
		
		
		
		
		UserToken token=new UserToken();
		token.setTokenKey(UserToken.createToken(true));
		token.setUserId(user.getPk());
		token.setMemberAdmin(0L);
		token.setOrgAdmin(0L);
		token.setIsOis(0L);
		
		
		
		

		PUserMember um=null;
		if(user.getUserId().equals("1")) {
			um=new PUserMember();
			um.setUserId(user.getUserId());
			um.setMemberId("1");
			um.setIsCurrent(1L);
			um.setStatus(1L);
			um.setSwitchDate(NetWorkTime.getCurrentDatetime());
			this.save(um, null);
			
			token.setMemberAdmin(1L);
			
			token.setMemberId("1");
			
			
			String pwd=DataChange.replaceNull(user.getLoginPwd()).trim();
			if(DataChange.isEmpty(pwd) ){
				throw new Exception("用户未设置密码");
			}
			
			if(!pwd.equals(MD5Encrypt.MD5Encode(password) )) {
				this.getRdClient().loginFaild(user.getPk(), this);
				throw new Exception("登录密码不正确");
			}else {
				this.getRdClient().loginSuccess(user.getPk());
			}
			
			
			
		}else {
			
			
			um=new PUserMember();
			um.setUserId(user.getUserId());
			um.setIsCurrent(1L);
			um.setStatus(1L);
			um=this.getMgClient().get(um, "!switchDate",this);
			if(um==null){
				um=new PUserMember();
				um.setUserId(user.getUserId());
				um.setStatus(1L);
				um=this.getMgClient().get(um, "!switchDate",this);
			}
			
			
			
			
			if(um!=null) {//机构
				
				um.setIsCurrent(1L);
				um.setSwitchDate(NetWorkTime.getCurrentDatetime());
				this.save(um, null);
				
				token.setMemberId(um.getMemberId());
				
				
				PMember member=this.getById(token.getMemberId(), PMember.class);
				if(member==null) {
					//与member里面的密码进行对比
					throw new Exception("机构不存在");
				}
				
				
				if(member.getIsValid()==null) {
					user.setIsValid(0);
				}
				
				if(member.getIsValid().longValue()!=1) {
					throw new Exception("机构已被删除");
				}
				
				if(user.getUserId().equals(member.getAdminId())) {
					token.setMemberAdmin(1L);
				}
				
				Long standalone=PSysParam.paramLongValue("1", BosConstants.paramStandaloneLoginPwdForMember);
				if(standalone==null) {
					standalone=0L;
				}
				
				
				BosEntity org=this.getById(um.getOrgId(), BosConstants.orgClassName);
				if(org!=null) {
					String chargeUser=org.propertyValueString("chargeUser");
					if(user.getUserId().equals(chargeUser)) {
						token.setOrgAdmin(1L);
					}
				}
				
				if(standalone.intValue()==1) {
					
					String pwd=DataChange.replaceNull(um.getLoginPwd()).trim();
					if(DataChange.isEmpty(pwd) ){
						throw new Exception("用户未设置密码");
					}
					
					if(!pwd.equals(MD5Encrypt.MD5Encode(password) )) {
						this.getRdClient().loginFaild(user.getPk(), this);
						throw new Exception("登录密码不正确");
					}else {
						this.getRdClient().loginSuccess(user.getPk());
					}
					
					
				}else {
					
					String pwd=DataChange.replaceNull(user.getLoginPwd()).trim();
					if(DataChange.isEmpty(pwd) ){
						throw new Exception("用户未设置密码");
					}
					
					if(!pwd.equals(MD5Encrypt.MD5Encode(password) )) {
						this.getRdClient().loginFaild(user.getPk(), this);
						throw new Exception("登录密码不正确");
					}else {
						this.getRdClient().loginSuccess(user.getPk());
					}
					
				}
				
				
				//设置权限
				this.setUserPermission(token);
				
				
			}else {
				//个人
				
				String pwd=DataChange.replaceNull(user.getLoginPwd()).trim();
				if(DataChange.isEmpty(pwd) ){
					throw new Exception("用户未设置密码");
				}
				
				if(!pwd.equals(MD5Encrypt.MD5Encode(password) )) {
					this.getRdClient().loginFaild(user.getPk(), this);
					throw new Exception("登录密码不正确");
				}else {
					this.getRdClient().loginSuccess(user.getPk());
				}
				
				
			}
			
		}
		
		

		token.setIsMobile(DataChange.intToLong(isMobile));
		token.setLoginIp(ip);
		token.setOverTime(DateUtil.addSecond(NetWorkTime.getCurrentDatetime(), BosConstants.getExpirationTime(isMobile,token.getMemberId())));
		token.setLoginTime(NetWorkTime.getCurrentDatetime());
		token.setRefreshKey(UserToken.createToken(true));
		token.setUserName(user.getUserName());
		token.setMail(user.getMail());
		token.setMobile(user.getMobile());
		this.save(token, new UserToken());
		
		
		JSONObject json = new JSONObject(true);
		json.put("userId", token.getUserId());////用户id
		json.put("memberId", token.getMemberId());//商户id
		json.put("loginType", "0");// 0:PC 1:微信 2:App
		json.put("ip", ip);//ip地址
		json.put("token", token.getTokenKey());//ip地址
		this.rabbitTemplate.convertAndSend("loginLog", json);
		
		token.setUserInfo(user.getSimpleDBJson(false));
		
		
		
		
		

		return token;
	}
	
	
	
	
	
	
	
	public UserToken adminLogin(String loginName,String password,int isMobile,String ip,String lan) throws Exception{
		
		//BosConstants.debug("-------------loginName="+loginName+"   loginPwd="+loginPwd+"   x="+x+"   y="+y);
		
		if(DataChange.isEmpty(loginName)) {
			throw new Exception("登录信息为空");
		}
		
		if(DataChange.isEmpty(password)) {
			throw new Exception("登录密码为空");
		}
		
		loginName=loginName.trim();
		password=password.trim();
		
		PUser user=new PUser();
		
		if(loginName.indexOf("@")!=-1) {//邮件登录  email
			user.setMail(loginName);
		}else if(DataCheck.checkNum(loginName)) {
			user.setMobile(loginName);
		}else {
			user.setLoginName(loginName);
		}
		user=this.get(user, null);
		if(user==null) {
			throw new Exception("登录信息不正确");
		}
		
		
		
		if(user.getIsValid()==null) {
			user.setIsValid(0);
		}
		
		if(user.getIsValid().longValue()!=1) {
			throw new Exception("用户已被删除");
		}
		
		
		Long status=DataChange.getLongValueWithDefault(user.getStatus(), 0);
		if(status.intValue()!=1) {
			throw new Exception("用户未启用");
		}
		
		
		String lock=this.getRdClient().get(BosConstants.getUserLoginLockKey(user.getPk()));
		if(!DataChange.isEmpty(lock)) {
			throw new Exception("您已被登录锁定"+PSysParam.paramLongValue("1", BosConstants.paramAutoUnlockingLogin, 10)+"分钟,请稍后再试");
		}
		
		int faildNum=this.getRdClient().getLoginFaild(user.getPk(),this);
		if(faildNum>0) {
			//如果六次登录失败,就锁定20分钟
			throw new Exception("多次登录失败,账号锁定"+PSysParam.paramLongValue("1", BosConstants.paramAutoUnlockingLogin, 10)+"分钟");
		}
		
		
		
		PUserMember um=new PUserMember();
		um.setUserId(user.getUserId());
		um.setMemberId("1");
		um=this.getMgClient().getById(um.getPk(), PUserMember.class);
		if(um==null) {
			throw new Exception("该用户未在机构注册");
		}
		
		
		if(DataChange.getIntValueWithDefault(um.getStatus(), 0)==0) {
			throw new Exception("该用户在机构注册未启用");
		}
		
		String pwd=DataChange.replaceNull(user.getLoginPwd()).trim();
		if(DataChange.isEmpty(pwd) ){
			throw new Exception("用户未设置密码");
		}
		
		
		BosConstants.debug("src passwd="+user.getLoginPwd());
		
		
		BosConstants.debug("log passwd="+password.trim()+"   md5="+MD5Encrypt.MD5Encode(password.trim()));
		
		if(!pwd.equals(MD5Encrypt.MD5Encode(password.trim()) )) {
			this.getRdClient().loginFaild(user.getPk(), this);
			throw new Exception("登录密码不正确");
		}else {
			this.getRdClient().loginSuccess(user.getPk());
		}
		
		

		UserToken token=new UserToken();
		token.setTokenKey(UserToken.createToken(true));
		token.setUserId(user.getPk());
		token.setMemberId("1");
		token.setOrgAdmin(0L);
		token.setIsOis(0L);
		token.setMemberAdmin(0L);
		//token.setMemberId(user.getMemberId());
		//token.setOrgId(user.getOrgId());
		//token.setCityId((String)info.get("cityId"));
		token.setIsMobile(DataChange.intToLong(isMobile));
		token.setLoginIp(ip);
		token.setOverTime(DateUtil.addSecond(NetWorkTime.getCurrentDatetime(), BosConstants.getExpirationTime(isMobile,token.getMemberId())));
		token.setLoginTime(NetWorkTime.getCurrentDatetime());
		token.setRefreshKey(UserToken.createToken(true));
		token.setUserName(user.getUserName());
		token.setMail(user.getMail());
		token.setMobile(user.getMobile());
		if(user.getUserId().equals("1")) {
			token.setOrgAdmin(1L);
			token.setMemberAdmin(1L);
		}
		this.save(token, new UserToken());
		
		
		JSONObject json = new JSONObject(true);
		json.put("userId", token.getUserId());////用户id
		json.put("memberId", token.getMemberId());//商户id
		json.put("loginType", "0");// 0:PC 1:微信 2:App
		json.put("ip", ip);//ip地址
		json.put("token", token.getTokenKey());//ip地址
		this.rabbitTemplate.convertAndSend("loginLog", json);
		
		
		
		
		token.setUserInfo(user.getSimpleDBJson(false));
		
		//设置权限
		this.setUserPermission(token);
		
		
		System.out.println("USER:"+token.getUserId()+"  "+user.getLoginName()+"  getMemberAdmin="+token.getMemberAdmin()+" per="+token.getPers()+"   roles="+token.getRoles());
		
		

		return token;
	}
	
	
	
	public UserToken memberLogin(String memberId,String loginName,String password,int isMobile,String ip,String lan) throws Exception{
		
		//BosConstants.debug("-------------loginName="+loginName+"   loginPwd="+loginPwd+"   x="+x+"   y="+y);
		
		if(DataChange.isEmpty(loginName)) {
			throw new Exception("登录信息为空");
		}
		
		if(DataChange.isEmpty(password)) {
			throw new Exception("登录密码为空");
		}
		
		loginName=loginName.trim();
		password=password.trim();
		
		PUser user=new PUser();
		
		if(loginName.indexOf("@")!=-1) {//邮件登录  email
			user.setMail(loginName);
		}else if(DataCheck.checkNum(loginName)) {
			user.setMobile(loginName);
		}else {
			user.setLoginName(loginName);
		}
		user=this.get(user, null);
		if(user==null) {
			throw new Exception("登录信息不正确");
		}
		
		
		if(user.getIsValid()==null) {
			user.setIsValid(0);
		}
		
		if(user.getIsValid().longValue()!=1) {
			throw new Exception("用户已被删除");
		}
		
		
		Long status=DataChange.getLongValueWithDefault(user.getStatus(), 0);
		if(status.intValue()!=1) {
			throw new Exception("用户未启用");
		}
		
		
		String lock=this.getRdClient().get(BosConstants.getUserLoginLockKey(user.getPk()));
		if(!DataChange.isEmpty(lock)) {
			throw new Exception("您已被登录锁定"+PSysParam.paramLongValue("1", BosConstants.paramAutoUnlockingLogin, 10)+"分钟,请稍后再试");
		}
		
		
	
		
		int faildNum=this.getRdClient().getLoginFaild(user.getPk(),this);
		if(faildNum>0) {
			//如果六次登录失败,就锁定20分钟
			throw new Exception("多次登录失败,账号锁定"+PSysParam.paramLongValue("1", BosConstants.paramAutoUnlockingLogin, 10)+"分钟");
		}
		
		
		
		PMember member=this.getById(memberId, PMember.class);
		if(member==null) {
			//与member里面的密码进行对比
			throw new Exception("机构不存在");
		}
		
		
		if(member.getIsValid()==null) {
			user.setIsValid(0);
		}
		
		if(member.getIsValid().longValue()!=1) {
			throw new Exception("机构已被删除");
		}
		
		
		PUserMember um=new PUserMember();
		um.setUserId(user.getUserId());
		um.setMemberId(memberId);
		um=this.getMgClient().getById(um.getPk(), PUserMember.class);
		if(um==null) {
			throw new Exception("该用户未在机构注册");
		}
		if(DataChange.getIntValueWithDefault(um.getStatus(), 0)==0) {
			throw new Exception("该用户在机构注册未启用");
		}
		um.setIsCurrent(1L);
		um.setSwitchDate(NetWorkTime.getCurrentDatetime());
		this.save(um, null);
		
		
		
		
		Long standalone=PSysParam.paramLongValue("1", BosConstants.paramStandaloneLoginPwdForMember);
		if(standalone==null) {
			standalone=0L;
		}
		
		if(standalone.intValue()==0) {
			
			String pwd=DataChange.replaceNull(user.getLoginPwd()).trim();
			if(DataChange.isEmpty(pwd) ){
				throw new Exception("用户未设置密码");
			}
			
			if(!pwd.equals(MD5Encrypt.MD5Encode(password) )) {
				this.getRdClient().loginFaild(user.getPk(), this);
				throw new Exception("登录密码不正确");
			}else {
				this.getRdClient().loginSuccess(user.getPk());
			}
			
		}else {
			
			String pwd=DataChange.replaceNull(um.getLoginPwd()).trim();
			if(DataChange.isEmpty(pwd) ){
				throw new Exception("用户未设置密码");
			}
			
			if(!pwd.equals(MD5Encrypt.MD5Encode(password) )) {
				this.getRdClient().loginFaild(user.getPk(), this);
				throw new Exception("登录密码不正确");
			}else {
				this.getRdClient().loginSuccess(user.getPk());
			}
		}

			
		UserToken token=new UserToken();
		token.setTokenKey(UserToken.createToken(true));
		token.setUserId(user.getPk());
		token.setMemberAdmin(0L);
		token.setOrgAdmin(0L);
		token.setIsOis(0L);
		token.setAdminId(member.getAdminId());
		if(user.getUserId().equals(member.getAdminId())) {
			token.setMemberAdmin(1L);
		}
		
		BosEntity org=this.getById(um.getOrgId(), BosConstants.orgClassName);
		if(org!=null) {
			String chargeUser=org.propertyValueString("chargeUser");
			if(user.getUserId().equals(chargeUser)) {
				token.setOrgAdmin(1L);
			}
		}
		
		
		//token.setMemberId(user.getMemberId());
		//token.setOrgId(user.getOrgId());
		//token.setCityId((String)info.get("cityId"));
		token.setIsMobile(DataChange.intToLong(isMobile));
		token.setLoginIp(ip);
		token.setOverTime(DateUtil.addSecond(NetWorkTime.getCurrentDatetime(), BosConstants.getExpirationTime(isMobile,token.getMemberId())));
		token.setLoginTime(NetWorkTime.getCurrentDatetime());
		token.setRefreshKey(UserToken.createToken(true));
		token.setUserName(user.getUserName());
		token.setMail(user.getMail());
		token.setMobile(user.getMobile());
		this.save(token, token);
		
		
		JSONObject json = new JSONObject(true);
		json.put("userId", token.getUserId());////用户id
		json.put("memberId", token.getMemberId());//商户id
		json.put("loginType", "0");// 0:PC 1:微信 2:App
		json.put("ip", ip);//ip地址
		json.put("token", token.getTokenKey());//ip地址
		this.rabbitTemplate.convertAndSend("loginLog", json);
		
		token.setUserInfo(user.getSimpleDBJson(false));
		
		//设置权限
		this.setUserPermission(token);
		
		
		
		

		return token;
	}
	
	
	
	
	
	
	
}
