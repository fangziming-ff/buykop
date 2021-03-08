package com.buykop.console.entity.product;


import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.util.type.Entity;
import com.buykop.framework.util.BosConstants;
import com.buykop.console.util.Constants;


@Mongo(code = RPageInf.table, sys = Constants.current_sys, display = "页面接口", retainDays = 0, defaultOrderByField = "")
public class RPageInf extends Entity {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7296801889481518510L;

	protected static final String table = "M_PAGE_INF";

	@PK
	@FK( table = ProductPage.class)
	@Column(dbLen = DBLen.idLen, displayName = "页面", isNotNull = false, label = "",fieldType=2)
	protected String pageId;

	@PK
	@FK( table = DiyInf.class)
	@Column(dbLen = DBLen.idLen, displayName = "接口编号", isNotNull = false, label = "",fieldType=2)
	protected String infCode;
	
	
	@FK( table = Product.class)
	@Column(dbLen = DBLen.idLen, displayName = "产品", isNotNull = false, label = "")
	protected String productId;
	
	
	

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getPageId() {
		return pageId;
	}

	public void setPageId(String pageId) {
		this.pageId = pageId;
	}

	public String getInfCode() {
		return infCode;
	}

	public void setInfCode(String infCode) {
		this.infCode = infCode;
	}

}
