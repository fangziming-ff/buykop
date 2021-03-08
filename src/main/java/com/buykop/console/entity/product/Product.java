package com.buykop.console.entity.product;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.Display;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.per.OwnerUserId;
import com.buykop.framework.annotation.per.UserId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.DiyInfField;
import com.buykop.framework.entity.DiyInfMsgPush;
import com.buykop.framework.entity.DiyInfOutField;
import com.buykop.framework.entity.DiyInfQuery;
import com.buykop.framework.entity.DiyInfQueryOperation;
import com.buykop.framework.entity.DiyInfSub;
import com.buykop.framework.entity.DiyInfSynField;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.sort.Common;
import com.buykop.framework.util.type.Entity;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.BosConstants;

import com.buykop.console.util.Constants;

@Mongo(sys = Constants.current_sys, display = "产品", code = Product.CODE, retainDays = 0, defaultOrderByField = "")
public class Product extends Entity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3734365766785734743L;

	protected static final String CODE = "M_PRODUCT";

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "产品", isNotNull = false, label = "")
	protected String productId;

	@Display
	@Column(dbLen = DBLen.len256, displayName = "产品名称", isNotNull = false, label = "", fieldType = 2)
	protected String productName;

	@UserId
	@Column(dbLen = DBLen.idLen, displayName = "测试人员", label = "")
	private String testUserId;

	@UserId
	@Column(dbLen = DBLen.idLen, displayName = "开发负责人", label = "")
	private String leaderId;

	@OwnerUserId
	@Column(dbLen = DBLen.idLen, displayName = "产品经理", label = "")
	private String userId;


	@CodeValue(codeType = "79010003")
	@Column(dbLen = DBLen.idLen, displayName = "状态", isNotNull = false, label = "", defaultValue = "1", sensitive = true)
	protected Integer status;// 1:立项 2:开发中 3:结束

	@FK(table = PRoot.class)
	@Column(dbLen = DBLen.len32, displayName = "所属系统", label = "")
	private String sys;

	private List<ProductMenuDiy> menuList = new ArrayList<ProductMenuDiy>();

	private List<ProductPage> pageList = new ArrayList<ProductPage>();

	private List<RPageInf> infList = new ArrayList<RPageInf>();

	private List<RPageInfBatch> infBatchList = new ArrayList<RPageInfBatch>();

	public List<RPageInf> getInfList() {
		return infList;
	}

	public void setInfList(List<RPageInf> infList) {
		this.infList = infList;
	}

	public List<RPageInfBatch> getInfBatchList() {
		return infBatchList;
	}

	public void setInfBatchList(List<RPageInfBatch> infBatchList) {
		this.infBatchList = infBatchList;
	}

	public List<ProductPage> getPageList() {
		return pageList;
	}

	public void setPageList(List<ProductPage> pageList) {
		this.pageList = pageList;
	}

	public List<ProductMenuDiy> getMenuList() {
		return menuList;
	}

	public void setMenuList(List<ProductMenuDiy> menuList) {
		this.menuList = menuList;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getTestUserId() {
		return testUserId;
	}

	public void setTestUserId(String testUserId) {
		this.testUserId = testUserId;
	}

	public String getLeaderId() {
		return leaderId;
	}

	public void setLeaderId(String leaderId) {
		this.leaderId = leaderId;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public String getSys() {
		return sys;
	}

	public void setSys(String sys) {
		this.sys = sys;
	}

	

	public static Product loadProduct(ServiceInf service, String id) throws Exception {

		Object value = BosConstants.getExpireHash().getObj(Product.class.getName(), id);
		if (value == null) {

			Product obj = service.getById(id, Product.class);
			if (obj == null)
				return null;

			if (true) {// 菜单
				ProductMenuDiy s = new ProductMenuDiy();
				s.setProductId(id);
				s.setStatus(1L);
				QueryListInfo<ProductMenuDiy> list = service.getList(s, "sort");
				obj.setMenuList(list.getList());
			}

			if (true) {
				ProductPage s = new ProductPage();
				s.setProductId(id);
				QueryListInfo<ProductPage> list = service.getList(s, "pageCode");
				obj.setPageList(list.getList());
			}

			if (true) {
				RPageInf s = new RPageInf();
				s.setProductId(id);
				QueryListInfo<RPageInf> list = service.getList(s, "infCode");
				obj.setInfList(list.getList());
			}

			if (true) {
				RPageInfBatch s = new RPageInfBatch();
				s.setProductId(id);
				QueryListInfo<RPageInfBatch> list = service.getList(s, "infCode");
				obj.setInfBatchList(list.getList());
			}

			BosConstants.getExpireHash().putObj(Product.class.getName(), id, obj);

			return obj;

		}
		return (Product) value;

	}

}
