package com.buykop.console.entity;

import java.util.Date;

import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.ExColumn;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Index;
import com.buykop.framework.annotation.IndexList;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.per.MemberId;
import com.buykop.framework.annotation.per.OrgId;
import com.buykop.framework.annotation.per.PlaceId;
import com.buykop.framework.annotation.per.UserId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.scan.ServerConfig;
import com.buykop.framework.util.type.BaseChartEntity;
import com.buykop.console.util.Constants;



@Mongo(code = "M_LOGIN_LOG", display = "登录日志", sys = Constants.current_sys, defaultOrderByField = "", retainDays = 0,redisEntity=false,transaction=false)
@IndexList(value = { @Index(fields = "!logTime", name = "logTime", unique = false, prompt = "", seq = 0) })
public class LoginLog extends BaseChartEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6958705805359060711L;

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "", isNotNull = false, label = "")
	protected String id;//

	@UserId
	@Column(dbLen = DBLen.idLen, displayName = "用户", isNotNull = false, label = "")
	protected String userId;//

	@MemberId
	@Column(dbLen = DBLen.idLen, displayName = "组织机构", isNotNull = false, label = "")
	protected String memberId;//

	@OrgId
	@Column(dbLen = DBLen.idLen, displayName = "部门", isNotNull = false, label = "")
	protected String orgId;//

	@Column(dbLen = DBLen.ip, displayName = "ip地址", isNotNull = false, label = "")
	protected String ip;//

	@Column(dbLen = DBLen.idLen, displayName = "token", isNotNull = false, label = "")
	protected String token;//

	// type 0:WEB 1:WX 2:APP 3:DEVICE 4:SSO
	@CodeValue(codeType = "148")
	@Column(dbLen = DBLen.ip, displayName = "登录方式", isNotNull = false, label = "", defaultValue = "0")
	protected Long loginType;//

	@Column(dbLen = DBLen.dateLen, displayName = "登录时间", isNotNull = false, label = "")
	protected Date logTime;
	
	@FK(table = ServerConfig.class)
	@Column(dbLen = DBLen.idLen, displayName = "服务", label = "")
	private String serverId;

	@PlaceId
	@ExColumn(dbLen = DBLen.idLen, displayName = "地区", label = "")
	protected String placeId;

	@OrgId
	@ExColumn(dbLen = DBLen.idLen, displayName = "部门", label = "")
	protected String parentOrgId;
	
	
	

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public Long getLoginType() {
		return loginType;
	}

	public void setLoginType(Long loginType) {
		this.loginType = loginType;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	public Date getLogTime() {
		return logTime;
	}

	public void setLogTime(Date logTime) {
		this.logTime = logTime;
	}

	public String getPlaceId() {
		return placeId;
	}

	public void setPlaceId(String placeId) {
		this.placeId = placeId;
	}

	public String getParentOrgId() {
		return parentOrgId;
	}

	public void setParentOrgId(String parentOrgId) {
		this.parentOrgId = parentOrgId;
	}

	@Override
	public String getChartDateDisplay() {
		// TODO Auto-generated method stub
		return "登录日期";
	}

}