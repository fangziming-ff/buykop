package com.buykop.console.entity;

import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.Display;
import com.buykop.framework.annotation.Index;
import com.buykop.framework.annotation.IndexList;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.util.type.Entity;

import com.buykop.framework.util.BosConstants;

@Mongo(sys = BosConstants.current_sys, display = "来源渠道", code = "M_SOURCE_CHANNEL", defaultOrderByField = "", retainDays = 0)
@IndexList(value = { @Index(fields = "channelName", name = "channelName", unique = true, prompt = "渠道名称重复", seq = 0),
		@Index(fields = "domain", name = "domain", unique = true, prompt = "域名重复", seq = 200) })
public class Channel extends Entity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1798422062115361680L;

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "来源渠道", label = "")
	protected String channelId;

	@Display
	@Column(dbLen = DBLen.idLen, displayName = "渠道名称", label = "", fieldType = 2)
	protected String channelName;

	@Column(dbLen = DBLen.idLen, displayName = "域名", label = "", redis = true, fieldType = 2)
	protected String domain;

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	public String getChannelName() {
		return channelName;
	}

	public void setChannelName(String channelName) {
		this.channelName = channelName;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

}
