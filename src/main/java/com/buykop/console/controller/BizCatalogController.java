package com.buykop.console.controller;

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
import com.buykop.console.service.CatalogService;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.BizFieldModifyTrack;
import com.buykop.framework.entity.FileBizCatalog;
import com.buykop.framework.entity.FileUpload;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.office.excel.PSheet;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PFormAction;
import com.buykop.framework.scan.PFormMember;
import com.buykop.framework.scan.PFormRowAction;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PMapTJConfig;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.Calculation;
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


@Module(display = "文件目录", sys = Constants.current_sys)
@RestController
@RequestMapping(BizCatalogController.URI)
public class BizCatalogController extends BaseController{
	
	private static Logger  logger=LoggerFactory.getLogger(BizCatalogController.class);
	
	protected static final String URI="/catalog/biz";
	
	
	private static final String TREE_BIZ_CATALOG="TREE_BIZ_CATALOG";
	
	

	@Autowired
	private CatalogService service;
	
	
	protected PTreeForm loadTree(String parentIdValue,String className,HttpEntity json,UserToken token,RdClient conn) throws Exception{
		
		PTreeForm  obj=new PTreeForm();
 		obj.setCode(TREE_BIZ_CATALOG);
 		obj.setName("业务文件目录树");
 		obj.setSys(BosConstants.current_sys);
 		obj.setClassName(FileBizCatalog.class.getName());
 		obj.setBizClassName(className);
 		obj.setRegType(0L);
 		obj.setRootId("0");
 		if(DataChange.isEmpty(obj.getRootName())) {
 			obj.setRootName("根目录");
 		}
 		if(obj.getStatus()==null) {
 			obj.setStatus(1L);
 		}
		if(DataChange.isEmpty(parentIdValue)) {
			parentIdValue="0";
		}
		
		FileBizCatalog search=new FileBizCatalog();
		search.setParentId(parentIdValue);
		search.setClassName(className);
		search.setMemberId(token.getMemberId());
		QueryListInfo<FileBizCatalog> list=this.service.getMgClient().getList(search, "seq,catalogName",this.service);
		
		SelectBidding bidding=list.getSelectBidding(this.service);
		json.getData().put("TREE",bidding.showJSON());
		obj.setRootId(parentIdValue);
		obj.setRootName("根目录");
		super.objToJson(obj, json);
		
		return obj;
	}
	
	
	
	@Security(accessType = "1,2", displayName = "目录列表", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/list",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject list(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String className=json.getSelectedId(Constants.current_sys, URI+"/bizClassName", FileBizCatalog.class.getName(), "className", true,this.service);
			
			Table table=this.service.getMgClient().getTableById(className);
			super.objToJson(table, json);
			
			String parentId=json.getSelectedId(Constants.current_sys, URI+"/list", FileBizCatalog.class.getName(), null,this.service);
			
			PTreeForm tree=this.loadTree(parentId,className, json, ut,this.service.getRdClient());

			if(DataChange.isEmpty(parentId)) {
				parentId=tree.getRootId();
			}
			
			FileBizCatalog search=json.getSearch(FileBizCatalog.class,null,ut,this.service);
			search.setParentId(parentId);
			search.setClassName(className);
			search.setMemberId(ut.getMemberId());
			QueryListInfo<FileBizCatalog> list=this.service.getMgClient().getList(search, "seq,catalogName",this.service);
			super.listToJson(list, json, BosConstants.getTable(FileBizCatalog.class));
			
			SelectBidding sb = new SelectBidding();
			sb.put("0", "--根目录--");
			
			FileBizCatalog parentx=this.getService().getById(parentId, FileBizCatalog.class);
			if(parentx!=null) {
				FileBizCatalog p=this.getService().getById(parentx.getParentId(), FileBizCatalog.class);
				if(p!=null) {
					sb.put(p.getCatalogId(),p.getCatalogName()+"[上级目录]");
				}
				sb.put(parentId, parentx.getCatalogName(), true);
				super.objToJson(parentx, "parent", json);
			}
			
			
			if(true) {
				for(FileBizCatalog x:list.getList()) {
					if(DataChange.isEmpty(x.getParentId())) {
						x.setParentId("0");
					}
					sb.put(x.getCatalogId(), x.getCatalogName());
				}
			}
			
			super.selectToJson(sb, json, FileBizCatalog.class.getSimpleName(),"parentId");
			
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("目录列表", e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	
	
	
	

	@Security(accessType = "1,2", displayName = "删除", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/delete",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String className=json.getSelectedId(Constants.current_sys, URI+"/bizClassName", FileBizCatalog.class.getName(), "className", true,this.service);
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/delete",FileBizCatalog.class.getName(),null,true,this.service);
	
			
			FileBizCatalog org=new FileBizCatalog();
			org.setParentId(id);
			org.setClassName(className);
			org.setMemberId(ut.getMemberId());
			org.setIsValid(1);
			if(this.service.getMgClient().getCount(org,this.service)>0) {
				json.setUnSuccess(-1, "因含有下级目录而不能删除");
				return json.jsonValue();
			}
			
			
			FileBizCatalog u=new FileBizCatalog();
			u.setCatalogId(id);
			if(this.service.getMgClient().getCount(u,this.service)>0) {
				json.setUnSuccess(-1, "因含有文件而不能删除");
				return json.jsonValue();
			}


			this.service.getMgClient().deleteByPK(id, FileBizCatalog.class,ut,this.service);

			
			json.getData().put("TREEREMOVE",id);
			
			
			String parentId=json.getSelectedId(Constants.current_sys, URI+"/list", FileBizCatalog.class.getName(), null,this.service);
			PTreeForm tree=this.loadTree(parentId,className, json, ut,this.service.getRdClient());
			if(DataChange.isEmpty(parentId)) {
				parentId=tree.getRootId();
			}
			
			
			json.setSuccess("删除成功");
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("删除目录", e);
		}
		
		return json.jsonValue();
		
	}
	

	
	
	@Security(accessType = "1,2", displayName = "列表保存", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/saveList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String className=json.getSelectedId(Constants.current_sys, URI+"/bizClassName", FileBizCatalog.class.getName(), "className", true,this.service);
			

			String parentId=json.getSelectedId(Constants.current_sys, URI+"/list",FileBizCatalog.class.getName(),null,this.service);
			PTreeForm tree=this.loadTree(parentId,className, json, ut,this.service.getRdClient());
			if(DataChange.isEmpty(parentId)) {
				parentId=tree.getRootId();
			}
			
			
			
			FileBizCatalog parentx=this.service.getMgClient().getById(parentId, FileBizCatalog.class);
			
			
			long seq=0;
			List<FileBizCatalog> list=json.getList(FileBizCatalog.class, "catalogId,catalogName,!parentId,!remark",this.service);
			for(FileBizCatalog x:list) {
				
				if(DataChange.isEmpty(x.getParentId())) {
					x.setParentId("0");
				}
				
				if(x.getCatalogId().equals(x.getParentId())) {
					json.setUnSuccess(-1,x.getCatalogName()+"的上级目录不能是自身");
					return json.jsonValue();
				}
				
				x.setMemberId(ut.getMemberId());
				
				x.setClassName(className);
				x.setSeq(seq++);
				x.setIsValid(1);
				this.service.save(x,ut);
			}
			
			JSONArray arr=new JSONArray();
			for(BosEntity x:list) {
				if(!x.existMust("catalogName")) continue;
				arr.add(x.getTreeJson(2));
			}
			json.getData().put("TREEMODIFY", arr);
			
			
			
			json.setSuccess("保存成功");
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("列表保存", e);
		}
		
		return json.jsonValue();
	}
			
	
	
	@Security(accessType = "0", displayName = "目录及文件列表", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/listForFile",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject listForFile(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String sys=json.getSimpleData("sys", "子系统", String.class, true,this.service);
			String simpleName=json.getSimpleData("simpleName", "对象名", String.class, true,this.service);
			String idValue=json.getSimpleData("id", "业务id", String.class, true,this.service);
			String formId=json.getSimpleData("formId", "数据模板id", String.class, false,this.service);
			String parentId=json.getSelectedId(Constants.current_sys, URI+"/listForFile", FileBizCatalog.class.getName(), null,this.service);
			
			
			
			Table table=Table.getSysTableBySimpleName(sys, simpleName,this.service);
			if(table==null) {
				json.setUnSuccess(-1, "表对象不存在");
				return json.jsonValue();
			}
			
			super.objToJson(table, json);
			json.getData().put("zipFlag", String.valueOf(DataChange.getLongValueWithDefault(table.getZipFlag(), 0)));
			
			
			if(!DataChange.isEmpty(formId)) {
				PForm form=this.service.getMgClient().getById(formId, PForm.class);
				if(form!=null) {
					super.objToJson(form, json);
				}
			}
			
			
			
			BosEntity obj=this.service.getById(idValue, table.getClassName());
			if(obj==null) {
				json.setUnSuccessForNoRecord(table.getClassName(), idValue);
				return json.jsonValue();
			}
			super.objToJson(obj, json);
			
			
			

			
			PTreeForm tree=this.loadTree(parentId,table.getClassName(), json, ut,this.service.getRdClient());
			if(DataChange.isEmpty(parentId)) {
				parentId=tree.getRootId();
			}
			
			
			HashMap<String,FileBizCatalog>  cache=new HashMap<String,FileBizCatalog>();
			
			
			SelectBidding sb = new SelectBidding();
			sb.put("0", "--根目录--");
			FileBizCatalog current=this.service.getMgClient().getById(parentId, FileBizCatalog.class);
			if(current!=null) {
				
				FileBizCatalog p=this.service.getMgClient().getById(current.getParentId(), FileBizCatalog.class);
				if(p!=null) {
					FileBizCatalog pp=FileBizCatalog.getCacheObj(cache, p.getParentId(),this.service);
					if(pp==null) {
						sb.put(p.getCatalogId(),p.getCatalogName()+"[上级目录]");
					}else {
						sb.put(p.getCatalogId(),p.getCatalogName()+"("+pp.getCatalogName()+")[上级目录]");
					}
				}
				
				FileBizCatalog pp=FileBizCatalog.getCacheObj(cache, current.getParentId(),this.service);
				if(pp==null) {
					sb.put(parentId, current.getCatalogName()+"[当前目录]", true);
				}else {
					sb.put(parentId, current.getCatalogName()+"("+pp.getCatalogName()+")[当前目录]", true);
				}
				
				super.objToJson(current, "parent", json);
			}
			
			
			if(true) {
				FileBizCatalog sub=new FileBizCatalog();
				//sub.setParentId(parentId);
				sub.setMemberId(ut.getMemberId());
				sub.setClassName(table.getClassName());
				QueryListInfo<FileBizCatalog> subList=this.service.getMgClient().getList(sub, "parentId,catalogName",this.service);
				for(FileBizCatalog x:subList.getList()) {
					if(sb.containsKey(x.getCatalogId())) continue;
					FileBizCatalog pp=FileBizCatalog.getCacheObj(cache, x.getParentId(),this.service);
					if(pp==null) {
						sb.put(x.getCatalogId(), x.getCatalogName());
					}else {
						sb.put(x.getCatalogId(), x.getCatalogName()+"("+pp.getCatalogName()+")");
					}
				}
			}
			
			
			
			
			JSONObject jo=json.getData().getJSONObject(table.getSimpleName());
			JSONArray arr=new JSONArray();
			
			Vector<String> delV=new Vector<String>();
			FileUpload search=json.getSearch(FileUpload.class,null,ut,this.service);
			if(!DataChange.isEmpty(parentId) &&  !parentId.equals("0")) {
				search.setCatalogId(parentId);
			}
			search.setClassName(table.getClassName());
			search.setIdValue(idValue);
			QueryListInfo<FileUpload>  fetch=this.service.getList(search, "catalogId,srcName");
			for(FileUpload x:fetch.getList()) {
				
				arr.add(x._getJson());
				
				if(DataChange.isEmpty(x.getCatalogId())) {
					x.setCatalogId("0");
				}
				
				
				if(sb.containsKey(x.getCatalogId())) continue;
				FileBizCatalog c=this.service.getMgClient().getById(x.getCatalogId(),FileBizCatalog.class);
				if(c!=null) {
					FileBizCatalog pp=FileBizCatalog.getCacheObj(cache, c.getParentId(),this.service);
					if(pp==null) {
						sb.put(c.getCatalogId(), c.getCatalogName());
					}else {
						sb.put(c.getCatalogId(), c.getCatalogName()+"("+pp.getCatalogName()+")");
					}
				}
			}
			
			for(String d:delV) {
				for(int i=0;i<fetch.getList().size();i++) {
					if(fetch.getList().get(i).getFileId().equals(d)) {
						fetch.getList().remove(i);
					}
				}
			}
			
			jo.put("_fileList_"+table.getSimpleName(), arr);
			
			super.listToJson(fetch, json, BosConstants.getTable(FileUpload.class));
			
			super.selectToJson(sb, json, FileUpload.class.getSimpleName(), "catalogId");
			
			
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("文件列表", e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "0", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/saveFileList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject saveFileList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			
			String sys=json.getSimpleData("sys", "子系统", String.class, true,this.service);
			String simpleName=json.getSimpleData("simpleName", "对象名", String.class, true,this.service);
			String idValue=json.getSimpleData("id", "业务id", String.class, true,this.service);
			Vector<String>  ids=json.showIds();
			
			
			Table table=Table.getSysTableBySimpleName(sys, simpleName,this.service);
			if(table==null) {
				json.setUnSuccess(-1, "表对象不存在");
				return json.jsonValue();
			}
			
			String parentId=json.getSelectedId(Constants.current_sys, URI+"/listForFile", FileBizCatalog.class.getName(), null,this.service);
			FileBizCatalog parentx=this.service.getMgClient().getById(parentId, FileBizCatalog.class);
			if(parentx!=null) {
				
				
			}
			
			List<FileUpload> list=json.getList(FileUpload.class, "fileId,!catalogId",this.service);
			for(FileUpload x:list) {
				x.setClassName(table.getClassName());
				x.setIdValue(idValue);

				if(DataChange.isEmpty(x.getCatalogId()) || x.getCatalogId().equals("0")) {
					x.setCatalogId("");
				}
				
				x.setClassName(table.getClassName());
				x.setIdValue(idValue);
				this.service.save(x,ut);
			}
			
		
			
			json.setSuccess("保存成功");
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("文件列表保存", e);
		}
		
		return json.jsonValue();
	}
	
	

	
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}
	
	
	
	
}