package com.buykop.console.entity;

import com.buykop.framework.annotation.Code;
import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.Display;
import com.buykop.framework.annotation.ExColumn;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.Parent;
import com.buykop.framework.annotation.Sort;
import com.buykop.framework.annotation.per.PlaceId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.sort.CommonSort;
import com.buykop.framework.util.type.Entity;
import com.buykop.console.util.Constants;


@Mongo(code = POrg.table, sys = Constants.current_sys, display = "部门", retainDays = 0, defaultOrderByField = "",map=true)
public class POrg extends Entity implements CommonSort {
	
	protected static final String table = "M_ORG";
	
	private static final long serialVersionUID = -5747208933604594299L;

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "", isNotNull = false, label = "")
	protected String orgId;//

	@Column(dbLen = DBLen.note, displayName = "机构S", isNotNull = false, label = "")
	private String allOrgIds;

	@Display
	@Column(dbLen = DBLen.len64, displayName = "部门名称", isNotNull = true, label = "",fieldType=2)
	protected String orgName;//

	
	@Code(len = 6)
	@Column(dbLen = DBLen.len32, displayName = "部门编码", isNotNull = false, label = "",fieldType=2)
	protected String orgCode;//

	@Column(dbLen = DBLen.len64, displayName = "外部编码", isNotNull = false, label = "",fieldType=2)
	private String outCode;

	@Sort
	@Column(dbLen = DBLen.codeValueTypeLen, displayName = "排序", isNotNull = false, label = "")
	protected Long seq;

	@Column(dbLen = DBLen.idLen, displayName = "所属机构", isNotNull = false, label = "", bindingFormId = BosConstants.LIST_ALL_MEMBER,redis=true)
	@FK(table = PMember.class)
	protected String memberId;//

	@Parent
	@Column(dbLen = DBLen.idLen, displayName = "上级部门", isNotNull = false, label = "", defaultValue = "0", parent = "orgId",bindingFormId = BosConstants.LIST_PARENT_ORG_BYCURRENTMEMBER,redis=true)//
	@FK(table = POrg.class, display = "")
	protected String parentId;//

	@CodeValue(codeType = "136")
	@Column(dbLen = DBLen.len1, displayName = "部门类型", isNotNull = false, label = "", defaultValue = "0")
	protected Long orgType;//

	@Column(dbLen = DBLen.len1024, displayName = "描述", isNotNull = false, label = "",fieldType=2)
	protected String orgDesc;

	@FK(table = PUser.class, display = "")
	@Column(dbLen = DBLen.idLen, displayName = "负责人", isNotNull = false, label = "", parent = "orgId",redis=true)
	protected String chargeUser;// 负责人

	@CodeValue(codeType = "501")
	@Column(dbLen = DBLen.len1, displayName = "状态", isNotNull = false, label = "", defaultValue = "1")
	protected Long status;

	
	@Column(dbLen = DBLen.len8, displayName = "下级子部门数量", isNotNull = false, label = "")
	protected Long subCount;//
	
	@FK(table=Table.class)
	@Column(dbLen = DBLen.len256, displayName = "绑定业务类",  label = "",fieldType=2)
	private String className;
	
	@Column(dbLen = DBLen.idLen, displayName = "业务主键 ", label = "")
	private String bizId;
	
	
	
	// @Transient

	@ExColumn(dbLen = DBLen.len32, displayName = "上级编码", label = "")
	protected String parentCode;

	public POrg() {
		// this.ssdTableName=TABLE;
	}

	public String getOutCode() {
		return outCode;
	}

	public void setOutCode(String outCode) {
		this.outCode = outCode;
	}

	
	
	public String getCountryId() {
		return countryId;
	}

	public void setCountryId(String countryId) {
		this.countryId = countryId;
	}

	public String getProvinceId() {
		return provinceId;
	}

	public void setProvinceId(String provinceId) {
		this.provinceId = provinceId;
	}

	public String getCityId() {
		return cityId;
	}

	public void setCityId(String cityId) {
		this.cityId = cityId;
	}

	public String getCountyId() {
		return countyId;
	}

	public void setCountyId(String countyId) {
		this.countyId = countyId;
	}

	public String getTownId() {
		return townId;
	}

	public void setTownId(String townId) {
		this.townId = townId;
	}

	public String getVillageId() {
		return villageId;
	}

	public void setVillageId(String villageId) {
		this.villageId = villageId;
	}

	public Long getSubCount() {
		return subCount;
	}

	public void setSubCount(Long subCount) {
		this.subCount = subCount;
	}

	

	public String getOrgDesc() {
		return orgDesc;
	}

	public void setOrgDesc(String orgDesc) {
		this.orgDesc = orgDesc;
	}

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	public String getOrgName() {
		return orgName;
	}

	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}

	public String getOrgCode() {
		return orgCode;
	}

	public void setOrgCode(String orgCode) {
		this.orgCode = orgCode;
	}

	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public Long getOrgType() {
		return orgType;
	}

	public void setOrgType(Long orgType) {
		this.orgType = orgType;
	}

	public String getChargeUser() {
		return chargeUser;
	}

	public void setChargeUser(String chargeUser) {
		this.chargeUser = chargeUser;
	}

	public Long getStatus() {
		return status;
	}

	public void setStatus(Long status) {
		this.status = status;
	}

	public String getParentCode() {
		return parentCode;
	}

	public void setParentCode(String parentCode) {
		this.parentCode = parentCode;
	}

	public String getAllOrgIds() {
		return allOrgIds;
	}

	public void setAllOrgIds(String allOrgIds) {
		this.allOrgIds = allOrgIds;
	}

	public Long getSeq() {
		return seq;
	}

	public void setSeq(Long seq) {
		this.seq = seq;
	}



	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getBizId() {
		return bizId;
	}

	public void setBizId(String bizId) {
		this.bizId = bizId;
	}

	public Long sort() {
		// TODO Auto-generated method stub
		return this.seq;
	}
}
