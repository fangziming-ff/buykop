package com.buykop.console.entity;

import java.util.Date;

import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Index;
import com.buykop.framework.annotation.IndexList;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.per.MemberId;
import com.buykop.framework.annotation.per.UserId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.scan.ServerConfig;
import com.buykop.framework.util.type.BaseChartEntity;
import com.buykop.console.util.Constants;

/**
 * 链路跟踪
 * 
 * @author GIF
 *
 */
@Mongo(code = "M_INVOKE_LOG", display = "链路跟踪", sys = Constants.current_sys, defaultOrderByField = "!invokeTime", retainDays = 0, redisEntity = false, transaction = false)
@IndexList(value = { @Index(fields = "!invokeTime", name = "invokeTime", unique = false, prompt = "", seq = 0) })
public class InvokeLog extends BaseChartEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6937231398484165256L;

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "", isNotNull = false, label = "")
	protected String id;//

	@Column(dbLen = DBLen.idLen, displayName = "业务id", isNotNull = false, label = "")
	protected String busiId;//

	@Column(dbLen = DBLen.len256, displayName = "业务类名", isNotNull = false, label = "", fieldType = 2)
	protected String className;//

	@Column(dbLen = DBLen.len64, displayName = "入口方法", isNotNull = false, label = "", fieldType = 2)
	protected String method;//

	@Column(dbLen = DBLen.len64, displayName = "操作类型", isNotNull = false, label = "")
	protected String actionType;//

	@Column(dbLen = DBLen.len512, displayName = "备注", isNotNull = false, label = "", fieldType = 2)
	protected String remark;//

	@Column(dbLen = DBLen.len512, displayName = "参数", isNotNull = false, label = "", fieldType = 2)
	protected String param;//
	
	
	@Column(dbLen = DBLen.len512, displayName = "返回结果", isNotNull = false, label = "", fieldType = 2)
	protected String result;//

	@Column(dbLen = DBLen.dateLen, displayName = "调用时间", isNotNull = false, label = "")
	protected Date invokeTime;

	@Column(dbLen = DBLen.len4, displayName = "执行时间(毫秒)", isNotNull = false, label = "")
	protected Long execTime;

	@CodeValue(codeType = "3")
	@Column(dbLen = DBLen.len4, displayName = "是否成功", isNotNull = false, label = "", defaultValue = "1")
	protected Integer success;

	@UserId
	@Column(dbLen = DBLen.idLen, displayName = "调用人", label = "")
	private String invokeUserId;

	@MemberId
	@Column(dbLen = DBLen.idLen, displayName = "调用机构", label = "")
	private String invokeMemberId;

	@Column(dbLen = DBLen.idLen, displayName = "访问令牌", label = "")
	private String token;

	@Column(dbLen = DBLen.len512, displayName = "事件", isNotNull = false, label = "", fieldType = 2)
	protected String event;//

	@FK(table = ServerConfig.class)
	@Column(dbLen = DBLen.idLen, displayName = "服务", label = "")
	private String serverId;
	
	

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public Integer getSuccess() {
		return success;
	}

	public void setSuccess(Integer success) {
		this.success = success;
	}

	public Long getExecTime() {
		return execTime;
	}

	public void setExecTime(Long execTime) {
		this.execTime = execTime;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getBusiId() {
		return busiId;
	}

	public void setBusiId(String busiId) {
		this.busiId = busiId;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public String getActionType() {
		return actionType;
	}

	public void setActionType(String actionType) {
		this.actionType = actionType;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
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

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getEvent() {
		return event;
	}

	public void setEvent(String event) {
		this.event = event;
	}

	public String getInvokeMemberId() {
		return invokeMemberId;
	}

	public void setInvokeMemberId(String invokeMemberId) {
		this.invokeMemberId = invokeMemberId;
	}

	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

	@Override
	public String getChartDateDisplay() {
		// TODO Auto-generated method stub
		return "记录时间";
	}

}
