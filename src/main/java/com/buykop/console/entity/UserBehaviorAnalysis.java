package com.buykop.console.entity;

import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.per.MemberId;
import com.buykop.framework.annotation.per.OrgId;
import com.buykop.framework.annotation.per.UserId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.BaseChartEntity;
import com.buykop.console.util.Constants;


@Mongo(code = "M_USER_BEHAVIOR_ANALYSIC", display = "用户行为分析", sys = Constants.current_sys, defaultOrderByField = "", retainDays = 0,redisEntity=false)
public class UserBehaviorAnalysis extends BaseChartEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8151983818203949808L;

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

	@Column(dbLen = DBLen.codeValueLen, displayName = "部门", isNotNull = false, label = "")
	private Long srcType;// 0:WEB 1:WX 2:APP 3:DEVICE 4:SSO

	@Column(dbLen = DBLen.idLen, displayName = "系统", isNotNull = false, label = "")
	protected String sys;//

	@Column(dbLen = DBLen.idLen, displayName = "模块", isNotNull = false, label = "")
	protected String module;//

	@Column(dbLen = DBLen.idLen, displayName = "页面", isNotNull = false, label = "")
	protected String page;//

	@Column(dbLen = DBLen.len8, displayName = "时间(秒)", isNotNull = false, label = "")
	protected Long costTime;
	
	@Column(dbLen = DBLen.idLen, displayName = "标题", isNotNull = false, label = "",fieldType=2)
	protected String title;

	@Column(dbLen = DBLen.len256, displayName = "备注", isNotNull = false, label = "",fieldType=1)
	protected String remark;

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

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	public Long getSrcType() {
		return srcType;
	}

	public void setSrcType(Long srcType) {
		this.srcType = srcType;
	}

	public String getSys() {
		return sys;
	}

	public void setSys(String sys) {
		this.sys = sys;
	}

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}

	public String getPage() {
		return page;
	}

	public void setPage(String page) {
		this.page = page;
	}

	public Long getCostTime() {
		return costTime;
	}

	public void setCostTime(Long costTime) {
		this.costTime = costTime;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
	
	
	

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String getChartDateDisplay() {
		// TODO Auto-generated method stub
		return "行为日期";
	}

}
