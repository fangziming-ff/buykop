package com.buykop.console.controller;

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
import com.buykop.console.service.TableService;
import com.buykop.console.thread.util.SynData;
import com.buykop.console.thread.util.SynToEs;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.SynTableDataConfig;
import com.buykop.framework.entity.SynTableDataItem;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;

@Module(display = "数据表同步", sys = Constants.current_sys)
@RestController
public class TableSynDataController  extends BaseController {
	
	
	protected final static String URI = "/table/synData";

	private static Logger logger = LoggerFactory.getLogger(TableSynDataController.class);

	@Autowired
	private TableService service;
	
	
	@Menu(js = "synData", name = "同步复制", trunk = "基础信息,数据管理")
	@Security(accessType = "1", displayName = "设置列表", needLogin = true, isEntAdmin = false, isSysAdmin =  false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			SynTableDataConfig search = json.getSearch(SynTableDataConfig.class, "",ut,this.service);
			PageInfo page = json.getPageInfo(SynTableDataConfig.class);
			QueryFetchInfo<SynTableDataConfig> fetch=this.service.getFetch(search, "srcSys,srcClassName,sys,className", page.getCurrentPage(), page.getPageSize());
			super.fetchToJson(fetch, json, search.showTable());
			
			super.selectToJson(PRoot.getJsonForDev(ut.getMemberId(),this.service), json, SynTableDataConfig.class.getSimpleName(), "srcSys");
			super.selectToJson(PRoot.getJsonForDev(ut.getMemberId(),this.service), json, SynTableDataConfig.class.getSimpleName(), "sys");
			
			
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "设置列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			List<SynTableDataConfig> list=json.getList(SynTableDataConfig.class, "title,srcSys,srcClassName,sys,className,status", service);
			for(SynTableDataConfig x:list) {
				if(x.getClassName().equals(x.getSrcClassName())) {
					json.setUnSuccess(-1, "源表与目标表不能相同");
					return json.jsonValue();
				}
				this.service.save(x, ut);
				
				
				CacheTools.removeSynTableDataConfig(x.getSrcClassName());
				
				new ClassInnerNotice().invoke(SynTableDataConfig.class.getSimpleName(), x.getSrcClassName());
			}
	
	
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "设置详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/info", SynTableDataConfig.class.getName(), null, true, service);
			
			SynTableDataConfig obj=this.service.getById(id, SynTableDataConfig.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(SynTableDataConfig.class,id);
				return json.jsonValue();
			}
			super.objToJson(obj, json);
			
			
			SynTableDataItem item=new SynTableDataItem();
			item.setClassName(id);
			QueryListInfo<SynTableDataItem> list=this.service.getList(item, "field");
			super.listToJson(list, json, item.showTable());
			
			
			
			super.selectToJson(Field.getJsonForSelect(obj.getClassName(), 1L,json.getLan(),service), json, SynTableDataItem.class.getSimpleName(), "field");
			
			super.selectToJson(Field.getJsonForSelect(obj.getSrcClassName(), 1L,json.getLan(),service), json, SynTableDataItem.class.getSimpleName(), "srcField");
			
			
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	

	@Security(accessType = "1", displayName = "设置删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/delete", SynTableDataConfig.class.getName(), null, true, service);
			this.service.deleteById(id, SynTableDataConfig.class.getName(), ut);
			
			
			CacheTools.removeSynTableDataConfig(id);
			
			new ClassInnerNotice().invoke(SynTableDataConfig.class.getSimpleName(), id);
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	@Security(accessType = "1", displayName = "设置保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/info", SynTableDataConfig.class.getName(), null, true, service);
			
			SynTableDataConfig obj=json.getObj(SynTableDataConfig.class, null, service);
			obj.setSrcClassName(id);
			this.service.save(obj, ut);
			
			
			//Table srcTable=BosConstants.getTable(id);//如果是非唯一主键,需要检查属性,必须带有主键属性
			Table toTable=BosConstants.getTable(obj.getClassName());//如果是非唯一主键,需要检查属性,必须带有主键属性
			//Vector<String> srcV=Field.split(srcTable.getKeyField());
			Vector<String> toV=Field.split(toTable.getKeyField());
			
			
			SynTableDataItem item=new SynTableDataItem();
			item.setClassName(id);
			this.service.delete(item, ut);
			
			List<SynTableDataItem> list=json.getList(SynTableDataItem.class, "field,srcField", service);
			for(SynTableDataItem x:list) {
				//srcV.remove(x.getSrcField());
				toV.remove(x.getField());
				x.setClassName(id);
				this.service.save(x, ut);
			}
			
			//if(srcV.size()>0) {
				//json.setUnSuccess(-1, srcTable.getDisplayName()+"绑定属性必须含有:"+srcV);
				//return json.jsonValue();
			//}
			
			if(toV.size()>0) {
				json.setUnSuccess(-1, toTable.getDisplayName()+"绑定属性必须含有:"+toV);
				return json.jsonValue();
			}
			
			
			CacheTools.removeSynTableDataConfig(obj.getSrcClassName());
			
			new ClassInnerNotice().invoke(SynTableDataConfig.class.getSimpleName(), obj.getSrcClassName());
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
			
	
	@Security(accessType = "1", displayName = "同步到ES", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/synData", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject synData(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI+"/synData", Table.class.getName(),
					null, true,this.getService());
			
			
			SynTableDataConfig obj=this.service.getById(id, SynTableDataConfig.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(SynTableDataConfig.class,id);
				return json.jsonValue();
			}
			

			// 发起线程进行同步
			SynData call = new SynData(id);
			Thread t1 = new Thread(call);
			t1.start();

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	

	@Override
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}
	
	
	
	
}
