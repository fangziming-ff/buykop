package com.buykop.console.entity.product;

import java.math.BigDecimal;
import java.util.Date;

import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.Display;
import com.buykop.framework.annotation.ExColumn;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.per.OwnerUserId;
import com.buykop.framework.annotation.per.UserId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.Entity;
import com.buykop.framework.util.BosConstants;

import com.buykop.console.util.Constants;

@Mongo(sys = Constants.current_sys, display = "页面任务", code = ProductPage.CODE, retainDays = 0, defaultOrderByField = "")
public class ProductPage extends Entity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3652468875895310434L;

	protected static final String CODE = "M_PRODUCT_PAGE";

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "页面", isNotNull = false, label = "")
	protected String pageId;

	@Column(dbLen = DBLen.idLen, displayName = "页面编号", isNotNull = false, label = "", fieldType = 2, caseSensitive = 2)
	protected String pageCode;

	@Display
	@Column(dbLen = DBLen.idLen, displayName = "页面名称", isNotNull = false, label = "", fieldType = 2)
	protected String displayName;

	@FK(table = Product.class)
	@Column(dbLen = DBLen.idLen, displayName = "产品", isNotNull = false, label = "")
	protected String productId;

	@FK(table = ProductMenuDiy.class)
	@Column(dbLen = DBLen.idLen, displayName = "菜单", isNotNull = false, label = "")
	protected String menuId;

	@CodeValue(codeType = "157")
	@Column(dbLen = DBLen.codeValueLen, displayName = "页面类型", isNotNull = false, label = "")
	protected Integer productType;// 0:聚合页 1:普通页 2:小弹窗

	@Column(dbLen = DBLen.idLen, displayName = "备注", isNotNull = false, label = "", fieldType = 2)
	protected String remark;

	@Column(dbLen = DBLen.len4, displayName = "功能权重", label = "")
	private Integer funWeight;

	@Column(dbLen = DBLen.len4, displayName = "交互权重", label = "")
	private Integer designWeight;

	@Column(dbLen = DBLen.len4, displayName = "功能权重评分", label = "")
	private Integer funWeightFact;

	@Column(dbLen = DBLen.len4, displayName = "交互符合评分", label = "")
	private Integer designWeightFact;
	
	
	@Column(dbLen = "3,1", displayName = "附加评分", label = "")
	private BigDecimal addedFact;
	

	@Column(dbLen = DBLen.len4, displayName = "计划用时(小时)", isNotNull = false, label = "", sensitive = true, defaultValue = "1")
	protected Integer planHour;

	@Column(dbLen = DBLen.len4, displayName = "实际用时(小时)", isNotNull = false, label = "", sensitive = true)
	protected Integer factHour;

	@Column(dbLen = DBLen.len4, displayName = "当前测试批次", isNotNull = false, label = "", sensitive = true, defaultValue = "0")
	protected Integer testingBatch;

	@Column(dbLen = DBLen.dateLen, displayName = "任务开始时间", label = "", sensitive = true)
	private Date startTime;

	@Column(dbLen = DBLen.len4, displayName = "绩效所属年份", label = "", sensitive = true)
	private Integer startYear;

	@CodeValue(codeType = "13")
	@Column(dbLen = DBLen.len4, displayName = "绩效所属月份", label = "", sensitive = true)
	private String startMonth;

	@Column(dbLen = DBLen.dateLen, displayName = "提测时间", label = "")
	private Date testTime;

	@Column(dbLen = DBLen.dateLen, displayName = "任务完成时间", label = "", sensitive = true)
	private Date endTime;

	@OwnerUserId
	@Column(dbLen = DBLen.idLen, displayName = "前端开发人员", label = "", sensitive = true)
	private String userId;

	@CodeValue(codeType = "79010001")
	@Column(dbLen = DBLen.codeValueLen, displayName = "状态", isNotNull = false, label = "", defaultValue = "0", sensitive = true)
	protected Integer status;// 0:未开发 1:开发中 2:返工 3:提测 4:测试中 5:完成

	@Column(dbLen = DBLen.len4, displayName = "返工次数", isNotNull = false, label = "", defaultValue = "0")
	protected Integer backNum;

	@CodeValue(codeType = "79010004")
	@Column(dbLen = DBLen.codeValueLen, displayName = "测试结果", isNotNull = false, label = "", sensitive = true)
	protected Integer testResult;;// 0:不合格 1:合格
	
	
	@Column(dbLen = "3,1", displayName = "评分结果", isNotNull = false, label = "", sensitive = true)
	protected BigDecimal evaluation;

	@FK( table = PageTime.class)
	@Column(dbLen = DBLen.idLen, displayName = "当前开发计时", isNotNull = false, label = "")
	protected String timeId;

	@ExColumn(dbLen = DBLen.dateLen, displayName = "开发开始时间", label = "")
	private Date devStartTime;

	@ExColumn(dbLen = DBLen.dateLen, displayName = "开发完成时间", label = "")
	private Date devEndTime;
	
	
	

	public BigDecimal getAddedFact() {
		return addedFact;
	}

	public void setAddedFact(BigDecimal addedFact) {
		this.addedFact = addedFact;
	}

	public Integer getFunWeightFact() {
		return funWeightFact;
	}

	public void setFunWeightFact(Integer funWeightFact) {
		this.funWeightFact = funWeightFact;
	}

	public Integer getDesignWeightFact() {
		return designWeightFact;
	}

	public void setDesignWeightFact(Integer designWeightFact) {
		this.designWeightFact = designWeightFact;
	}

	public Integer getFunWeight() {
		return funWeight;
	}

	public void setFunWeight(Integer funWeight) {
		this.funWeight = funWeight;
	}

	public Integer getDesignWeight() {
		return designWeight;
	}

	public void setDesignWeight(Integer designWeight) {
		this.designWeight = designWeight;
	}

	public Integer getBackNum() {
		return backNum;
	}

	public void setBackNum(Integer backNum) {
		this.backNum = backNum;
	}

	public Integer getTestResult() {
		return testResult;
	}

	public void setTestResult(Integer testResult) {
		this.testResult = testResult;
	}

	public Integer getStartYear() {
		return startYear;
	}

	public void setStartYear(Integer startYear) {
		this.startYear = startYear;
	}

	public String getStartMonth() {
		return startMonth;
	}

	public void setStartMonth(String startMonth) {
		this.startMonth = startMonth;
	}

	public Integer getPlanHour() {
		return planHour;
	}

	public void setPlanHour(Integer planHour) {
		this.planHour = planHour;
	}

	public Integer getFactHour() {
		return factHour;
	}

	public void setFactHour(Integer factHour) {
		this.factHour = factHour;
	}

	public Integer getTestingBatch() {
		return testingBatch;
	}

	public void setTestingBatch(Integer testingBatch) {
		this.testingBatch = testingBatch;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getTestTime() {
		return testTime;
	}

	public void setTestTime(Date testTime) {
		this.testTime = testTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getMenuId() {
		return menuId;
	}

	public void setMenuId(String menuId) {
		this.menuId = menuId;
	}

	public Integer getProductType() {
		return productType;
	}

	public void setProductType(Integer productType) {
		this.productType = productType;
	}

	public String getPageCode() {
		return pageCode;
	}

	public void setPageCode(String pageCode) {
		this.pageCode = pageCode;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTimeId() {
		return timeId;
	}

	public void setTimeId(String timeId) {
		this.timeId = timeId;
	}

	public Date getDevStartTime() {
		return devStartTime;
	}

	public void setDevStartTime(Date devStartTime) {
		this.devStartTime = devStartTime;
	}

	public Date getDevEndTime() {
		return devEndTime;
	}

	public void setDevEndTime(Date devEndTime) {
		this.devEndTime = devEndTime;
	}

	public BigDecimal getEvaluation() {
		return evaluation;
	}

	public void setEvaluation(BigDecimal evaluation) {
		this.evaluation = evaluation;
	}

}
