package com.buykop.console.entity;

import java.util.Date;

import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.Display;
import com.buykop.framework.annotation.ExColumn;
import com.buykop.framework.annotation.IndexList;
import com.buykop.framework.annotation.Index;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PBFile;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.per.MemberId;
import com.buykop.framework.annotation.per.OrgId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.annotation.util.DataCheck;
import com.buykop.framework.entity.IpPools;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.Entity;
import com.buykop.console.util.Constants;

@Mongo(code = PUser.table, sys = Constants.current_sys, display = "用户", retainDays = 0, defaultOrderByField = "")
@IndexList(value = { @Index(fields = "mobile", name = "mobile", unique = true, prompt = "手机号重复", seq = 0),
		@Index(fields = "mail", name = "mail", unique = true, prompt = "邮件重复", seq = 10),
		@Index(fields = "loginName", name = "loginName", unique = true, prompt = "登录名重复", seq = 20),
		@Index(fields = "userKey,userSecret", name = "userKey", unique = true, prompt = "USERKEY重复", seq = 30),
		@Index(fields = "serialCode", name = "serialCode", unique = true, prompt = "推荐码重复", seq = 200) })
public class PUser extends Entity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3037043953792935626L;

	protected static final String table = "M_USER";

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "", isNotNull = false, label = "")
	protected String userId;//

	@FK(table = Channel.class)
	@Column(dbLen = DBLen.idLen, displayName = "来源渠道", label = "",redis=true)
	protected String channelId;

	@FK(table = PUser.class)
	@Column(dbLen = DBLen.idLen, displayName = "推荐者", label = "")
	protected String recommendId;

	@Column(dbLen = DBLen.len64, displayName = "推荐码", label = "")
	protected String serialCode;

	@CodeValue(codeType = "1")
	@Column(dbLen = DBLen.codeValueTypeLen, displayName = "性别", label = "")
	protected Long sex;

	@Column(dbLen = DBLen.len16, displayName = "登录名", label = "", check = DataCheck.loginName, fieldType = 2, sensitive = true,redis=true)
	protected String loginName;//

	@Column(dbLen = DBLen.len64, displayName = "昵称", label = "", fieldType = 2)
	protected String nickName;//

	@Column(dbLen = DBLen.len64, displayName = "登录密码", label = "",check = DataCheck.passwd, encryption = true)
	protected String loginPwd;//

	@Display
	@Column(dbLen = DBLen.len64, displayName = "姓名", label = "", fieldType = 2, sensitive = true,redis=true)
	protected String userName;//

	@Column(dbLen = DBLen.dateLen, displayName = "帐号起始日期", label = "")
	protected Date startDate;//

	@Column(dbLen = DBLen.dateLen, displayName = "帐号终止日期", label = "")
	protected Date endDate;//

	// protected Long state;//
	@FK(display = "", table = PMember.class)
	@ExColumn(dbLen = DBLen.idLen, displayName = "所属单位", label = "")
	protected String memberId;//

	@MemberId
	@Column(dbLen = DBLen.idLen, displayName = "默认所属机构", label = "") // ,bindingFormId=BosConstants.LIST_MEMBER_BY_USER
	protected String defaultMemberId;//

	@Column(dbLen = DBLen.len4, displayName = "登录失败次数", label = "")
	protected Long faildNum;//

	@Column(dbLen = DBLen.len64, displayName = "邮箱", label = "",  check = DataCheck.mail, fieldType = 2, sensitive = true,redis=true)
	protected String mail;//

	@Column(dbLen = DBLen.mobile, displayName = "手机号码", label = "",  check = DataCheck.mobile, fieldType = 2, sensitive = true,redis=true)
	protected String mobile;//

	@Column(dbLen = DBLen.dateLen, displayName = "密码修改时间", label = "")
	protected Date pwdChangeTime;// 如果为空,强制用户修改密码
	
	@Column(dbLen = DBLen.dateLen, displayName = "最后登录时间", label = "")
	protected Date lastLoginTime;
	
	@FK(table=IpPools.class)
	@Column(dbLen = DBLen.ip, displayName = "最后登录IP", label = "")
	protected String lastLoginIp;

	@Column(dbLen = DBLen.len1, displayName = "状态", label = "", defaultValue = "1")
	@CodeValue(codeType = "811")
	protected Long status;// 0:未启用 1:启用

	@PBFile(property = "")
	@Column(dbLen = DBLen.idLen, displayName = "头像", label = "",redis=true)
	protected String avatarFileId;

	@CodeValue(codeType = "68")
	@Column(dbLen = DBLen.codeValueTypeLen, displayName = "密码提问", label = "")
	protected Long question;

	@Column(dbLen = DBLen.idLen, displayName = "密码回答", label = "", encryption = true)
	protected String answer;

	@Column(dbLen = DBLen.len32, displayName = "证件号码", isNotNull = false, label = "", check = "idcardnum", sensitive = true)
	protected String idCardNum;//

	@CodeValue(codeType = "888")
	@Column(dbLen = DBLen.codeValueTypeLen, displayName = "证件类型", isNotNull = false, label = "", defaultValue = "0")
	protected Long idCardType;//

	@Column(dbLen = DBLen.len128, displayName = "UserKey", isNotNull = false, label = "")
	private String userKey;

	@Column(dbLen = DBLen.len128, displayName = "UserSecret", isNotNull = false, label = "")
	private String userSecret;

	@FK(table = PUser.class)
	@ExColumn(dbLen = DBLen.idLen, displayName = "上级主管", label = "")
	protected String parentId;//

	@OrgId
	@ExColumn(dbLen = DBLen.idLen, displayName = "所属部门", label = "", parent = "memberId", bindingFormId = BosConstants.LIST_ALL_ORG_BYMEMBER) //
	protected String orgId;//

	@OrgId
	@ExColumn(dbLen = DBLen.idLen, displayName = "上级部门", label = "")
	protected String parentOrgId;//

	@ExColumn(dbLen = DBLen.len64, displayName = "确认密码", label = "")
	protected String rePwd;

	@ExColumn(dbLen = DBLen.len64, displayName = "原密码", label = "")
	protected String oldPwd;

	@ExColumn(dbLen = DBLen.len128, displayName = "验证码", label = "")
	protected String checkCode;

	@FK(table = PRole.class, display = "")
	@ExColumn(dbLen = DBLen.idLen, displayName = "角色", label = "")
	protected String roleId;//

	@ExColumn(dbLen = DBLen.len64, displayName = "用户编号", label = "")
	protected String userCode;

	@ExColumn(dbLen = DBLen.len64, displayName = "职务", label = "")
	protected String job;//

	@CodeValue(codeType = "3")
	@ExColumn(dbLen = DBLen.codeValueLen, displayName = "雇佣关系", label = "")
	protected Long employment;

	@ExColumn(dbLen = DBLen.dateLen, displayName = "雇佣起始日期", label = "")
	protected Date empStartDate;

	@ExColumn(dbLen = DBLen.dateLen, displayName = "雇佣终止日期", label = "")
	protected Date empEndDate;
	
	
	

	public String getLastLoginIp() {
		return lastLoginIp;
	}

	public void setLastLoginIp(String lastLoginIp) {
		this.lastLoginIp = lastLoginIp;
	}

	public Date getLastLoginTime() {
		return lastLoginTime;
	}

	public void setLastLoginTime(Date lastLoginTime) {
		this.lastLoginTime = lastLoginTime;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public void setInfo(PUserMember info) {
		if (info == null)
			return;
		this.setJob(info.getJob());
		this.setUserCode(info.getUserCode());
		this.setEmployment(info.getEmployment());
		this.setEmpEndDate(info.getEndDate());
		this.setEmpStartDate(info.getStartDate());
		this.setOrgId(info.getOrgId());
		this.setParentId(info.getParentId());
	}

	public PUserMember getInfo(String memberId) {
		PUserMember info = new PUserMember();
		info.setUserId(this.userId);
		info.setMemberId(memberId);
		info.setJob(this.getJob());
		info.setUserCode(this.getUserCode());
		info.setEmployment(this.getEmployment());
		info.setEndDate(this.getEmpEndDate());
		info.setStartDate(this.getEmpStartDate());
		info.setOrgId(this.getOrgId());
		info.setParentId(this.getParentId());
		return info;
	}

	public Long getEmployment() {
		return employment;
	}

	public void setEmployment(Long employment) {
		this.employment = employment;
	}

	public Date getEmpStartDate() {
		return empStartDate;
	}

	public void setEmpStartDate(Date empStartDate) {
		this.empStartDate = empStartDate;
	}

	public Date getEmpEndDate() {
		return empEndDate;
	}

	public void setEmpEndDate(Date empEndDate) {
		this.empEndDate = empEndDate;
	}

	public String getDefaultMemberId() {
		return defaultMemberId;
	}

	public void setDefaultMemberId(String defaultMemberId) {
		this.defaultMemberId = defaultMemberId;
	}

	public Date getPwdChangeTime() {
		return pwdChangeTime;
	}

	public void setPwdChangeTime(Date pwdChangeTime) {
		this.pwdChangeTime = pwdChangeTime;
	}

	public String getParentOrgId() {
		return parentOrgId;
	}

	public void setParentOrgId(String parentOrgId) {
		this.parentOrgId = parentOrgId;
	}

	public String getUserKey() {
		return userKey;
	}

	public void setUserKey(String userKey) {
		this.userKey = userKey;
	}

	public String getUserSecret() {
		return userSecret;
	}

	public void setUserSecret(String userSecret) {
		this.userSecret = userSecret;
	}

	public PUser() {
		// super.ssdTableName=TABLE;
	}

	public String getJob() {
		return job;
	}

	public void setJob(String job) {
		this.job = job;
	}

	public String getOldPwd() {
		return oldPwd;
	}

	public void setOldPwd(String oldPwd) {
		this.oldPwd = oldPwd;
	}

	public String getRoleId() {
		return roleId;
	}

	public void setRoleId(String roleId) {
		this.roleId = roleId;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
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

	public void setLoginPwd(String loginPwd) {
		this.loginPwd = loginPwd;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
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

	public Long getFaildNum() {
		return faildNum;
	}

	public void setFaildNum(Long faildNum) {
		this.faildNum = faildNum;
	}

	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getOrgId() {
		return orgId;
	}

	public void setOrgId(String orgId) {
		this.orgId = orgId;
	}

	public Long getStatus() {
		return status;
	}

	public void setStatus(Long status) {
		this.status = status;
	}

	public String getUserCode() {
		return userCode;
	}

	public void setUserCode(String userCode) {
		this.userCode = userCode;
	}

	public String getAvatarFileId() {
		return avatarFileId;
	}

	public void setAvatarFileId(String avatarFileId) {
		this.avatarFileId = avatarFileId;
	}

	public String getNickName() {
		return nickName;
	}

	public void setNickName(String nickName) {
		this.nickName = nickName;
	}

	public Long getQuestion() {
		return question;
	}

	public void setQuestion(Long question) {
		this.question = question;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public String getRecommendId() {
		return recommendId;
	}

	public void setRecommendId(String recommendId) {
		this.recommendId = recommendId;
	}

	public String getRePwd() {
		return rePwd;
	}

	public void setRePwd(String rePwd) {
		this.rePwd = rePwd;
	}

	public String getSerialCode() {
		return serialCode;
	}

	public void setSerialCode(String serialCode) {
		this.serialCode = serialCode;
	}

	public String getIdCardNum() {
		return idCardNum;
	}

	public void setIdCardNum(String idCardNum) {
		this.idCardNum = idCardNum;
	}

	public Long getIdCardType() {
		return idCardType;
	}

	public void setIdCardType(Long idCardType) {
		this.idCardType = idCardType;
	}

	public Long getSex() {
		return sex;
	}

	public void setSex(Long sex) {
		this.sex = sex;
	}

	public String getCheckCode() {
		return checkCode;
	}

	public void setCheckCode(String checkCode) {
		this.checkCode = checkCode;
	}

}
