package com.buykop.console.entity;

import com.buykop.framework.annotation.Code;
import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.Display;
import com.buykop.framework.annotation.ExColumn;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.Sort;
import com.buykop.framework.annotation.per.OwnerPlaceId;
import com.buykop.framework.annotation.per.UserId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.sort.CommonSort;
import com.buykop.framework.util.type.Entity;
import com.buykop.console.util.Constants;


@Mongo(code = PMember.table, sys = Constants.current_sys, display = "机构", retainDays = 0, defaultOrderByField = "",map=true)
public class PMember extends Entity implements CommonSort {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4148343374567984296L;

	protected static final String table = "M_MEMBER";

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "机构", label = "")
	protected String memberId;//
	
	
	@FK(table=PUser.class)
	@Column(dbLen = DBLen.idLen, displayName = "管理者账号", label = "",redis=true)
	protected String adminId;
	

	@FK(table = PMember.class)
	@Column(dbLen = DBLen.idLen, displayName = "运营方", label = "", defaultValue = "1",bindingFormId=Constants.LIST_ALL_OIS) // , bindingFormId =																		// 
	protected String oisId;

	@CodeValue(codeType = "3")
	@Column(dbLen = DBLen.codeValueTypeLen, displayName = "是否运营方", label = "", defaultValue = "0")
	protected Long isOis;

	@Column(dbLen = DBLen.len64, displayName = "推荐码", label = "")
	protected String serialCode;

	@Column(dbLen = DBLen.len64, displayName = "统一社会信用代码证", label = "", sensitive = true,fieldType=2)
	protected String memberCode;//

	@Code(len = 6)
	@Column(dbLen = DBLen.len64, displayName = "内部代码", label = "",fieldType=2)
	protected String innerCode;//

	@Column(dbLen = DBLen.len64, displayName = "外部编码", isNotNull = false, label = "")
	private String outCode;

	@Column(dbLen = DBLen.len64, displayName = "简称", label = "",fieldType=2)
	protected String shortName;//

	@Display
	@Column(dbLen = DBLen.len128, displayName = "机构名称", label = "", sensitive = true,fieldType=2)
	protected String name;//

	@Column(dbLen = DBLen.len64, displayName = "英文名", label = "", sensitive = true,fieldType=2)
	protected String enName;//

	@Sort
	@Column(dbLen = DBLen.len8, displayName = "排序", label = "")
	private Long seq;

	@Column(dbLen = DBLen.len4, displayName = "账号数量限制", label = "")
	protected Long accountLimit;//

	@CodeValue(codeType = "125")
	@Column(dbLen = DBLen.codeValueTypeLen, displayName = "接入网络类型", label = "")
	protected Long netWorkType;

	
	@Column(dbLen = DBLen.len64, displayName = "平台子域名", label = "", sensitive = true)
	protected String subdomain ;
	
	@Column(dbLen = DBLen.len64, displayName = "域名", label = "", sensitive = true)
	protected String domain;

	@Column(dbLen = DBLen.len64, displayName = "首页", label = "")
	protected String indexPage;

	@Column(dbLen = DBLen.len64, displayName = "个性化系统名称", label = "",fieldType=2)
	protected String diySysName;

	@CodeValue(codeType = "501")
	@Column(dbLen = DBLen.len1, displayName = "状态", label = "", defaultValue = "1")
	protected Long status;

	@Column(dbLen = DBLen.nameLen, displayName = "联系人", label = "", sensitive = true,fieldType=2)
	private String linkMan;

	@Column(dbLen = DBLen.nameLen, displayName = "联系电话", label = "", sensitive = true,fieldType=2)
	private String linkPhone;

	@Column(dbLen = DBLen.nameLen, displayName = "联系邮件", isNotNull = false, label = "",fieldType=2)
	private String linkMail;

	@Column(dbLen = DBLen.len256, displayName = "开户行", isNotNull = false, label = "", sensitive = true,fieldType=2)
	protected String bank;

	@Column(dbLen = DBLen.len256, displayName = "银行账号", isNotNull = false, label = "", sensitive = true)
	protected String accountNum;

	@FK(table = PMember.class)
	@Column(dbLen = DBLen.idLen, displayName = "上级主管机构", label = "", defaultValue = "0", bindingFormId = BosConstants.LIST_ALL_MEMBER,redis=true)
	protected String parentId;//

	@Column(dbLen = DBLen.len512, displayName = "备注", isNotNull = false, label = "")
	protected String remark;

	@OwnerPlaceId
	@Column(dbLen = DBLen.idLen, displayName = "所属地区", isNotNull = false, label = "")
	protected String ownerPlaceId;

	@ExColumn(dbLen = DBLen.len64, displayName = "姓名", label = "")
	protected String userName;//

	@ExColumn(dbLen = DBLen.len16, displayName = "管理员账号", label = "", check = "loginname")
	protected String loginName;//

	@ExColumn(dbLen = DBLen.len64, displayName = "登录密码", label = "")
	protected String loginPwd;//
	
	
	

	public String getAdminId() {
		return adminId;
	}





	public void setAdminId(String adminId) {
		this.adminId = adminId;
	}





	public String getSubdomain() {
		return subdomain;
	}





	public void setSubdomain(String subdomain) {
		this.subdomain = subdomain;
	}

	// @Transient
	@ExColumn(dbLen = DBLen.idLen, displayName = "距离", label = "")
	private Long distance;

	public PUser getUser() {
		PUser info = new PUser();
		if(DataChange.isEmpty(this.adminId)) {
			info.setUserId(this.memberId);
		}else {
			info.setUserId(this.adminId);
		}
		info.setMemberId(this.memberId);
		info.setLoginName(this.loginName);
		info.setUserName(this.userName);
		info.setLoginPwd(this.loginPwd);
		info.setStatus(1L);
		return info;
	}
	
	
	
	

	public String getOwnerPlaceId() {
		return ownerPlaceId;
	}

	public void setOwnerPlaceId(String ownerPlaceId) {
		this.ownerPlaceId = ownerPlaceId;
	}

	public String getIndexPage() {
		return indexPage;
	}

	public void setIndexPage(String indexPage) {
		this.indexPage = indexPage;
	}

	public PMember() {
		// super.ssdTableName=TABLE;
	}

	public Long getIsOis() {
		return isOis;
	}

	public void setIsOis(Long isOis) {
		this.isOis = isOis;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getLoginPwd() {
		return loginPwd;
	}

	public String getOutCode() {
		return outCode;
	}

	public void setOutCode(String outCode) {
		this.outCode = outCode;
	}

	public String getInnerCode() {
		return innerCode;
	}

	public void setInnerCode(String innerCode) {
		this.innerCode = innerCode;
	}

	public Long getNetWorkType() {
		return netWorkType;
	}

	public void setNetWorkType(Long netWorkType) {
		this.netWorkType = netWorkType;
	}

	public Long getSeq() {
		return seq;
	}

	public void setSeq(Long seq) {
		this.seq = seq;
	}

	public Long getAccountLimit() {
		return accountLimit;
	}

	public void setAccountLimit(Long accountLimit) {
		this.accountLimit = accountLimit;
	}

	public void setLoginPwd(String loginPwd) {
		this.loginPwd = loginPwd;
	}

	public String getDiySysName() {
		return diySysName;
	}

	public void setDiySysName(String diySysName) {
		this.diySysName = diySysName;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public String getMemberCode() {
		return memberCode;
	}

	public void setMemberCode(String memberCode) {
		this.memberCode = memberCode;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEnName() {
		return enName;
	}

	public void setEnName(String enName) {
		this.enName = enName;
	}

	public Long getStatus() {
		return status;
	}

	public void setStatus(Long status) {
		this.status = status;
	}

	public Long getDistance() {
		return distance;
	}

	public void setDistance(Long distance) {
		this.distance = distance;
	}

	public String getSerialCode() {
		return serialCode;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public void setSerialCode(String serialCode) {
		this.serialCode = serialCode;
	}

	public String getOisId() {
		return oisId;
	}

	public void setOisId(String oisId) {
		this.oisId = oisId;
	}

	public String getLinkMan() {
		return linkMan;
	}

	public void setLinkMan(String linkMan) {
		this.linkMan = linkMan;
	}

	public String getLinkPhone() {
		return linkPhone;
	}

	public void setLinkPhone(String linkPhone) {
		this.linkPhone = linkPhone;
	}

	public String getLinkMail() {
		return linkMail;
	}

	public void setLinkMail(String linkMail) {
		this.linkMail = linkMail;
	}

	public String getBank() {
		return bank;
	}

	public void setBank(String bank) {
		this.bank = bank;
	}

	public String getAccountNum() {
		return accountNum;
	}

	public void setAccountNum(String accountNum) {
		this.accountNum = accountNum;
	}

	@Override
	public Long sort() {
		// TODO Auto-generated method stub
		return this.seq;
	}

}
