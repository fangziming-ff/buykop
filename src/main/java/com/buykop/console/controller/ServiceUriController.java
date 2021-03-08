package com.buykop.console.controller;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.ServiceUriService;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRRoleFun;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PServiceParam;
import com.buykop.framework.scan.PServiceParamField;
import com.buykop.framework.scan.PServiceUri;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.query.BaseQuery;
import com.buykop.framework.util.sort.ServiceParamComparator;
import com.buykop.framework.util.sort.ServiceUriComparator;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;



@Module(display = "模块", sys = Constants.current_sys)
@RestController
@RequestMapping("/module")
public class ServiceUriController extends BaseController{
	
	private static Logger  logger=LoggerFactory.getLogger(ServiceUriController.class);
	
	
	@Autowired
	private ServiceUriService service;
	
	
	
	
	@Security(accessType = "1,2", displayName = "子系统的菜单列表", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/menuListForRoot",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject menuListForRoot(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String sys=json.getSelectedId(Constants.current_sys, "/module/menuListForRoot",PServiceUri.class.getName(),null,true,this.getService());

			
			PServiceUri search=json.getSearch(PServiceUri.class,null,ut,this.service);
			search.setSys(sys);
			search.setIsMenu(1L);
			QueryListInfo<PServiceUri> list=this.service.getMgClient().getList(search, "menu",this.service);
			
			
			Collections.sort(list.getList(), new ServiceUriComparator());
			super.listToJson(list,json,BosConstants.getTable(PServiceUri.class.getName()));
			

			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
			
		return json.jsonValue();	
		
	}
	
	
	
	@Security(accessType = "1,2", displayName = "角色服务列表", needLogin = true, isEntAdmin = true, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/listForRoleSet",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject listForRoleSet(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		

		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String roleId=json.getSelectedId(Constants.current_sys, "/module/listForRoleSet",PRole.class.getName(),null,true,this.getService());
			
			
			PRole obj=this.service.getById(roleId, PRole.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PRole.class);
				return json.jsonValue();
			}
			
			
			super.objToJson(obj,json);
			
			
			
			PRRoleFun rrf=new PRRoleFun();
			rrf.setRoleId(roleId);
			QueryListInfo<PRRoleFun> rrfList=this.service.getMgClient().getList(rrf,this.service);
			Vector<String> fv=new Vector<String>();
			for(PRRoleFun f:rrfList.getList()) {
				fv.add(f.getFunCode());
			}
			
			
			Vector<PServiceUri> delV=new Vector<PServiceUri>();
			
			
			PServiceUri search=json.getSearch(PServiceUri.class,null,ut,this.service);
			search.setNeedLogin(1L);
			if(obj.getSys().equals(BosConstants.current_sys) ||  obj.getSys().equals(BosConstants.current_sys_console)) {
				BaseQuery bq=new BaseQuery();
				bq.setFields("sys");
				bq.setType(1);
				bq.setValue(BosConstants.current_sys+","+BosConstants.current_sys_console);
				search.getQueryList().add(bq);
			}else {
				search.setSys(obj.getSys());
			}
			
			search.setStatus(1l);
			QueryListInfo<PServiceUri> list=this.service.getMgClient().getList(search, "uri",this.service);
			
			for(PServiceUri su:list.getList()) {
				
				Vector<String> v=MyString.splitBy(su.getAccessType(), ",");
				
				boolean flag=true;
				for(String x:v) {
					if(!x.endsWith("*")) flag=false;
				}
				
				if(flag) {
					delV.add(su);
					continue;
				}
				
				if(v.contains("0")) {
					delV.add(su);
					continue;
				}
				
				
				if(v.contains("-1")) {
					delV.add(su);
					continue;
				}
				
				
				if(v.size()==1 && (v.contains("2*") || v.contains("1*")) ) {
					delV.add(su);
					continue;
				}
				
				
				if(fv.contains(su.getPk())) {
					su.setSelected("checked");
				}
			}
			
			for(PServiceUri ps:delV) {
				list.getList().remove(ps);
			}
			
			
			//进行排序
			Collections.sort(list.getList(), new ServiceUriComparator());
			super.listToJson(list,json,BosConstants.getTable(PServiceUri.class.getName()));
			
			PRoot root=new PRoot();
			root.setStatus(1L);
			super.selectToJson(this.service.getMgClient().getList(root,this.service).getSelectBidding(this.service), json, PServiceUri.class.getSimpleName(), "sys");
			
			

			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
			
		return json.jsonValue();	
			
	}
	
	
	
	
	@Security(accessType = "1", displayName = "角色服务保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/listForRoleSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject listForRoleSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			
			//PServiceUri search=json.getSearch(PServiceUri.class,null,ut);
			
			String roleId=json.getSelectedId(Constants.current_sys, "/module/listForRoleSet",PRole.class.getName(),null,true,this.getService());
			PRole obj=this.service.getById(roleId, PRole.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PRole.class);
				return json.jsonValue();
			}
			

			
			PRRoleFun rrf=new PRRoleFun();
			rrf.setRoleId(roleId);
			this.service.getMgClient().delete(rrf,ut,this.service);
			
			
			
			//保存信息 ,分隔
			Vector<String> ids = json.showIds();
			
			BosConstants.debug("roleId="+roleId+"  ids="+ids);
			
			for(String per:ids) {
				rrf=new PRRoleFun();
				rrf.setRoleId(roleId);
				rrf.setFunCode(per);
				this.service.save(rrf,ut);
			}
				
			
			
			
			super.objToJson(obj,json);
			

			json.setSuccess("设置成功");
			

			return this.listForRoleSet(json,request);
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
			
			
	}
	
	
	
	
	
	
			
	
	
	@Security(accessType = "1,2", displayName = "服务列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/serviceFetch",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject serviceFetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		//Constants.debug("***************1\n"+json.stringValue());
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String sys=json.getSelectedId(Constants.current_sys, "/module/serviceFetch",PRoot.class.getName(),null,this.getService());

			PRoot root=this.service.getMgClient().getById(sys, PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,sys);
				return json.jsonValue();
			}
			
			PServiceUri search=json.getSearch(PServiceUri.class,null,ut,this.service);
			
			
			search.setSys(sys);
			if(search.getIsMenu()!=null && search.getIsMenu().intValue()==1) {
				QueryListInfo<PServiceUri> list=this.service.getMgClient().getList(search,"seq,menu",this.service);
				super.listToJson(list, json, BosConstants.getTable(PServiceUri.class.getName()));
			}else {
				PageInfo page=json.getPageInfo(PServiceUri.class);
				QueryFetchInfo<PServiceUri> fetch=this.service.getMgClient().getFetch(search,"uri", page.getCurrentPage(), page.getPageSize(),this.service);
				//排查test
				super.fetchToJson(fetch,json,BosConstants.getTable(PServiceUri.class.getName()));
				
			}
			

			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		
		
		
		
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "服务列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/serviceFetchSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject serviceFetchSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		//Constants.debug("***************1\n"+json.stringValue());
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String sys=json.getSelectedId(Constants.current_sys, "/module/serviceFetch",PRoot.class.getName(),null,this.getService());
			

			PRoot root=this.service.getMgClient().getById(sys, PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,sys);
				return json.jsonValue();
			}
			
			
			
			long seq=0;
			List<PServiceUri> list=json.getList(PServiceUri.class, "sys,uri,module,displayName,accessType,needLogin,isEntAdmin,isSysAdmin",this.service);
			for(PServiceUri x:list) {
				if(x.getRefreshTime()==null) {
					x.setRefreshTime(NetWorkTime.getCurrentDatetime());
				}
				if(x.getNeedLogin()==0) {
					x.setIsEntAdmin(0L);
					x.setIsSysAdmin(0L);
					x.setAccessType("0");
				}
				
				if(!x.getUri().startsWith("/")) {
					json.setUnSuccess(-1, "URI格式有误");
					return json.jsonValue();
				}
				Vector<String> v=MyString.splitBy(x.getAccessType(), ",");
				v.remove("0");
				v.remove("1");
				v.remove("2");
				v.remove("-1");
				v.remove("1*");
				v.remove("2*");
				if(v.size()>0) {
					json.setUnSuccess(-1, "accessType="+x.getAccessType()+LabelDisplay.get(" 格式有误,只允许 -1,0,1,2,1*,2*", json.getLan()) ,true);
					return json.jsonValue();
				}
				x.setSeq(seq++);
				this.service.save(x,ut);
				
			}
			

			json.setSuccess("保存成功");
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		
		
		
		
		return json.jsonValue();
	}
	
	

	@Security(accessType = "1,2", displayName = "删除服务", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/delete",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, "/module/delete",PServiceUri.class.getName(),null,true,this.getService());
			PServiceUri obj=this.service.getMgClient().getById(id, PServiceUri.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PServiceUri.class);
				return json.jsonValue();
			}
			
			if(obj.getRegType()!=null && obj.getRegType().intValue()==0) {
				json.setUnSuccess(-1, "自动注册的服务不能删除");
			}
			
			PRoot root=this.service.getMgClient().getById(obj.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,obj.getSys());
				return json.jsonValue();
			}
			
			//机构类型 PRMemberTypeFun
			
			//角色  PRRoleFun
			PRRoleFun rrf=new PRRoleFun();
			rrf.setFunCode(id);
			long count=this.service.getMgClient().getCount(rrf,this.service);
			if(count>0) {
				json.setUnSuccess(-1, "该URI已与角色关联,不能删除");
				return json.jsonValue();
			}
			
			
			this.service.getMgClient().deleteByPK(id, PServiceUri.class,ut,this.service);
			
			
			json.setSuccess("删除成功");
			
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1,2", displayName = "保存服务", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/save",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject save(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, "/module/info",PServiceUri.class.getName(),null,true,this.getService());
			
			
			PServiceUri obj=json.getObj(PServiceUri.class,"sys,uri",this.service);
			obj.setPK(id);
			
			PRoot root=this.service.getMgClient().getById(obj.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,obj.getSys());
				return json.jsonValue();
			}
			
			this.service.save(obj,ut);
			
			
			json.setSuccess("保存成功");
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
			
		}
		
		return json.jsonValue();
		
	}
	
	
	
	
	@Security(accessType = "1,2", displayName = "保存服务的菜单信息", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/saveMenu",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject saveMenu(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			PServiceUri obj=json.getObj(PServiceUri.class,"sys,uri,menu,menuJs,!script",this.service);
			
			
			PRoot root=this.service.getMgClient().getById(obj.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,obj.getSys());
				return json.jsonValue();
			}
			

			if(obj.getMenu().indexOf("|")==-1) {
				json.setUnSuccess(-1, "菜单格式有误,请用|分割，并且是二级菜单格式");
				return json.jsonValue();
			}
			
			
			
			int x=MyString.count(obj.getMenu(),"|");
			if(x!=1) {
				json.setUnSuccess(-1, "菜单格式有误,请用|分割，并且是二级菜单格式");
				return json.jsonValue();
			}
			
			this.service.save(obj,ut);
			
			


			json.setSuccess("保存成功");
			
			
				
		}catch(Exception e){
			json.setUnSuccess(e);
			
		}
		
		return json.jsonValue();
	}
	
	
	@Security(accessType = "1,2", displayName = "服务详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/info",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			//String sys=json.getSelectedId(Constants.current_sys, "/module/serviceList",PRoot.class,null);
			
			String id=json.getSelectedId(Constants.current_sys, "/module/info",PServiceUri.class.getName(),null,true,this.getService());
			
			PServiceUri uri=this.service.getMgClient().getById(id, PServiceUri.class);
			if(uri==null) {
				json.setUnSuccessForNoRecord(PServiceUri.class);
				return json.jsonValue();
			}
			
			
			PRoot root=this.service.getMgClient().getById(uri.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,uri.getSys());
				return json.jsonValue();
			}
			
			
		
			super.objToJson(uri,json);
			

			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		
		
		
		
		
		return json.jsonValue();
	}
	
	
	
	
	//@Menu(name = "服务查询", trunk = "基础信息", js = "service")
	@Security(accessType = "1,2", displayName = "服务列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/serviceList",method = RequestMethod.POST)
	@ResponseBody
	public JSONObject serviceList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		//Constants.debug("***************1\n"+json.stringValue());
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String sys=json.getSelectedId(Constants.current_sys, "/module/serviceList",PRoot.class.getName(),null,this.getService());
			
			PRoot root=this.service.getMgClient().getById(sys, PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,sys);
				return json.jsonValue();
			}
			
			
			
			//Vector<String> sysV=new Vector<String>();
			//PRoot pr=new PRoot();
			//pr.setStatus(1l);
			//QueryListInfo<PRoot> prList=this.service.getMgClient().getList(pr);
			//for(PRoot t:prList.getList()) {
				//sysV.add(t.getCode());
			//}
			
			Vector<PServiceUri> delV=new Vector<PServiceUri>();
			PServiceUri search=json.getSearch(PServiceUri.class,null,ut,this.service);
			search.setSys(sys);
			search.setStatus(1L);
			//search.setMemberId(ut.getMemberId());
			QueryListInfo<PServiceUri> list=this.service.getMgClient().getList(search,this.service);
			//排查test
	
			for(PServiceUri t:list.getList()) {
				if(DataChange.isEmpty(t.getMethod())) continue;
				if(t.getMethod().equals("test")) {
					delV.add(t);
				}
				//if(DataChange.isEmpty(t.getSys())) {
					//delV.add(t);
				//}else if(!sysV.contains(t.getSys())) {
				//	delV.add(t);
				//}
			}
				
			for(PServiceUri t:delV) {
				list.getList().remove(t);
			}
			
			Collections.sort(list.getList(), new ServiceUriComparator());
			super.listToJson(list,json,BosConstants.getTable(PServiceUri.class.getName()));
			
	
			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		
		
		
		
		return json.jsonValue();
	}



	@Security(accessType = "1,2", displayName = "开发帮助", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/help",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject help(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, "/module/help",PServiceUri.class.getName(),null,true,this.getService());

			
			PServiceUri uri=this.service.getMgClient().getById(id, PServiceUri.class);
			if(uri==null) {
				json.setUnSuccessForNoRecord(PServiceUri.class);
				return json.jsonValue();
			}
			
			
			
			PRoot root=this.service.getMgClient().getById(uri.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,uri.getSys());
				return json.jsonValue();
			}
			

			
			JSONObject inDemo=json.jsonValue();
			inDemo.put("sys", uri.getSys());
			inDemo.put("uri", uri.getUri());
			inDemo.put("data", new JSONObject(true));
			
			
			
			JSONObject outDemo=json.jsonValue();
			outDemo.put("sys", uri.getSys());
			outDemo.put("uri", uri.getUri());
			outDemo.put("data", new JSONObject(true));
			
			
			
			
			super.objToJson(uri, json);
			
			
			
			//Constants.debug("--------------------------------------------------");
			
			
			if(BosConstants.devMode()) {
				
				//模拟访问一下
				HttpEntity test=new HttpEntity();
				test.setSys(uri.getSys());
				test.setUri(uri.getUri());
				Vector<String> v=MyString.splitBy(uri.getAccessType(), ",");
				if(v.contains("1")) {
					test.setTokenKey("admin");
				}else if(v.contains("2")) {
					test.setTokenKey("member");
				}else if(v.contains("-1")) {
					test.setTokenKey("user");
				}else {
					test.setTokenKey("visitor");
				}
					
			}
			
			
			if(true) {
				PServiceParam search=new PServiceParam();
				search.setSys(uri.getSys());
				QueryListInfo<PServiceParam> list=this.service.getMgClient().getList(search,this.service);
				//SysConstants.debug("--size="+Pool.getInstance().getConn().getList(search).getList().size()+"-----x-");
				for(PServiceParam p:list.getList()) {
					BosConstants.debug("sys="+p.getSys()+"  uri="+p.getUri()+"   key="+p.getKey()+"  type="+p.getType()+"    io="+p.getInOut());
				}
				
			}
			
			
			
			PServiceParam search=new PServiceParam();
			search.setSys(uri.getSys());
			search.setUri(uri.getUri());
			QueryListInfo<PServiceParam> list=this.service.getMgClient().getList(search,this.service);
			BosConstants.debug("sys="+uri.getSys()+"  uri="+uri.getUri()+"-----size1="+list.getList().size()+"-----x-");
			
			
			Collections.sort(list.getList(), new ServiceParamComparator());
			
			for(PServiceParam p:list.getList()) {
				
				//Constants.debug("key="+p.getKey()+"  dis="+p._getDisplay(conn)+"   io="+p.getInOut()+"    type="+p.getType()+"   class="+p.getClassName());
				
				
				if(p.getType().longValue()==3) continue;
				
				if(DataChange.isEmpty(p.getClassName())) continue;
				
				Table table = BosConstants.getTable(p.getClassName());
				
				//-1:简单数据类型   0:对象     1:列表       2:翻页列表    3:图表
				if(p.getType().longValue()==-1) {//简单数据类型
					if(p.getInOut().longValue()==0) {//输入
						inDemo.getJSONObject("data").put(p.getKey(), p.getDisplay());
					}else {
						outDemo.getJSONObject("data").put(p.getKey(), p.getDisplay());
					}
				}else if(p.getType().longValue()==0) {//对象
					if(table==null) continue;
					
					JSONObject js=table.showJsonForHelp(p.getFields(),json.getLan());
					
					if(p.getKey().equals(table.getClassName().substring(table.getClassName().lastIndexOf(".")+1, table.getClassName().length())+"Search")) {
						if(DataChange.isEmpty(p.getFields())) {
							js=new JSONObject(true);
						}
					}
					p.setHelpJson(js);
					if(p.getInOut().longValue()==0) {//输入
						inDemo.getJSONObject("data").put(p.getKey(),js);
					}else {
						outDemo.getJSONObject("data").put(p.getKey(), js);
					}
					
					
				}else if(p.getType().longValue()==1) {//列表
					if(table==null) continue;
					p.setHelpJson(table.showJsonForHelp(p.getFields(),json.getLan()));
					
					JSONArray array=new JSONArray();
					array.add(table.showJsonForHelp(p.getFields(),json.getLan()));
					if(p.getInOut().longValue()==0) {//输入
						inDemo.getJSONObject("data").put(p.getKey(),array);
					}else {
						outDemo.getJSONObject("data").put(p.getKey(), array);
					}
					
				}else if(p.getType().longValue()==2) {//翻页
					if(table==null) continue;
					p.setHelpJson(table.showJsonForHelp(p.getFields(),json.getLan()));
					
					JSONArray array=new JSONArray();
					array.add(table.showJsonForHelp(p.getFields(),json.getLan()));
					
					if(p.getInOut().longValue()==1) {//输出
						//inDemo.getJSONObject("data").put(p.getKey()+"Search",array);
						//inDemo.getJSONObject("data").put("pageSize_"+p.getKey(), "每页的记录数");
						//inDemo.getJSONObject("data").put("currentPage_"+p.getKey(), "当前页数");
						outDemo.getJSONObject("data").put(p.getKey(), array);
						JSONObject page=new JSONObject(true);
						page.put("total","总记录数");
						page.put("pages","翻页字符串");
						page.put("pageSize","每页记录数");
						page.put("currentPage","当前页");
						page.put("maxPage","最大页");
						outDemo.getJSONObject("data").put("page_"+p.getKey(), page);
					}
					
				}	
			}
			
			
			json.getData().put("JSONIN", inDemo);
			json.getData().put("JSONOUT", outDemo);
			
			
			
			super.listToJson(list, json, BosConstants.getTable(PServiceParam.class));
			
			
			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		
		return json.jsonValue();
	}
	
	
	
	//-----------------------------------
	@Security(accessType = "1,2", displayName = "参数列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/paramFieldList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject paramFieldList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		//根据某个服务获取所有的类,每个类下的所有服务
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			//PServiceParamField
			
			String id=json.getSelectedId(Constants.current_sys, "/module/paramFieldList", PServiceParam.class.getName(), null, true,this.getService());
			
			PServiceParam param=this.service.getMgClient().getById(id, PServiceParam.class);
			if(param==null) {
				json.setUnSuccessForNoRecord(PServiceParam.class);
				return json.jsonValue();
			}
			
			PRoot root=this.service.getMgClient().getById(param.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,param.getSys());
				return json.jsonValue();
			}
			
			
			super.objToJson(param, json);
			
			if(DataChange.isEmpty(param.getClassName())) {
				json.setUnSuccess(-1, "参数类为空");
				return json.jsonValue();
			}
			
			if(Table.judgeSimpleClass(param.getClassName())) {
				json.setUnSuccess(-1, LabelDisplay.get("参数类:", json.getLan()) +param.getClassName()+LabelDisplay.get("不是表单对象,无须设置输入初始化", json.getLan()) ,true);
				return json.jsonValue();
			}
			
			
			if(param.getType().intValue()==4 ||  param.getType().intValue()==-1) {
				json.setUnSuccess(-1,LabelDisplay.get("参数类:", json.getLan()) +param.getClassName()+LabelDisplay.get(" 该类型，无须设置输入初始化", json.getLan()) ,true);
				return json.jsonValue();
			}
			
			
			if(param.getInOut().longValue()==1) {
				json.setUnSuccess(-1,LabelDisplay.get( "参数类:", json.getLan())+param.getClassName()+LabelDisplay.get(",输出参数无须设置输入初始化", json.getLan()),true );
				return json.jsonValue();
			}
			
			Table table=this.service.getMgClient().getTableById(param.getClassName());
			if(table==null) {
				json.setUnSuccess(-1, LabelDisplay.get("参数类:", json.getLan()) +param.getClassName()+LabelDisplay.get("的表单对象不存在", json.getLan()) ,true);
				return json.jsonValue();
			}
			
			
			
			if(DataChange.isEmpty(param.getFields())) {
				param.initField(table);
			}
			
			
			
			QueryListInfo<PServiceParamField> list=new QueryListInfo<PServiceParamField>();
			
			HashMap<String,PServiceParamField> fHash=new HashMap<String,PServiceParamField>();
			PServiceParamField search=new PServiceParamField(param);
			QueryListInfo<PServiceParamField> list1=this.service.getMgClient().getList(search, "property",this.service);

			BosConstants.debug(list1);
			for(PServiceParamField f:list1.getList()) {
				fHash.put(f.getProperty(), f);
			}

			
			param.initField(table);
			
			List<Field> fList=Table.getFieldsForInputSetFromMongo(table.getClassName(), param.getFields(),this.service);
			
			
			
			for(Field f:fList) {
				PServiceParamField x=new PServiceParamField(param);
				x.setProperty(f.getProperty());
				x.setDisplay(f.getDisplay());
				PServiceParamField pspf=fHash.get(f.getProperty());
				if(pspf!=null) {
					x.setInitValue(pspf.getInitValue());
					x.setIsNotNull(pspf.getIsNotNull());
				}
				x.setRegType(param.getRegType());
				list.getList().add(x);
			}
			
			
			
			super.listToJson(list, json, BosConstants.getTable(PServiceParamField.class));
			
			
			json.setSuccess();
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1,2", displayName = "参数列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/paramFieldListSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject paramFieldListSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		//根据某个服务获取所有的类,每个类下的所有服务
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			//PServiceParamField
			
			String id=json.getSelectedId(Constants.current_sys, "/module/paramFieldList", PServiceParam.class.getName(), null, true,this.getService());
			
			PServiceParam param=this.service.getMgClient().getById(id, PServiceParam.class);
			if(param==null) {
				json.setUnSuccessForNoRecord(PServiceParam.class);
				return json.jsonValue();
			}
			
			
			PRoot root=this.service.getMgClient().getById(param.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,param.getSys());
				return json.jsonValue();
			}
			
			
			if(param.getRegType().longValue()==0) {
				json.setUnSuccess(-1, "自动注册的参数不能保存");
				return json.jsonValue();
			}
			
			
			if(DataChange.isEmpty(param.getClassName())) {
				json.setUnSuccess(-1, "参数类为空");
				return json.jsonValue();
			}
			
			if(Table.judgeSimpleClass(param.getClassName())) {
				json.setUnSuccess(-1, LabelDisplay.get("参数类:", json.getLan()) +param.getClassName()+LabelDisplay.get("不是表单对象,无须设置输入初始化", json.getLan()) ,true);
				return json.jsonValue();
			}
			
			
			Table table=this.service.getMgClient().getTableById(param.getClassName());
			if(table==null) {
				json.setUnSuccess(-1,LabelDisplay.get("参数类:", json.getLan()) +param.getClassName()+LabelDisplay.get("的表单对象不存在", json.getLan()) ,true);
				return json.jsonValue();
			}
			
			
			
			List<PServiceParamField> list=json.getList(PServiceParamField.class, "property,!initValue,!isNotNull",this.service);
			
			BosConstants.debug("------------paramFieldListSave  size="+list.size()+"------------");
			
			for(PServiceParamField x:list) {
				x.init(param);
				if(DataChange.isEmpty(x.getInitValue())) {
					this.service.getMgClient().deleteByPK(x.getPk(),PServiceParamField.class,ut,this.service);
				}else {
					this.service.save(x,ut);
				}
			}
			
			
			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	
	@Security(accessType = "1,2", displayName = "改变注册方式", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/changeRegType",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject changeRegType(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		//根据某个服务获取所有的类,每个类下的所有服务
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, "/module/changeRegType", PServiceUri.class.getName(), null, true,this.getService());
			
			PServiceUri obj=this.service.getMgClient().getById(id, PServiceUri.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PServiceUri.class);
				return json.jsonValue();
			}
			
			
			PRoot root=this.service.getMgClient().getById(obj.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,obj.getSys());
				return json.jsonValue();
			}
			
			
			if(obj.getRegType()==null) {
				obj.setRegType(0L);
			}
			
			
			if(obj.getRegType().intValue()==0) {
				obj.setRegType(1L);
			}else {
				obj.setRegType(0L);
			}
			
			this.service.save(obj,ut);

			
			json.setSuccess("操作成功");
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	//行为  
	@Security(accessType = "1,2", displayName = "行为列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/actionList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject actionList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		//根据某个服务获取所有的类,每个类下的所有服务
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, "/module/actionList", PServiceUri.class.getName(), null, true,this.getService());
			
			PServiceUri obj=this.service.getMgClient().getById(id, PServiceUri.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PServiceUri.class);
				return json.jsonValue();
			}
			
			
			PRoot root=this.service.getMgClient().getById(obj.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,obj.getSys());
				return json.jsonValue();
			}
			
			
			super.objToJson(obj, json);
			
			json.setSuccess();
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	
	//------------------------------------------
	@Security(accessType = "1,2", displayName = "参数列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/paramList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject paramList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		//根据某个服务获取所有的类,每个类下的所有服务
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, "/module/paramList", PServiceUri.class.getName(), null, true,this.getService());
			
			
			
			
			PServiceUri obj=this.service.getMgClient().getById(id, PServiceUri.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PServiceUri.class);
				return json.jsonValue();
			}
			
			
			PRoot root=this.service.getMgClient().getById(obj.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,obj.getSys());
				return json.jsonValue();
			}	
			
			super.objToJson(obj, json);
			
			
			PServiceParam search=new PServiceParam();
			search.setSys(obj.getSys());
			search.setUri(obj.getUri());
			
			QueryListInfo<PServiceParam> list=this.service.getMgClient().getList(search, "inOut,type,key",this.service);
			for(PServiceParam p:list.getList()) {
				if(DataChange.isEmpty(p.getClassName())) continue;
				if(!Table.judgeSimpleClass(p.getClassName())) {
					Table t=this.service.getMgClient().getTableById(p.getClassName());
					if(t!=null) {
						p.setClassSys(t.getSys());
					}
				}
			}
			super.listToJson(list, json, BosConstants.getTable(PServiceParam.class));
			
			
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, PServiceParam.class, "classSys");
			
			
			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1,2", displayName = "参数详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/paramInfo",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject paramInfo(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		//根据某个服务获取所有的类,每个类下的所有服务
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, "/module/paramInfo", PServiceParam.class.getName(), null, true,this.getService());
			
			PServiceParam obj=this.service.getMgClient().getById(id, PServiceParam.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PServiceUri.class);
				return json.jsonValue();
			}
			
			PRoot root=this.service.getMgClient().getById(obj.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,obj.getSys());
				return json.jsonValue();
			}

			super.selectToJson(PRoot.getJsonForSelect(this.service), json, PServiceParam.class, "classSys");
			
			if(!DataChange.isEmpty(obj.getClassName())) {
				Table table=this.service.getMgClient().getTableById(obj.getClassName());
				if(table!=null) {
					obj.setClassSys(table.getSys());
				}
			}
			
			
			if(!DataChange.isEmpty(obj.getClassSys())) {
				super.selectToJson(Table.getJsonForSelect(obj.getClassSys(),0L,this.service), json, PServiceParam.class, "className");
			}else {
				super.selectToJson(Table.getSimpleJsonForSelect(), json, PServiceParam.class, "className");
			}
			
			
			
			super.objToJson(obj, json);
			
			
			
			PServiceParamField fs=new PServiceParamField(obj);
			QueryListInfo<PServiceParamField> fsList=this.service.getMgClient().getList(fs, "property",this.service);
			super.listToJson(fsList, json, BosConstants.getTable(PServiceParamField.class));
			for(PServiceParamField f:fsList.getList()) {
				Field field=new Field();
				field.setClassName(obj.getClassName());
				field.setProperty(f.getProperty());
				field=this.service.getMgClient().getById(field.getPk(),Field.class);
				if(field!=null) {
					f.setDisplay(field.getDisplay());
				}
			}
			
			
			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	
	@Security(accessType = "1,2", displayName = "删除服务下的某个参数", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/paramDelete",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject paramDelete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		//根据某个服务获取所有的类,每个类下的所有服务

		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, "/module/paramDelete", PServiceParam.class.getName(), null, true,this.getService());
			
			PServiceParam obj=this.service.getMgClient().getById(id, PServiceParam.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PServiceUri.class);
				return json.jsonValue();
			}

			
			PRoot root=this.service.getMgClient().getById(obj.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,obj.getSys());
				return json.jsonValue();
			}
			
			this.service.getMgClient().deleteByPK(id,PServiceParam.class,ut,this.service);
			

			json.setSuccess("删除成功");
			
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
