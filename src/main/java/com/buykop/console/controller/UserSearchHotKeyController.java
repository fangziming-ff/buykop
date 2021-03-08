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
import com.buykop.console.service.UserSearchHotKeyService;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.HotKeyWord;
import com.buykop.framework.entity.HotKeyWordRankingList;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;

@Module(display = "热搜",sys = Constants.current_sys)
@RestController
@RequestMapping(UserSearchHotKeyController.URI)
public class UserSearchHotKeyController extends BaseController{
	
	
	private static Logger  logger=LoggerFactory.getLogger(UserSearchHotKeyController.class);
	
	protected static final String URI="/hotSearch";
	
	
	@Autowired
    private UserSearchHotKeyService service;
	
	
	
	
	@Menu(js = "hotSearch", name = "热搜关键字", trunk = "基础信息,数据管理")
	@Security(accessType = "1", displayName = "热搜翻页列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		try {

			UserToken ut = super.securityCheck(json,request);
			

			if (ut == null) {
				return json.jsonValue();
			}
	
			
			if(true) {
				HotKeyWord search=new HotKeyWord();
				Vector<String> cv=this.service.getMgClient().getVector(search, "className",this.service);
				SelectBidding data=new SelectBidding();
				for(String x:cv) {
					data.put(x, CacheTools.getEntityDisplay(x, Table.class.getName()));
				}
				super.selectToJson(data, json, HotKeyWord.class.getName(), "className");
			}


			
			HotKeyWord search=json.getSearch(HotKeyWord.class,null,ut,this.service);
			PageInfo page=json.getPageInfo(HotKeyWord.class);
			
			QueryFetchInfo<HotKeyWord> fetch=this.service.getMgClient().getFetch(search, "!searchNum", page.getCurrentPage(), page.getPageSize(),this.service);
			
			System.out.println("list size="+this.service.getMgClient().getList(search,this.service).size());
			
			for(HotKeyWord x:fetch.getList()) {
				Table table=BosConstants.getTable(x.getClassName());
				if(table==null) continue;
				x.setFormId(table.getFormId());
			}
			super.fetchToJson(fetch, json, BosConstants.getTable(HotKeyWord.class));
			
			
	
			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	
	
	
	
	@Security(accessType = "1", displayName = "热搜显示详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			List<HotKeyWord> list=json.getList(HotKeyWord.class, "hotKey,sys,className,!searchNum", this.service);
			for(HotKeyWord x:list) {
				if(x.getSearchNum()==null) {
					x.setSearchNum(0L);
				}
				this.service.save(x, ut);
			}
			

			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "热搜删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			

			if (ut == null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/delete", HotKeyWord.class.getName(), null,true,this.getService());
			
			this.service.getMgClient().deleteByPK(id, HotKeyWord.class,ut,this.service);

			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "热搜显示保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			

			if (ut == null) {
				return json.jsonValue();
			}
			
			HotKeyWord obj=json.getObj(HotKeyWord.class, "hotKey,className",this.service);
			
			Table table=this.service.getMgClient().getTableById(obj.getClassName());
			if(table==null) {
				json.setUnSuccessForNoRecord(Table.class,obj.getClassName());
				return json.jsonValue();
			}
			
			if(!table.judgePK()) {
				json.setUnSuccess(-1, "不支持联合主键类对象");
				return json.jsonValue();
			}
			
			
			this.service.save(obj,ut);
			
			
			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
		
	
	@Security(accessType = "1", displayName = "热搜业务翻页列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "/fetchForBiz", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetchForBiz(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			

			if (ut == null) {
				return json.jsonValue();
			}
	
	
	
			String id = json.getSelectedId(Constants.current_sys, URI+"/bizList",HotKeyWord.class.getName(),null,true,this.getService());
			
			HotKeyWord obj=this.service.getMgClient().getById(id, HotKeyWord.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(HotKeyWord.class,id);
				return json.jsonValue();
			}
			
			Table table=BosConstants.getTable(obj.getClassName());
			if(table==null) {
				json.setUnSuccessForNoRecord(Table.class,obj.getClassName());
				return json.jsonValue();
			}
			super.objToJson(table, json);
			
			
			PForm form=this.service.getMgClient().getById(table.getFormId(), PForm.class);
			if(form==null) {
				json.setUnSuccessForNoRecord(PForm.class,table.getFormId());
				return json.jsonValue();
			}
			super.objToJson(form, json);
			
			PageInfo page=json.getPageInfo(obj.getClassName());
			BosEntity search=json.getSearch(obj.getClassName(), null, ut,this.service);
			//search.setSearchKeyWord(obj.getHotKey());
			QueryFetchInfo<BosEntity> fetch=this.service.getFetch(search, table.getSortField(), page.getCurrentPage(), page.getPageSize());
			super.fetchToJson(fetch, json, table.getSimpleName(),form.getListFields2(table),table);

			
	
			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
		}



	@Security(accessType = "1", displayName = "热搜对象排行翻页列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "/bizList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject bizList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		try {

			UserToken ut = super.securityCheck(json,request);
			

			if (ut == null) {
				return json.jsonValue();
			}
	
	
			
			String id = json.getSelectedId(Constants.current_sys, URI+"/bizList",HotKeyWord.class.getName(),null,true,this.getService());
			
			HotKeyWord obj=this.service.getMgClient().getById(id, HotKeyWord.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(HotKeyWord.class,id);
				return json.jsonValue();
			}
			
			
			HotKeyWordRankingList search=json.getSearch(HotKeyWordRankingList.class, null, ut, service);
			search.setKwId(obj.getId());
			search.setClassName(obj.getClassName());
			QueryListInfo<HotKeyWordRankingList> list=this.service.getList(search, "seq");
			for(HotKeyWordRankingList x:list.getList()) {
				x.setDisplay(CacheTools.getEntityDisplay(x.getIdValue(), x.getClassName()));
			}
			
			super.listToJson(list, json, HotKeyWordRankingList.class.getSimpleName(), search.showTable().listDBFields(false)+",display", search.showTable());
			
			

			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	@Security(accessType = "1", displayName = "热搜对象排行删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "/bizDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject bizDelete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/bizDelete", HotKeyWordRankingList.class.getName(), null,true, service);
			
			this.service.deleteById(id, HotKeyWordRankingList.class.getName(), ut);
			

			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
			
	
	
	
	
	@Security(accessType = "1", displayName = "热搜对象排行保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "/bizSaveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject bizSaveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			List<HotKeyWordRankingList> list=json.getList(HotKeyWordRankingList.class, "kwId,className,idValue,seq", this.service);
			for(HotKeyWordRankingList x:list) {
				this.service.save(x, ut);
			}
			
			

			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
			
			
		
	
	
	
	@Security(accessType = "1", displayName = "热搜对象排行保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_oisAdmin)
	@RequestMapping(value = "/bizSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject bizSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
	
			
			
			String id = json.getSelectedId(Constants.current_sys, URI+"/bizList",HotKeyWord.class.getName(),null,true,this.getService());
			
			HotKeyWord kw=this.service.getMgClient().getById(id, HotKeyWord.class);
			if(kw==null) {
				json.setUnSuccessForNoRecord(HotKeyWord.class,id);
				return json.jsonValue();
			}
			
			
			
			String idValue=json.getSelectedId(Constants.current_sys, URI+"/bizSave",kw.getClassName(),null,true,this.getService());
			
			
			//不能超过10个推荐
			HotKeyWordRankingList obj=new HotKeyWordRankingList();
			obj.setKwId(kw.getId());
			obj.setClassName(kw.getClassName());
			if(this.service.getCount(obj)>10) {
				json.setUnSuccess(-1, "推荐不能超过10个");
				return json.jsonValue();
			}
			
			
			obj=new HotKeyWordRankingList();
			obj.setKwId(kw.getId());
			obj.setClassName(kw.getClassName());
			obj.setIdValue(idValue);
			obj=this.service.getById(obj.getPk(), HotKeyWordRankingList.class);
			if(obj==null) {
				obj=new HotKeyWordRankingList();
				obj.setSeq(0);
			}
			obj.setKwId(kw.getId());
			obj.setClassName(kw.getClassName());
			obj.setIdValue(idValue);
			BosEntity biz=this.service.getById(idValue, kw.getClassName());
			if(biz!=null && !DataChange.isEmpty(biz.showTable().getOwnerMemberIdField()) ) {
				obj.setOwnerMemberId(biz.propertyValueString(biz.showTable().getOwnerMemberIdField()));
			}
			this.service.save(obj, ut);
			
			
			json.setSuccess("选择成功");
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}

}
