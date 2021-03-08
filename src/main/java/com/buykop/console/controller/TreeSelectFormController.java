package com.buykop.console.controller;

import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.DataFormService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.TreeSelectForm;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;
import com.buykop.console.util.Constants;

@Module(display = "树形选择", sys = Constants.current_sys)
@RestController
@RequestMapping(TreeSelectFormController.URI)
public class TreeSelectFormController  extends BaseController{
	
	private static Logger  logger=LoggerFactory.getLogger(TreeSelectFormController.class);

	protected static final String URI="/treeSelect";
	
	@Autowired
	private DataFormService service;
	
	@Menu(js = "treeSelect", name = "业务关联树形选择", trunk = "开发服务,模板管理")
	@Security(accessType = "1", displayName = "业务关联树形列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 



		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			TreeSelectForm search=json.getSearch(TreeSelectForm.class, null, ut,this.service);
			PageInfo page=json.getPageInfo(TreeSelectForm.class);
			QueryFetchInfo<BosEntity> fetch=this.service.getMgClient().getFetch(search, "rsys,rclassName", page.getCurrentPage(), page.getPageSize(),this.service);
			fetch.initBiddingForSysClassName(this, json, "rsys", "rclassName", 0L);
			fetch.initBiddingForSysClassName(this, json, "sys", "className", 0L);
			super.fetchToJson(fetch, json, BosConstants.getTable(TreeSelectForm.class.getName()));
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, TreeSelectForm.class.getName(), "sys");
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, TreeSelectForm.class.getName(), "rsys");
			
			
		
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "通用绑定模板列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		

		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			List<TreeSelectForm> list=json.getList(TreeSelectForm.class, "code,name,sys,!className,rsys,!rclassName",this.service);
			for(TreeSelectForm x:list) {
				this.service.save(x,ut);
			}
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "业务关联树形详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/info", TreeSelectForm.class.getName(), null, true,this.getService());
			TreeSelectForm obj=this.service.getMgClient().getById(id, TreeSelectForm.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(TreeSelectForm.class,id);
				return json.jsonValue();
			}
			
			if(DataChange.isEmpty(obj.getSys())) {
				json.setUnSuccess(-1, "请设置树形所属系统");
				return json.jsonValue();
			}
			
			if(DataChange.isEmpty(obj.getClassName())) {
				json.setUnSuccess(-1, "请设置树形绑定业务类");
				return json.jsonValue();
			}
			
			
			if(DataChange.isEmpty(obj.getRsys())) {
				json.setUnSuccess(-1, "请设置业务所属系统");
				return json.jsonValue();
			}
			
			if(DataChange.isEmpty(obj.getRclassName())) {
				json.setUnSuccess(-1, "请设置绑定业务类");
				return json.jsonValue();
			}
			
			
			
			//,
			
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, TreeSelectForm.class.getName(), "rsys");
			super.selectToJson(Table.getJsonForSelect(obj.getRsys(),0L,this.service), json, TreeSelectForm.class, "rclassName");
			
			if(!DataChange.isEmpty(obj.getRclassName())) {
				
				Table table=this.service.getMgClient().getTableById(obj.getRclassName());
				if(table.getRegType()==null) {
					table.setRegType(1L);
				}
				
				super.selectToJson(Field.getJsonForSelect(obj.getRclassName(), 1L,json.getLan(),this.service), json, TreeSelectForm.class, "selectedIdField");
				super.selectToJson(Field.getJsonForSelect(obj.getRclassName(), 1L,json.getLan(),this.service), json, TreeSelectForm.class, "bizIdField");
				
			}
			
			
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, TreeSelectForm.class.getName(), "sys");
			if(!DataChange.isEmpty(obj.getSys())) {
				super.selectToJson(Table.getJsonForTreeSelect(obj.getSys(),0L,this.service), json, TreeSelectForm.class, "className");
			}
			super.objToJson(obj, json);
	
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "业务关联树形删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/delete", TreeSelectForm.class.getName(), null, true,this.getService());
			TreeSelectForm obj=this.service.getMgClient().getById(id, TreeSelectForm.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(TreeSelectForm.class,id);
				return json.jsonValue();
			}
		
			
			this.service.getMgClient().deleteByPK(id, TreeSelectForm.class,ut,this.service);
	
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "业务关联树形保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			
			

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			TreeSelectForm obj=json.getObj(TreeSelectForm.class, "rsys,rclassName,sys,className",this.service);
			this.service.save(obj,ut);
	
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	
	
	
	@Security(accessType = "1*,2*", displayName = "加载树形结构数据", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/loadTreeData", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject loadTreeData(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 



		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String formId=json.getSelectedId(Constants.current_sys, URI+"/loadTreeData", TreeSelectForm.class.getName(), null, true,this.getService());
			TreeSelectForm obj=this.service.getMgClient().getById(formId, TreeSelectForm.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(TreeSelectForm.class,formId);
				return json.jsonValue();
			}
			super.objToJson(obj, json);
			
			
			String bizId=json.getSelectedId(Constants.current_sys, URI+"/saveTreeData", obj.getRclassName(), obj.getBizIdField(), true,this.getService());
			
			
			Vector<String> selectedV=new Vector<String>();
			BosEntity be=new TableJson(obj.getRclassName());
			be.putValue(obj.getBizIdField(), bizId);
			be.initByFormual(obj.getRinitScript(),json);
			
			QueryListInfo<BosEntity> slist=this.service.getList(be,be.showTable().getSortField());
			for(BosEntity x:slist.getList()) {
				selectedV.add(x.propertyValueString(obj.getSelectedIdField()));
			}

			BosEntity search=new TableJson(obj.getClassName());
			search.initByFormual(obj.getInitScript(),json);
			
			QueryListInfo<BosEntity> list=this.service.getList(search,search.showTable().getSortField());
			
			for(BosEntity x:list.getList()) {
				if(selectedV.contains(x.getPk())) {
					x.setSelected();
				}
			}
			
			
			
			json.getData().put("TREE",list.toTreeJson("0") );
			
			
			/**if(obj.getLazyLoad()==null) {
				obj.setLazyLoad(1L);
			}
			
			
			if(obj.getLazyLoad().intValue()==1) {
				SelectBidding  bidding=list.getSelectBidding(false,null,null,ut,null,this.service);
				json.getData().put("TREE",bidding.showJSON() );
			}else {
				SelectBidding  bidding=list.getSelectBidding(true,null,null,ut,null,this.service);
				json.getData().put("TREE",bidding.showJSON() );
			}*/
			
			
			
	
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	
	
	
	@Security(accessType = "1*,2*", displayName = "保存", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/saveTreeData", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveTreeData(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 



		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String formId=json.getSelectedId(Constants.current_sys, URI+"/loadTreeData", TreeSelectForm.class.getName(), null, true,this.getService());
			TreeSelectForm obj=this.service.getMgClient().getById(formId, TreeSelectForm.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(TreeSelectForm.class,formId);
				return json.jsonValue();
			}
			
			String bizId=json.getSelectedId(Constants.current_sys, URI+"/saveTreeData", obj.getRclassName(), obj.getBizIdField(), true,this.getService());
			
			BosEntity be=new TableJson(obj.getRclassName());
			be.putValue(obj.getBizIdField(), bizId);
			be.initByFormual(obj.getRinitScript(),json);
			this.service.delete(be,ut,true);
			
			
			String ids=json.getSimpleData("ids", "选择记录", String.class, false,this.getService());
			
			if(!DataChange.isEmpty(ids)) {
				Vector<String> v=MyString.splitBy(ids, ",");
				for(String idValue:v) {
					be=new TableJson(obj.getRclassName());
					be.putValue(obj.getBizIdField(), bizId);
					be.putValue(obj.getSelectedIdField(), idValue);
					be.initByFormual(obj.getRinitScript(),json);
					be.setIsValid(1);
					this.service.save(be, ut);
				}
			}
				
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	
	
	
	
	
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}
	
	
}
