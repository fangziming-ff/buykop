package com.buykop.console.entity.product;

import java.util.Vector;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.Display;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PBFile;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.Parent;
import com.buykop.framework.annotation.Sort;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.sort.Common;
import com.buykop.framework.util.type.Entity;
import com.buykop.framework.util.BosConstants;

import com.buykop.console.util.Constants;

@Mongo(sys = Constants.current_sys, display = "产品菜单", code = ProductMenuDiy.CODE, retainDays = 0, defaultOrderByField = "")
public class ProductMenuDiy extends Entity implements Common {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2247102702307463730L;

	protected static final String CODE = "M_PRODUCT_MENU";

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "菜单", isNotNull = false, label = "")
	protected String menuId;

	@FK(table = Product.class)
	@Column(dbLen = DBLen.idLen, displayName = "产品", isNotNull = false, label = "")
	protected String productId;

	@Display
	@Column(dbLen = DBLen.len256, displayName = "菜单名称", isNotNull = false, label = "", fieldType = 2)
	protected String menuName;

	@Parent
	@Column(dbLen = DBLen.idLen, displayName = "上级菜单", isNotNull = false, label = "",parent="productId",bindingFormId=BosConstants.LIST_PRODUCT_MENU,defaultValue="0")
	protected String pId;

	@Column(dbLen = DBLen.idLen, displayName = "前端路由路径", isNotNull = false, label = "")
	protected String routerPath;

	@CodeValue(codeType = "501")
	@Column(dbLen = DBLen.len1, displayName = "状态", isNotNull = false, label = "", defaultValue = "0")
	protected Long status;// 0:禁用 1:启用

	@Column(dbLen = DBLen.idLen, displayName = "图标", isNotNull = false, label = "")
	protected String ico;

	@Column(dbLen = DBLen.idLen, displayName = "链接地址", isNotNull = false, label = "")
	protected String url;
	
	
	@CodeValue(codeType = "3")
	@Column(dbLen = DBLen.len1, displayName = "叶子节点", isNotNull = false, label = "",defaultValue="0")
	protected Integer isLeaf;
	

	@FK(table = PRole.class)
	@Column(dbLen = DBLen.idLen, displayName = "显示角色限制", isNotNull = false, label = "")
	protected String showRoles;
	
	
	@CodeValue(codeType = "3")
	@Column(dbLen = DBLen.codeValueTypeLen, displayName = "允许个人访问", label = "", defaultValue = "0")
	protected Integer person;

	@CodeValue(codeType = "3")
	@Column(dbLen = DBLen.codeValueTypeLen, displayName = "允许游客访问", label = "", defaultValue = "0")
	protected Integer visitor;
	
	
	@CodeValue(codeType = "3")
	@Column(dbLen = DBLen.codeValueTypeLen, displayName = "只允许机构管理访问", label = "", defaultValue = "0")
	protected Integer memberAdmin;

	@CodeValue(codeType = "3")
	@Column(dbLen = DBLen.codeValueTypeLen, displayName = "只允许超级管理员访问", label = "", defaultValue = "0")
	protected Integer sysAdmin;

	@Sort
	@Column(dbLen = DBLen.len8, displayName = "排序", isNotNull = false, label = "")
	protected Long sort;

	
	public Integer getPerson() {
		return person;
	}

	public void setPerson(Integer person) {
		this.person = person;
	}

	public Integer getVisitor() {
		return visitor;
	}

	public void setVisitor(Integer visitor) {
		this.visitor = visitor;
	}

	public String getMenuId() {
		return menuId;
	}

	public void setMenuId(String menuId) {
		this.menuId = menuId;
	}

	public String getProductId() {
		return productId;
	}

	public void setProductId(String productId) {
		this.productId = productId;
	}

	public String getMenuName() {
		return menuName;
	}

	public void setMenuName(String menuName) {
		this.menuName = menuName;
	}

	public String getpId() {
		return pId;
	}

	public void setpId(String pId) {
		this.pId = pId;
	}

	public String getRouterPath() {
		return routerPath;
	}

	public void setRouterPath(String routerPath) {
		this.routerPath = routerPath;
	}

	public Long getStatus() {
		return status;
	}

	public void setStatus(Long status) {
		this.status = status;
	}

	public String getIco() {
		return ico;
	}

	public void setIco(String ico) {
		this.ico = ico;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Long getSort() {
		return sort;
	}

	public void setSort(Long sort) {
		this.sort = sort;
	}
	
	

	public Integer getMemberAdmin() {
		return memberAdmin;
	}

	public void setMemberAdmin(Integer memberAdmin) {
		this.memberAdmin = memberAdmin;
	}

	public Integer getSysAdmin() {
		return sysAdmin;
	}

	public void setSysAdmin(Integer sysAdmin) {
		this.sysAdmin = sysAdmin;
	}

	public String getShowRoles() {
		return showRoles;
	}

	public void setShowRoles(String showRoles) {
		this.showRoles = showRoles;
	}

	@Override
	public Long sort() {
		// TODO Auto-generated method stub
		return this.sort;
	}
	
	
	

	public Integer getIsLeaf() {
		return isLeaf;
	}

	public void setIsLeaf(Integer isLeaf) {
		this.isLeaf = isLeaf;
	}

	/**
	 * 通过角色控制
	 */
	public int judgePer(UserToken token) {
		// TODO Auto-generated method stub
		
		if(this.isLeaf==null) {
			this.isLeaf=0;
		}
		
		if(this.isLeaf==0) {
			return 0;
		}
		
		
		if(this.visitor==null) {
			this.visitor=0;
		}
		
		if(this.visitor==0 &&  DataChange.isEmpty(token.getUserId())) {
			return 14;
		}
		
		if(this.visitor==1) {
			return 0;
		}
		
		
		if(this.person==null) {
			this.person=0;
		}
		
		if(this.person==0 &&  DataChange.isPerson(token.getMemberId())) {
			return 14;
		}
		
		if(this.person==1) {
			return 0;
		}
		
		

		if (DataChange.isEmpty(this.showRoles))
			return 0;
		
		if(token.roleJudge(this.showRoles)) {
			return 0;
		}

		return 15;
	}
	
	
	
	
	/**
	 * 
	 * @param token
	 * @return
	 * @throws Exception
	 */
	public JSONObject getTreeJson(UserToken token) throws Exception {
		
		JSONObject json=new JSONObject(true);

		Table table = this.showTable();

		if (DataChange.isEmpty(table.getTreeParentField())) {
			if (BosConstants.runTimeMode()) {
				throw new Exception("实体类:" + this.getEntityClassName() + " 没有设置上级属性,无法形成树形结构");
			} else {
				throw new Exception("实体对象:" + table.getDisplayName() + " 没有设置上级属性,无法形成树形结构");
			}
		}

		
		
		if(this.isLeaf!=null &&  this.isLeaf.intValue()==1) {
			
			if(this.judgePer(token)==0) {
				json.put("id", this.getPk());
				json.put("text",DataChange.replaceNull(this.menuName));
				json.put("routerPath", DataChange.replaceNull(this.routerPath));
				json.put("ico", DataChange.replaceNull(this.ico));
				json.put("url", DataChange.replaceNull(this.url));
				json.put("isLeaf","1");
			}
			
		}else {
			
			if (this.getSubList() != null && this.getSubList().size() > 0) {
				
				json.put("id", this.getPk());
				json.put("text",DataChange.replaceNull(this.menuName));
				json.put("routerPath", DataChange.replaceNull(this.routerPath));
				json.put("ico", DataChange.replaceNull(this.ico));
				json.put("url", DataChange.replaceNull(this.url));
				json.put("isLeaf","0");
				JSONArray array = new JSONArray();
				for (int i = 0; i < this.getSubList().size(); i++) {
					ProductMenuDiy sub=(ProductMenuDiy)this.getSubList().get(i);
					JSONObject json2 = sub.getTreeJson(token);
					if(json2!=null) {
						array.add(json2);
					}
				}
				if(array.size()>1) {
					json.put("children", array);
				}
				json.put("size", array.size());
				
			}
			
		}
		
		
		if(json.size()>0) return json;
		return null;
		
	}

}
