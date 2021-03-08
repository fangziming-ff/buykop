package com.buykop.console.entity;

import java.util.Date;

import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Index;
import com.buykop.framework.annotation.IndexList;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.per.UserId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.scan.ServerConfig;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.Entity;
import com.buykop.console.util.Constants;

@Mongo(sys = Constants.current_sys, display = "系统日志", code = "E_SYS_LOG", defaultOrderByField = "", retainDays = 0, redisEntity = false, transaction = false)
@IndexList(value = { @Index(fields = "!logTime", name = "logTime", unique = false, prompt = "", seq = 0) })
public class SysLog extends Entity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -778580774706902071L;

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "", isNotNull = false, label = "")
	protected String id;//

	@CodeValue(codeType = "61")
	@Column(dbLen = DBLen.codeValueLen, displayName = "日志类型", label = "")
	private Long dbAction;// 1:DEBUG 2:INFO 3:WARN 4:ERROR

	@FK(table = Table.class)
	@Column(dbLen = DBLen.len256, displayName = "业务类", label = "", fieldType = 2)
	private String className;

	@Column(dbLen = DBLen.len64, displayName = "方法名", isNotNull = false, label = "")
	protected String method;

	@Column(dbLen = DBLen.len512, displayName = "信息", isNotNull = false, label = "", fieldType = 2)
	protected String msg;//

	@Column(dbLen = DBLen.len512, displayName = "备注", isNotNull = false, label = "", fieldType = 1)
	protected String info;//

	@Column(dbLen = DBLen.dateLen, displayName = "日志时间", label = "")
	private Date logTime;

	@UserId
	@Column(dbLen = DBLen.idLen, displayName = "执行人", label = "")
	private String invokeUserId;

	@FK(table = ServerConfig.class)
	@Column(dbLen = DBLen.idLen, displayName = "服务", label = "")
	private String serverId;
	
	

	public String getServerId() {
		return serverId;
	}

	public void setServerId(String serverId) {
		this.serverId = serverId;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Date getLogTime() {
		return logTime;
	}

	public void setLogTime(Date logTime) {
		this.logTime = logTime;
	}

	public String getInvokeUserId() {
		return invokeUserId;
	}

	public void setInvokeUserId(String invokeUserId) {
		this.invokeUserId = invokeUserId;
	}

}
