package com.buykop.console.controller;

import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.ConfigService;
import com.buykop.framework.aliyun.POSSConfig;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.FileUpload;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.mail.MailConfig;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PRootImportRecord;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.sort.PFileUploadComparatorByCreateDate;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.ConfigUtil;
import com.buykop.console.util.Constants;

@Module(display = "配置", sys = Constants.current_sys)
@RestController
@RequestMapping(ConfigController.URI)
public class ConfigController extends BaseController{
	
	
	private static Logger  logger=LoggerFactory.getLogger(ConfigController.class);
	
	protected static final String URI="/config";
	
	@Autowired
	private ConfigService  service;
	
	@Menu(name = "云配置", trunk = "基础信息,配置管理", js = "aliyun")
	@Security(accessType = "1", displayName = "阿里云配置信息", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/aliyunList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject aliyunList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			Vector<String> v=new Vector<String>();
			
			POSSConfig search=json.getSearch(POSSConfig.class, null, ut, service);
			QueryListInfo<POSSConfig> list=this.service.getList(search, "name");
			
			
			
			
			super.listToJson(list, json, BosConstants.getTable(POSSConfig.class));
			
			json.setSuccess();
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "阿里云配置信息", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/aliyunInfo",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject aliyunInfo(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/aliyunInfo", POSSConfig.class.getName(), null, true, service);
			
			POSSConfig obj=this.service.getMgClient().getById(id, POSSConfig.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(POSSConfig.class, id);
				return json.jsonValue();
			}
			
			super.objToJson(obj, json);
			
			
			json.setSuccess();
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "阿里云保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/aliyunSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject aliyunSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			

			
			UserToken ut = super.securityCheck(json,request);
			
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/aliyunInfo", POSSConfig.class.getName(), null, true, service);
			
			
			POSSConfig obj=json.getObj(POSSConfig.class,null,this.service);
			if(obj==null) {
				obj=new POSSConfig();
			}
			obj.setId(id);
			this.service.save(obj,ut);
			
			
			obj=this.service.getMgClient().getById(id, POSSConfig.class);
			
			BosConstants.getExpireHash().putObj(POSSConfig.class.getName(), id, obj);

			new ClassInnerNotice().invoke(POSSConfig.class.getName(), id);

			super.objToJson(obj, json);
			

			json.setSuccess("保存成功");
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}

	
	
	@Security(accessType = "1", displayName = "导入记录", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/importList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject importList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		
		try {
			
			
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			PRootImportRecord search=json.getSearch(PRootImportRecord.class, null, ut,this.service);
			PageInfo page=json.getPageInfo(PRootImportRecord.class);
			QueryFetchInfo<PRootImportRecord> fetch=this.service.getMgClient().getFetch(search, "sys,!importTime,!fileName", page.getCurrentPage(), page.getPageSize(),this.service);
			super.fetchToJson(fetch, json, BosConstants.getTable(PRootImportRecord.class));
			
			super.selectToJson(PRoot.getJsonForRun(ut.getMemberId(),1L,this.service), json, PRootImportRecord.class, "sys");
			
			json.setSuccess();
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
		}
		
		return json.jsonValue();
	}

	
	@Security(accessType = "1", displayName = "备份配置", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/exportJson",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject exportJson(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			PRoot root=new PRoot();
			root.setStatus(1L);
			QueryListInfo<PRoot> list=this.service.getMgClient().getList(root,this.service);
			for(PRoot x:list.getList()) {
				ConfigUtil.saveToJson(x.getCode(),this.service);
			}
			
		
			json.setSuccess("备份成功");
			
			
		}catch(Exception e){
			logger.error("导出配置", e);
			json.setUnSuccess(e);
			
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "导入配置文件列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/importJsonList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject importJsonList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String sys=json.getSelectedId(Constants.current_sys, URI+"/importJsonList",PRoot.class.getName(),null,true,this.getService());	
			
			FileUpload search=json.getSearch(FileUpload.class, null, ut,this.service);
			
			if(search.getDate()==null) {
				search.setDate(NetWorkTime.getCurrentDate());
			}
			
			QueryListInfo<FileUpload> list=new QueryListInfo<FileUpload>();
			
			
			String path=System.getProperty("user.dir")+File.separator+"config";
			
			

			File dir=new File(path);
			if(!dir.exists()) {
				dir.mkdirs();
			}
			

			
			path=path+File.separator+"export";
			dir=new File(path);
			if(!dir.exists()) {
				dir.mkdirs();
			}
			
			
			
			path=path+File.separator+sys;
			dir=new File(path);
			if(!dir.exists()) {
				dir.mkdirs();
			}
			
			
			path=path+File.separator+DataChange.dateToStr(search.getDate());
			dir=new File(path);
			if(!dir.exists()) {
				dir.mkdirs();
				json.setSuccess(LabelDisplay.get("不存在目录:", json.getLan())+dir.getPath());
				super.listToJson(list, json, FileUpload.class.getSimpleName(), "fileId,srcName,date,fileSize", BosConstants.getTable(FileUpload.class));
				return json.jsonValue();
			}
			
			
			Calendar cal = Calendar.getInstance();  

			
			File[] fl=dir.listFiles();
			for(File x:fl) {
				if(x.isDirectory()) continue;
				if(x.getName().endsWith(".json")) {
					FileUpload file=new FileUpload();
					file.setSrcName(x.getName());
					file.setPath(sys);
					cal.setTimeInMillis(x.lastModified());
					file.setDate(cal.getTime());
					file.setCreateTime(file.getDate());
					file.setFileId(sys+"|"+DataChange.dateToStr(search.getDate())+"|"+x.getName());
					file.setFileSize(x.length());
					list.getList().add(file);
				}
			}
			
			
			Collections.sort(list.getList(), new PFileUploadComparatorByCreateDate());
			
			
			super.listToJson(list, json, FileUpload.class.getSimpleName(), "fileId,srcName,date,fileSize", BosConstants.getTable(FileUpload.class));

			json.setSuccess();
			
			
		}catch(Exception e){
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "通过选择json文件导入配置", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/importJsonBySelectFile",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject importJsonBySelectFile(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/importJsonBySelectFile",FileUpload.class.getName(),null,true,this.getService());	
			String sys=id.substring(0, id.indexOf("|"));
			String date=id.substring(id.indexOf("|")+1, id.lastIndexOf("|"));
			String name=id.substring(id.lastIndexOf("|")+1, id.length());
			
			
			BosConstants.debug("sys="+sys+"   date="+date+"  name="+name);
			
			
			
			String path=System.getProperty("user.dir")+File.separator+"config"+File.separator+"export"+File.separator+sys;
			File dir=new File(path);
			if(!dir.exists()) {
				dir.mkdirs();
			}
			
			path=path+File.separator+date;
			dir=new File(path);
			if(!dir.exists()) {
				json.setUnSuccess(-1, LabelDisplay.get("不存在目录:", json.getLan())+dir.getPath(),true);
				return json.jsonValue();
			}
			
			BosConstants.debug("importJsonBySelectFile"+path+File.separator+name);
		
			ConfigUtil.initByJsonFile(sys,new File(path+File.separator+name),this.getService());
			
			
			PRootImportRecord record=new PRootImportRecord();
			record.setRecordId(PRootImportRecord.next());
			record.setSys(sys);
			record.setDate(date);
			record.setFileName(name);
			record.setUserId(ut.getUserId());
			record.setImportTime(NetWorkTime.getCurrentDatetime());
			this.service.save(record,ut);
	

			json.setSuccess("导入成功");
			
			
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
