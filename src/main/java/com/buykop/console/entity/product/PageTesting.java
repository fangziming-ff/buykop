package com.buykop.console.entity.product;

import java.util.Date;


import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.per.OwnerUserId;
import com.buykop.framework.annotation.per.UserId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.util.type.Entity;
import com.buykop.framework.util.BosConstants;

import com.buykop.console.util.Constants;


@Mongo(code = PageTesting.CODE, sys = Constants.current_sys, display = "页面测试结果", retainDays = 0, defaultOrderByField = "")
public class PageTesting extends Entity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5628323126022883836L;

	protected static final String CODE = "M_PAGE_TESTING";

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "测试", isNotNull = false, label = "")
	protected String testId;

	@FK(table = ProductPage.class)
	@Column(dbLen = DBLen.idLen, displayName = "页面", isNotNull = false, label = "",fieldType=2)
	protected String pageId;

	
	@CodeValue(codeType = "79010002")
	@Column(dbLen = DBLen.codeValueLen, displayName = "BUG等级", isNotNull = false, label = "")
	protected Integer bugLevel;//
	
	@Column(dbLen = DBLen.len4, displayName = "测试批次", isNotNull = false, label = "", sensitive = true,defaultValue="0")
	protected Integer testingBatch;

	@OwnerUserId
	@Column(dbLen = DBLen.idLen, displayName = "测试人员", label = "")
	private String testUserId;

	@UserId
	@Column(dbLen = DBLen.idLen, displayName = "开发人员", label = "", sensitive = true)
	private String userId;

	@Column(dbLen = DBLen.len128, displayName = "备注", label = "", sensitive = true)
	private String remark;

	@Column(dbLen = DBLen.dateLen, displayName = "bug时间", label = "")
	private Date bugTime;

	@CodeValue(codeType = "3")
	@Column(dbLen = DBLen.codeValueLen, displayName = "是否修复", isNotNull = false, label = "")
	protected Integer status;

	@Column(dbLen = DBLen.dateLen, displayName = "修复日期", label = "")
	private Date fixDate;
	
	

	public String getTestId() {
		return testId;
	}

	public void setTestId(String testId) {
		this.testId = testId;
	}

	public Integer getBugLevel() {
		return bugLevel;
	}

	public void setBugLevel(Integer bugLevel) {
		this.bugLevel = bugLevel;
	}

	public Integer getTestingBatch() {
		return testingBatch;
	}

	public void setTestingBatch(Integer testingBatch) {
		this.testingBatch = testingBatch;
	}

	public String getTestUserId() {
		return testUserId;
	}

	public void setTestUserId(String testUserId) {
		this.testUserId = testUserId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public Date getBugTime() {
		return bugTime;
	}

	public void setBugTime(Date bugTime) {
		this.bugTime = bugTime;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public Date getFixDate() {
		return fixDate;
	}

	public void setFixDate(Date fixDate) {
		this.fixDate = fixDate;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	
}
