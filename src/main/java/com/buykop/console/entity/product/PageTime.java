package com.buykop.console.entity.product;

import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.Entity;
import java.util.Date;

import com.buykop.console.util.Constants;

@Mongo(sys = Constants.current_sys, display = "页面开发耗时", code = PageTime.CODE, retainDays = 0, defaultOrderByField = "")
public class PageTime extends Entity{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1752062308282897776L;


	protected static final String CODE = "M_PAGE_TIME";
	
	
	@PK
	@Column(dbLen = DBLen.idLen, displayName = "计时", isNotNull = false, label = "")
	protected String timeId;
	
	
	@FK(table = ProductPage.class)
	@Column(dbLen = DBLen.idLen, displayName = "页面", isNotNull = false, label = "")
	protected String pageId;

	@Column(dbLen = DBLen.dateLen, displayName = "开发开始时间", label = "")
	private Date startTime;

	@Column(dbLen = DBLen.dateLen, displayName = "开发完成时间", label = "")
	private Date endTime;
	
	@Column(dbLen = DBLen.len4, displayName = "耗时(小时)", isNotNull = false, label = "")
	protected Integer factHour;

	
	
	public String getTimeId() {
		return timeId;
	}

	public void setTimeId(String timeId) {
		this.timeId = timeId;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Integer getFactHour() {
		return factHour;
	}

	public void setFactHour(Integer factHour) {
		this.factHour = factHour;
	}
	
	
	
	

}
