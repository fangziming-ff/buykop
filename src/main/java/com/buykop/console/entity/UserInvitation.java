package com.buykop.console.entity;

import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.per.MemberId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.Entity;
import com.buykop.console.util.Constants;


@Mongo(code = UserInvitation.CODE, display = "邀请成员", retainDays = 0, sys = Constants.current_sys, defaultOrderByField = "")
public class UserInvitation extends Entity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4796986413871325967L;

	protected static final String CODE = "M_USER_INVITATION";

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "邀请", isNotNull = false, label = "")
	protected String invitationId;//

	@Column(dbLen = DBLen.idLen, displayName = "邮件/手机", isNotNull = false, label = "")
	protected String mobile;

	@MemberId
	@Column(dbLen = DBLen.idLen, displayName = "机构", isNotNull = false, label = "")
	protected String memberId;

	@CodeValue(codeType = "3")
	@Column(dbLen = DBLen.codeValueLen, displayName = "是否同意", isNotNull = false, label = "")
	protected String status;

	@FK(table = PRole.class, multi = true)
	@Column(dbLen = DBLen.len256, displayName = "角色", label = "")
	protected String roles;//

	public String getInvitationId() {
		return invitationId;
	}

	public void setInvitationId(String invitationId) {
		this.invitationId = invitationId;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getMemberId() {
		return memberId;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

}
