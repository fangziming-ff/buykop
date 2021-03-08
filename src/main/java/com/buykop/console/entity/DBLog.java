package com.buykop.console.entity;

import java.util.Date;

import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Index;
import com.buykop.framework.annotation.IndexList;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.per.BizId;
import com.buykop.framework.annotation.per.MemberId;
import com.buykop.framework.annotation.per.OrgId;
import com.buykop.framework.annotation.per.PlaceId;
import com.buykop.framework.annotation.per.UserId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.entity.IpPools;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.ServerConfig;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.Entity;
import com.buykop.console.util.Constants;


@Mongo(sys = Constants.current_sys, display = "数据日志", code = "M_DB_LOG", defaultOrderByField = "", retainDays = 0,redisEntity=false,transaction=false)
@IndexList(value = { @Index(fields = "!invokeTime", name = "invokeTime", unique = false, prompt = "", seq = 0) })
public class DBLog extends Entity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -778580774706902071L;

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "", isNotNull = false, label = "")
	protected String id;//

	@CodeValue(codeType = "904")
	@Column(dbLen = DBLen.codeValueLen, displayName = "操作类型", label = "")
	private Long dbAction;// 1:增 2:改 3:删除  4:软删除  5:执行

	@FK(table = PRoot.class)
	@Column(dbLen = DBLen.len32, displayName = "所属系统", label = "")
	private String sys;

	@FK(table = Table.class)
	@Column(dbLen = DBLen.len256, displayName = "绑定业务类", label = "",fieldType =2)
	private String className;

	
	@BizId
	@Column(dbLen = DBLen.len64, displayName = "主键", isNotNull = false, label = "")
	protected String idValue;

	@Column(dbLen = DBLen.len512, displayName = "信息", isNotNull = false, label = "", fieldType =1)
	protected String info;//
	

	@Column(dbLen = DBLen.dateLen, displayName = "执行时间", label = "")
	private Date invokeTime;

	@UserId
	@Column(dbLen = DBLen.idLen, displayName = "执行人", label = "")
	private String invokeUserId;

	
	@Column(dbLen = DBLen.idLen, displayName = "执行人", label = "",fieldType =2)
	private String invokeUser;

	@MemberId
	@Column(dbLen = DBLen.idLen, displayName = "执行人所属机构", label = "")
	private String invokeMemberId;

	@Column(dbLen = DBLen.idLen, displayName = "执行人所属机构", label = "",fieldType =2)
	private String invokeMember;

	@PlaceId
	@Column(dbLen = DBLen.idLen, displayName = "执行人所属地区", label = "")
	private String placeId;

	@OrgId
	@Column(dbLen = DBLen.idLen, displayName = "执行人所属部门", label = "")
	private String orgId;

	@FK(table=IpPools.class)
	@Column(dbLen = DBLen.ip, displayName = "ip地址", isNotNull = false, label = "",fieldType =2)
	protected String ip;//

	@FK(table = ServerConfig.class)
	@Column(dbLen = DBLen.idLen, displayName = "服务", label = "")
	private String serverId;
	
	
	

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public String getSys() {
		return sys;
	}

	public void setSys(String sys) {
		this.sys = sys;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	

	public String getIdValue() {
		return idValue;
	}

	public void setIdValue(String idValue) {
		this.idValue = idValue;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Long getDbAction() {
		return dbAction;
	}

	public void setDbAction(Long dbAction) {
		this.dbAction = dbAction;
	}

	public String getInfo() {
		return info;
	}

	public void setInfo(String info) {
		this.info = info;
	}

	public String getInvokeUser() {
		return invokeUser;
	}

	public void setInvokeUser(String invokeUser) {
		this.invokeUser = invokeUser;
	}

	public String getInvokeMember() {
		return invokeMember;
	}

	public void setInvokeMember(String invokeMember) {
		this.invokeMember = invokeMember;
	}

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	public String getPlaceId() {
		return placeId;
	}

	public void setPlaceId(String placeId) {
		this.placeId = placeId;
	}

	public String getInvokeMemberId() {
		return invokeMemberId;
	}

	public void setInvokeMemberId(String invokeMemberId) {
		this.invokeMemberId = invokeMemberId;
	}

	public Date getInvokeTime() {
		return invokeTime;
	}

	public void setInvokeTime(Date invokeTime) {
		this.invokeTime = invokeTime;
	}

	public String getInvokeUserId() {
		return invokeUserId;
	}

	public void setInvokeUserId(String invokeUserId) {
		this.invokeUserId = invokeUserId;
	}

	

}
