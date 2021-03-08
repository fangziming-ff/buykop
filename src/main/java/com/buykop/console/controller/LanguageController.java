package com.buykop.console.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.LanLabelService;
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
import com.buykop.framework.scan.LableLanDisplay;
import com.buykop.framework.scan.PLanLabel;
import com.buykop.framework.scan.PLanguage;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;


@Module(display = "多语言支持", sys = Constants.current_sys)
@RestController
@RequestMapping(LanguageController.URI)
public class LanguageController extends BaseController{
	

	private static Logger  logger=LoggerFactory.getLogger(LanguageController.class);
	
	@Autowired
	private LanLabelService service;
	
	protected static final String URI="/language";
	

	@Menu(js = "lan", name = "多语言配置", trunk = "基础信息,配置管理")
	@Security(accessType = "1", displayName = "多语言列表", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/list",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject list(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		

		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(UserToken.createToken(true));
		}
		
		
		try {
			
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			PLanguage search=json.getSearch(PLanguage.class,null,ut,this.service);
			QueryListInfo<PLanguage> list=this.service.getMgClient().getList(search, "seq",this.service);
			super.listToJson(list, json, BosConstants.getTable(PLanguage.class));
			

			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	

	
	@Security(accessType = "1", displayName = "列表保存", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/saveList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			List<PLanguage> list=json.getList(PLanguage.class, "lan",this.service);
			for(PLanguage x:list) {
				if(x.getStatus()==null) {
					x.setStatus(0L);
				}
				x.setLan(x.getLan().toUpperCase());
				if(x.getLan().equals(BosConstants.defaultLan)) {
					json.setUnSuccess(-1, BosConstants.defaultLan+"是默认语种,不需要设置");
					return json.jsonValue();
				}
				this.service.save(x, null);
			}
			
			
			
			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/delete",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/delete", PLanguage.class.getName(), "", true,this.getService());
			
			PLanguage src=this.service.getMgClient().getById(id, PLanguage.class);
			if(src==null) {
				json.setUnSuccessForNoRecord(PLanguage.class,id);
				return json.jsonValue();
			}
			
			
			//是否被使用
			LableLanDisplay search=new LableLanDisplay();
			search.setLan(id);
			if(this.service.getMgClient().getCount(search,this.service)>0) {
				json.setUnSuccess(-1, "数据已被使用,不能删除");
				return json.jsonValue();
			}
			
			
			this.service.getMgClient().deleteByPK(id, PLanguage.class,ut,this.service);
			
			
			json.setSuccess("删除成功");
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	
	@Security(accessType = "1", displayName = "显示列表", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/fetchDisplayByLan",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject fetchDisplayByLan(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String lan=json.getSelectedId(Constants.current_sys, URI+"/fetchDisplayByLan", LableLanDisplay.class.getName(), "lan",true,this.getService());
			
			PageInfo page=json.getPageInfo(LableLanDisplay.class);
			
			LableLanDisplay search=json.getSearch(LableLanDisplay.class, null, ut,this.service);
			search.setLan(lan);
			QueryFetchInfo<LableLanDisplay> fetch=this.service.getMgClient().getFetch(search,"className,labelId", page.getCurrentPage(), page.getPageSize(),this.service);
			
			
			
			
			super.fetchToJson(fetch, json, BosConstants.getTable(LableLanDisplay.class));
			
			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "删除标签显示", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/deleteDisplay",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject deleteDisplay(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/deleteDisplay", LableLanDisplay.class.getName(), "", true,this.getService());
			
			LableLanDisplay src=this.service.getMgClient().getById(id, LableLanDisplay.class);
			if(src==null) {
				json.setUnSuccessForNoRecord(LableLanDisplay.class,id);
				return json.jsonValue();
			}
			
			
			this.service.getMgClient().deleteByPK(id, LableLanDisplay.class,ut,this.service);
			JSONObject jo=BosConstants.getExpireHash().getJSONObject(LableLanDisplay.class.getSimpleName()+"_"+src.getLabelId());
			if(jo!=null) {
				jo.remove(src.getLan());
			}
			new ClassInnerNotice().invoke(LableLanDisplay.class.getSimpleName(), src.getLabelId());
			
			
			
			
			
			json.setSuccess("删除成功");
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	@Security(accessType = "1", displayName = "显示列表", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/listForLabel",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject listForLabel(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			PLanLabel obj=json.getObj(PLanLabel.class, "labelId,className,id,property",this.service);
			BosEntity biz=this.service.getMgClient().getById(obj.getId(), obj.getClassName());
			if(biz!=null) {
				obj.setValue(biz.propertyValueString(obj.getProperty()));
			}
			super.objToJson(obj, json);
			
			
			
			QueryListInfo<LableLanDisplay> list=new QueryListInfo<LableLanDisplay>();
			
			HashMap<String,LableLanDisplay> hash=new HashMap<String,LableLanDisplay>();
			if(true) {
				LableLanDisplay s=new LableLanDisplay();
				s.setLabelId(obj.getLabelId());
				QueryListInfo<LableLanDisplay> llist=this.service.getMgClient().getList(s, "lan",this.service);
				for(LableLanDisplay x:llist.getList()) {
					hash.put(x.getLan(), x);
				}
			}
			
			
			if(true) {
				PLanguage lan=new PLanguage();
				lan.setStatus(1L);
				QueryListInfo<PLanguage> llist=this.service.getMgClient().getList(lan, "seq",this.service);
				for(PLanguage x:llist.getList()) {
					LableLanDisplay dis=hash.get(x.getLan());
					if(dis==null) {
						dis=new LableLanDisplay();
						dis.setLabelId(obj.getLabelId());
						dis.setClassName(obj.getClassName());
					}
					dis.setLan(x.getLan());
					dis.setRemark(x.getRemark());
					list.getList().add(dis);
				}
			}
			
			
			super.listToJson(list, json, BosConstants.getTable(LableLanDisplay.class));
			
			json.putSimpleData("LableLanDisplay_class", LableLanDisplay.class.getName(), "标签类名", service);
			

			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "显示列表保存", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/listForLabelSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject listForLabelSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			PLanLabel obj=json.getObj(PLanLabel.class, "labelId,className",this.service);
			
			List<LableLanDisplay> list=json.getList(LableLanDisplay.class, "lan,!display",this.service);
			for(LableLanDisplay x:list) {
				x.setLabelId(obj.getLabelId());
				x.setClassName(obj.getClassName());
				this.service.save(x, ut);
			}
			
			
			BosConstants.getExpireHash().removeJSONObject(obj.getLabelId());
 			new ClassInnerNotice().invoke(LableLanDisplay.class.getSimpleName(), obj.getLabelId());
			
			
			json.setSuccess();
			
		}catch(Exception e){
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
