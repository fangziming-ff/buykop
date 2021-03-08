package com.buykop.console.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
import com.buykop.console.entity.PMember;
import com.buykop.console.service.DataFormService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.cache.location.ExpiringMap;
import com.buykop.framework.entity.DiyInfField;
import com.buykop.framework.entity.DiyInfOutField;
import com.buykop.framework.entity.DiyInfQueryOperation;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.ExportTemplate;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.FieldCalculationFormula;
import com.buykop.framework.scan.PFormInField;
import com.buykop.framework.scan.PFormOutField;
import com.buykop.framework.scan.PFormQuery;
import com.buykop.framework.scan.PFormQueryOperation;
import com.buykop.framework.scan.PClob;
import com.buykop.framework.scan.PDiyUri;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PFormAction;
import com.buykop.framework.scan.PFormDiy;
import com.buykop.framework.scan.PFormField;
import com.buykop.framework.scan.PFormMember;
import com.buykop.framework.scan.PFormMsgPush;
import com.buykop.framework.scan.PFormRowAction;
import com.buykop.framework.scan.PFormSlave;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.MsgTemplate;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysCode;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.Statement;
import com.buykop.framework.scan.Table;
import com.buykop.framework.scan.TableRedicSubConfig;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.ClassInnerNotice;
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

@Module(display = "表单", sys = Constants.current_sys)
@RestController
@RequestMapping(DataFormController.URI)
public class DataFormController extends BaseController{
	
	
	private static Logger  logger=LoggerFactory.getLogger(DataFormController.class);
	
	protected static final String URI="/dtform";
	
	//private static Logger logger = Logger.getLogger(DataFormController.class);

	@Autowired
	private DataFormService service;
	

	@Security(accessType = "1", displayName = "表单列表(根据业务类)", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/listForClassName", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject listForClassName(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			
			Vector<String> v=new Vector<String>();
			
			
			
			PForm search=json.getSearch(PForm.class, "className,!formType",ut,this.service);
			search.setStatus(1L);
			QueryListInfo<PForm> list=this.service.getMgClient().getList(search,this.service);
			
			//String className=json.getSelectedId(Constants.current_sys, "/dtform/listForClassName", PForm.class.getName(), "className", true);
			Table table=this.service.getMgClient().getTableById(search.getClassName());
			if(table==null) {
				json.setUnSuccessForNoRecord(Table.class,search.getClassName());
				return json.jsonValue();
			}
			
			for(PForm t:list.getList()) {
				v.add(t.getFormId());
				t.setSelected();
			}
			
			

			
			
			
			super.listToJson(list, json,BosConstants.getTable(PForm.class));
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
		
	
	
	@Security(accessType = "1", displayName = "模板匹配机构列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/memberFetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject memberFetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String formId=json.getSelectedId(Constants.current_sys, URI+"/memberFetch", PForm.class.getName(), null, true,this.getService());

			
			PFormMember fm=new PFormMember();
			fm.setFormId(formId);
			Vector<String> mv=this.service.getMgClient().getVector(fm, "memberId",this.service);
			
			
			
			
			BosEntity search=json.getSearch(PMember.class.getName(),null,ut,this.service);
			if(search==null) {
				search=new TableJson(PMember.class.getName());
			}
			
			PageInfo page=json.getPageInfo(PMember.class.getName());
			
			QueryFetchInfo<BosEntity> fetch=this.service.getFetch(search,"seq,name,status", page.getCurrentPage(), page.getPageSize());
			for(BosEntity x:fetch.getList()) {
				if(mv.contains(x.getPk())) {
					x.setSelected();
				}
			}
			
			super.fetchToJson(fetch, json, BosConstants.getTable(PMember.class.getName()));
			
			//super.selectToJson(Field.getJsonForSelectDateRange(SysConstants.memberClassName,null), json, SysConstants.getTable(SysConstants.memberClassName).getSimpleName(), "queryDateProperty");
			//super.selectToJson(Field.getJsonForSelectNumRange(SysConstants.memberClassName,null), json, SysConstants.getTable(SysConstants.memberClassName).getSimpleName(), "queryNumProperty");
			//super.selectToJson(search.showTable().getFieldJsonForSelectCodeValue(json,null), json, SysConstants.getTable(SysConstants.memberClassName).getSimpleName(), "queryCodeValueProperty");
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	@Security(accessType = "1", displayName = "模板匹配机构清理", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/memberClear", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject memberClear(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

	
			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String formId=json.getSelectedId(Constants.current_sys, URI+"/memberFetch", PForm.class.getName(), null, true,this.getService());

			
			PFormMember fm=new PFormMember();
			fm.setFormId(formId);
			this.service.getMgClient().delete(fm,ut,this.service);
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	@Security(accessType = "1", displayName = "模板匹配机构保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/memberSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject memberSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String formId=json.getSelectedId(Constants.current_sys, URI+"/memberFetch", PForm.class.getName(), null, true,this.getService());
			
			Vector<String> idsV=json.showIds();
			
			PFormMember fm=new PFormMember();
			fm.setFormId(formId);
			
			List<BosEntity> list=json.getList(PMember.class.getName(), null,this.service);
			for(BosEntity x:list) {
				fm.setMemberId(x.getPk());
				this.service.getMgClient().deleteByPK(fm.getPk(), PFormMember.class,ut,this.service);
			}
			
			for(String x:idsV) {
				fm.setMemberId(x);
				this.service.save(fm,ut);
			}
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
			
	
	
	@Menu(js = "form", name = "数据维护表单", trunk = "开发服务,模板管理")
	@Security(accessType = "1", displayName = "流程列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			PForm search = json.getSearch(PForm.class, "",ut,this.service);
			search.setFormType(1L);
			PageInfo page = json.getPageInfo(PForm.class);
			
			if(!DataChange.isEmpty(search.getSys())) {
				
				
				QueryListInfo<PForm> fetch = this.service.getMgClient().getList(search,this.service);
				BosConstants.debug(fetch);
				for(PForm x:fetch.getList()) {
					if(x.getAllowAdd()==null) x.setAllowAdd(1L);
					if(x.getAllowDelete()==null) x.setAllowDelete(1L);
					if(DataChange.isEmpty(x.getClassName())) continue;
					Table table=BosConstants.getTable(x.getClassName());
					if(table!=null) {
						x.setSys(table.getSys());
					}
				}
				
				super.selectToJson(Table.getJsonForSelect(search.getSys(),null,this.service), json, PForm.class.getSimpleName(), "className");
				

				super.listToJson(fetch, json, BosConstants.getTable(PForm.class.getName()));
				
			}else {
				
				
				
				QueryFetchInfo<PForm> fetch = this.service.getMgClient().getFetch(search, page.getCurrentPage(),page.getPageSize(),this.service);


				fetch.initBiddingForSysClassName(this, json, "sys", "className",null);
				

				super.fetchToJson(fetch, json, BosConstants.getTable(PForm.class.getName()));
			}
			
			
			
			super.selectToJson(PRoot.getJsonForDev(ut.getMemberId(),this.service), json, PForm.class.getSimpleName(), "sys");
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1,2", displayName = "主表单更换类名", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/formClassChange", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject formClassChange(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			PForm obj=json.getObj(PForm.class, "formId,className",this.service);
			
			
			Table table = BosConstants.getTable(obj.getClassName());
			BosConstants.debug(table);
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,obj.getClassName());
				return json.jsonValue();
			}
			
			PForm src=this.service.getMgClient().getById(obj.getFormId(), PForm.class);
			if(src==null) {
				json.setUnSuccessForNoRecord(PForm.class,obj.getFormId());
				return json.jsonValue();
			}
			
			src.setClassName(obj.getClassName());

			this.service.save(src,ut);
			
			json.setSelectedId(Constants.current_sys, "/dtform/info", obj.getFormId());

			
			return this.info(json,request);
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

	}
	
	@Security(accessType = "1", displayName = "主表单详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, "/dtform/info", PForm.class.getName(), "", true,this.getService());
			
			PForm obj = this.service.getMgClient().getById(id, PForm.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PForm.class,id);
				return json.jsonValue();
			}
			if(obj.getDiyService()==null) {
				obj.setDiyService(0L);
			}
			
			
			if(DataChange.isEmpty(obj.getSys())) {
				json.setUnSuccess(-1, "请设置所属系统");
				return json.jsonValue();
			}
			
			if(DataChange.isEmpty(obj.getClassName())) {
				json.setUnSuccess(-1, "请设置绑定业务类");
				return json.jsonValue();
			}
			
			
			Table table = this.service.getMgClient().getTableById(obj.getClassName());
			if(table==null) {
				json.setUnSuccessForNoRecord(Table.class,obj.getClassName());
				return json.jsonValue();
			}
			super.objToJson(table, json);
			
			
			
			json.putUserInfo(ut);
			
			
			super.selectToJson(PRoot.getJsonForSelect(1L,this.service), json, PForm.class.getSimpleName(), "sys");

			//if (!DataChange.isEmpty(obj.getClassName())) {

			
			super.selectToJson(Table.getPkDBJsonForSelect(obj.getSys(),this.service), json, PForm.class.getSimpleName(),"className");
			
			if(!DataChange.isEmpty(obj.getClassName())) {
				super.selectToJson(Statement.getJsonForSelect(obj.getClassName(),this.service), json, PForm.class.getSimpleName(),"mapId");
			}
			
			super.selectToJson(PSysCode.getForSelect("126",this.service), json, PForm.class.getSimpleName(),"dataPerType");
			
			super.selectToJson(Table.getCodeValueFieldJsonForSelect(obj.getClassName(),this.service), json, PForm.class.getSimpleName(),"tabField");
			
		
			
			super.selectToJson(PTreeForm.getJsonForTreeSelect(ut.getMemberId(),obj.getClassName(),this.service), json, PForm.class.getSimpleName(),"treeId");
			
			//设置树形结构关联id
			super.selectToJson(Field.getJsonForSelectWithFK(obj.getClassName(), 1L, null,this.service), json, PForm.class.getSimpleName(),"treeIdRField");
			
			//super.selectToJson(PRole.getJsonForMember(ut.getMemberId(),obj.getSys()), json, PForm.class.getSimpleName(),"roleId");
			
			if(true) {
				
				SelectBidding  msb=CacheTools.getSysCodeSelectBidding("103",json.getLan());
				
				PDiyUri  dpu=new PDiyUri();
				dpu.setClassName(obj.getClassName());
				dpu.setExport(1L);
				QueryListInfo<PDiyUri> dpuList=this.service.getMgClient().getList(dpu,"uri",this.service);
				BosConstants.debug("PDiyUri   "+obj.getClassName()+"   export=1  size="+dpuList.size());
				for(PDiyUri x:dpuList.getList()) {
					msb.put(x.getUri(), x.getDisplay());
				}
				
				
				
				ExportTemplate tempate=new ExportTemplate();
				tempate.setClassName(obj.getClassName());
				tempate.setStatus(1L);
				QueryListInfo<ExportTemplate> tList=this.service.getMgClient().getList(tempate,"templateName",this.service);
				for(ExportTemplate x:tList.getList()) {
					if(DataChange.isEmpty(x.getFields())) continue;
					msb.put(BosConstants.EXPORT_TEMPLATE_URI+"/download/"+x.getTemplateId(), x.getTemplateName());
				}
				
				//加入个性化的导出配置
				super.selectToJson(msb, json, PForm.class.getName(), "exportMode");
			}
			
			
			PClob clob=this.service.getMgClient().getById(obj.getScriptId(), PClob.class);
			if(clob!=null) {
				obj.setScript(clob.getContent());
			}
			clob=this.service.getMgClient().getById(obj.getOnloadScriptId(), PClob.class);
			if(clob!=null) {
				obj.setOnloadScript(clob.getContent());
			}
			clob=this.service.getMgClient().getById(obj.getSubmitScriptId(), PClob.class);
			if(clob!=null) {
				obj.setSubmitScript(clob.getContent());
			}
			
			//Constants.debug("id="+obj.getScriptId()+"  content="+obj.getScript());
			
			if(obj.getMultiFileNum()==null) {
				obj.setMultiFileNum(0L);
			}
			
			

			if(true) {
				
				SelectBidding data=new SelectBidding();
				PDiyUri  dpu=new PDiyUri();
				dpu.setClassName(obj.getClassName());
				dpu.setExport(0L);
				QueryListInfo<PDiyUri> dpuList=this.service.getMgClient().getList(dpu,"uri",this.service);
				BosConstants.debug("PDiyUri   "+obj.getClassName()+"   export=0  size="+dpuList.size());
				for(PDiyUri x:dpuList.getList()) {
					data.put(x.getUri(), x.getDisplay()+"["+x.getUri()+"]");
				}
				
				super.selectToJson(data, json, PForm.class, "diyUri");
			}
			
			
			
			super.objToJson(obj, json);
			
			
			SelectBidding sbIn = new SelectBidding();
			SelectBidding sbOut = new SelectBidding();
			SelectBidding sbQuery = new SelectBidding();
			
			QueryListInfo<PFormField> fList=new QueryListInfo<PFormField>();
			
			QueryListInfo<PFormSlave> tableList =new QueryListInfo<PFormSlave>();
			
			
			
			
			
			
			
			
			
			
			HashMap<String,Field> fkFieldHash=new HashMap<String,Field>();
			
			
			if(!DataChange.isEmpty(obj.getClassName())) {
				
				//super.selectToJson(cn.powerbos.framework.util.Util.getIcoJson(Table.class.getName(), obj.getClassName()), json, PForm.class.getSimpleName(), "icoId");
				
				//HashMap<String,Field> fHash=new HashMap<String,Field>();
				
				
				HashMap<String,PFormField> fieldHash=new HashMap<String,PFormField>();
				
				
				
				PFormField field = new PFormField();
				field.setFormId(obj.getFormId());
				field.setClassName(obj.getClassName());
				QueryListInfo<PFormField> oList = this.service.getMgClient().getList(field, "seq",this.service);
				for (PFormField x : oList.getList()) {
					fieldHash.put(x.getProperty(), x);
					fList.getList().add(x);
				}
				
				
				Vector<String> notV=MyString.splitBy("queryCodeValue,queryCodeValueProperty,queryDateMax,queryDateMin,queryDateProperty,queryNumMax,queryNumMin,queryNumProperty", ",");

				Field sf = new Field();
				sf.setClassName(obj.getClassName());
				sf.setCustomer(0L);
				//sf.setPropertyType(1L);
				//sf.setIsKey(0L);
				QueryListInfo<Field> fxList=this.service.getMgClient().getTableFieldList(sf, "!isKey,!propertyType,property");
				for(Field x:fxList.getList()) {
					sbIn.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "  "
							+ CacheTools.getSysCodeDisplay("5", String.valueOf(x.getPropertyType()), json.getLan()) + " ]");
					
					sbOut.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "  "
							+ CacheTools.getSysCodeDisplay("5", String.valueOf(x.getPropertyType()), json.getLan()) + " ]");
					
					if(x.judgeDB()) {
						sbQuery.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "  "
								+ CacheTools.getSysCodeDisplay("5", String.valueOf(x.getPropertyType()), json.getLan()) + " ]");
					}
					
					
					PFormField o=fieldHash.get(x.getProperty());
					if(o==null) {
						
						if(notV.contains(x.getProperty())) {
							continue;
						}
						
						o= new PFormField();
						o.setClassName(obj.getClassName());
						o.setProperty(x.getProperty());
						o.setDefaultValue(x.getDefaultValue());
						if(o.getIsRead()==null) {
							o.setIsRead(0L);
						}
						if(o.getIsNotNuLL()==null) {
							o.setIsNotNuLL(0L);
						}
						if(DataChange.isEmpty(o.getDisType())){
							o.setDisType("T");
						}

						
						o.setFormId(obj.getFormId());
						o.setDisplay(x._getSimpleDisplay());
						o.setValueClass(x.getValueClass());
						o.setFkClasss(x.getFkClasss());
						o.setDbLen(x.getDbCol());
						o.setPropertyType(x.getPropertyType());
						
						
						fList.getList().add(o);
						
						
					}else {
						o.setSelected("checked");
						if(!DataChange.isEmpty(x.getFkClasss())) {
							fkFieldHash.put(x.getProperty(), x);
						}
						if(o.getIsRead()==null) {
							o.setIsRead(0L);
						}
						if(o.getIsNotNuLL()==null) {
							o.setIsNotNuLL(0L);
						}
						if(DataChange.isEmpty(o.getDisType())){
							o.setDisType("T");
						}
						
						o.setFormId(obj.getFormId());
						o.setDisplay(x._getSimpleDisplay());
						o.setValueClass(x.getValueClass());
						o.setFkClasss(x.getFkClasss());
						o.setDbLen(x.getDbCol());
						o.setPropertyType(x.getPropertyType());
						fieldHash.remove(x.getProperty());
						continue;
						
					}
					
					
					
					
					
					
					
					//if(!DataChange.isEmpty(x.getFkClasss()) && x.getFkClasss().equals(obj.getClassName()) ) continue;
					//if(BosConstants._fieldV.contains(x.getProperty())) continue;
					//fHash.put(x.getProperty(), x);
				}
				


				Iterator<String> its=fieldHash.keySet().iterator();
				while(its.hasNext()) {
					PFormField ff=fieldHash.get(its.next());
					fList.getList().remove(ff);
					
				}
				
				//Vector<PFormSlave> delV1 = new Vector<PFormSlave>();

				Vector<String> tV = Table.getSlaverV(obj.getClassName());
				BosConstants.debug("*******"+obj.getClassName()+"  slaver-size1="+tV.size());

				
				
				
				// 查询所有的从表信息
				PFormSlave slave = new PFormSlave();
				slave.setFormId(obj.getFormId());
				Vector<String> ev= this.service.getMgClient().getVector(slave, "className", service);
				for(String x:ev) {
					if(!tV.contains(x)) {
						PFormSlave wf = new PFormSlave();
						wf.setFormId(obj.getFormId());
						wf.setClassName(x);
						this.service.getMgClient().deleteByPK(wf.getPk(), PFormSlave.class,ut,this.service);
						//删除
					}
				}
				
				for(String x:tV) {
					PFormSlave wf = new PFormSlave();
					wf.setFormId(obj.getFormId());
					wf.setClassName(x);
					if (ev.contains(x)) {
						wf.setSelected();
					}
					tableList.getList().add(wf);
				}
				
				
				/**tableList = this.service.getMgClient().getList(slave, "className",this.service);
				
				for (PFormSlave t : tableList.getList()) {
					if (!tV.contains(t.getClassName())) {
						delV1.add(t);
						this.service.getMgClient().deleteByPK(t.getPk(), PFormSlave.class,ut,this.service);
						continue;
					}
					t.setSelected("checked");
					tV.remove(t.getClassName());
				}
				
				for (PFormSlave x :delV1) {
					tableList.getList().remove(x);
				}
				
				for (String x : tV) {
					PFormSlave wf = new PFormSlave();
					wf.setFormId(obj.getFormId());
					wf.setClassName(x);
					tableList.getList().add(wf);
				}*/
				
				
			}
				
				//
			super.listToJson(fList, json, BosConstants.getTable(PFormField.class));	
			
			

			
			super.listToJson(tableList, json, BosConstants.getTable(PFormSlave.class.getName()));
			
			
			if(true) {
				
				PFormInField dif = new PFormInField();
				dif.setId(id);
				QueryListInfo<PFormInField> dList = this.service.getList(dif, "field");
				super.listToJson(dList, json, dif.showTable());
				super.selectToJson(sbIn, json, PFormInField.class, "field");
			}
			
			
			if(true) {
				PFormOutField dif = new PFormOutField();
				dif.setId(id);
				QueryListInfo<PFormOutField> dList = this.service.getList(dif, "field");
				
				for(PFormOutField x:dList.getList()) {
					if(fkFieldHash.containsKey(x.getField())) {
						Field field=fkFieldHash.get(x.getField());
						
						Table fkT=BosConstants.getTable(field.getFkClasss());
						if(fkT!=null) {
							SelectBidding sb=new SelectBidding();
							//加入redis属性
							for(Field rf:fkT.listDBFields()) {
								if(DataChange.getIntValueWithDefault(rf.getSynRedis(), 0)==1) {
									sb.put(rf.getProperty(), rf.getDisplay()+"["+CacheTools.getSysCodeDisplay("121", rf.getValueClass(), json.getLan())  +"]");
								}
							}
							if(sb.size()>0) {
								super.selectToJson2(sb, json, x, "fkRedisFields");
								fkFieldHash.remove(x.getField());
							}
						}
						x.setFkClasss(field.getFkClasss());
					}
				}
				
				/**Iterator<String> its=fkFieldHash.keySet().iterator();
				while(its.hasNext()) {
					String f=its.next();
					Field field=fkFieldHash.get(f);
					
					PFormOutField added=new PFormOutField();
					added.setId(id);
					added.setFkClasss(field.getFkClasss());
					added.setField(field.getProperty());
					
					Table fkT=BosConstants.getTable(field.getFkClasss());
					if(fkT!=null) {
						SelectBidding sb=new SelectBidding();
						//加入redis属性
						for(Field rf:fkT.listDBFields()) {
							if(DataChange.getIntValueWithDefault(rf.getSynRedis(), 0)==1) {
								sb.put(rf.getProperty(), rf.getDisplay()+"["+CacheTools.getSysCodeDisplay("121", rf.getValueClass(), json.getLan())  +"]");
							}
						}
						if(sb.size()>0) {
							dList.getList().add(added);
							super.selectToJson2(sb, json, added, "fkRedisFields");
						}
					}
				}*/
				
				
				super.listToJson(dList, json, dif.showTable());
				super.selectToJson(sbOut, json, PFormOutField.class, "field");
			}
			
			
			
			
			
			//js操作
			PFormAction js=new PFormAction();
			js.setFormId(id);
			QueryListInfo<PFormAction> jsList=this.service.getMgClient().getList(js, "seq,actionName",this.service);
			super.listToJson(jsList, json, BosConstants.getTable(PFormAction.class));
			super.selectToJson(PRole.getJsonForDev(ut.getMemberId(), obj.getSys(),this.service), json, PFormAction.class, "actionRole");
			
			
			PFormDiy diy=new PFormDiy();
			diy.setFormId(id);
			QueryListInfo<PFormDiy> diyList=this.service.getMgClient().getList(diy, "action,diyUrl",this.service);
			super.listToJson(diyList, json, BosConstants.getTable(PFormDiy.class));
			SelectBidding actions=new SelectBidding();
			actions.put("info", "详情");
			actions.put("delete", "删除");
			actions.put("saveList", "列表保存");
			actions.put("showAdd", "显示新增");
			actions.put("save", "单个保存");
			for(PFormAction x:jsList.getList()) {
				actions.put(x.getAction(), x.getActionName());
			}
			super.selectToJson(actions, json, PFormDiy.class, "action");
			
			SelectBidding  urls=new SelectBidding();
			
			PDiyUri  dpu=new PDiyUri();
			dpu.setClassName(obj.getClassName());
			dpu.setExport(0L);
			QueryListInfo<PDiyUri> dpuList=this.service.getMgClient().getList(dpu,"uri",this.service);
			BosConstants.debug("PDiyUri   "+obj.getClassName()+"   export=0  size="+dpuList.size());
			for(PDiyUri x:dpuList.getList()) {
				urls.put(x.getUri(), x.getDisplay());
			}
			super.selectToJson(urls, json, PFormDiy.class, "diyUrl");
			
			
			
			
			//-----------------------------------------------------------------------------------------------------------------------
			
			if(true) {
				PFormQuery bq=new PFormQuery();
				bq.setFormId(id);
				QueryListInfo<PFormQuery> dList=service.getList(bq, "seq");
				super.listToJson(dList, json, bq.showTable());
			}
			
			
			
			if(true) {
				PFormMsgPush bq=new PFormMsgPush();
				bq.setId(id);
				QueryListInfo<PFormMsgPush> dList=service.getList(bq, "sort");
				super.listToJson(dList, json, bq.showTable());
				super.selectToJson(MsgTemplate.getJsonForSelect(obj.getClassName(),this.service), json, PFormMsgPush.class, "templateId");
			}
			
			
			if(true) {
				
				PFormQueryOperation qo=new PFormQueryOperation();
				qo.setId(id);
				QueryListInfo<PFormQueryOperation> list=this.service.getList(qo, "field");
				super.listToJson(list, json, qo.showTable());
				super.selectToJson(sbQuery, json, PFormQueryOperation.class, "field");
				
			}
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "复制", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/copy", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject copy(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/dtform/copy", PForm.class.getName(), null,true,this.getService());
			
			PForm obj=this.service.getMgClient().getById(id, PForm.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PForm.class,id);
				return json.jsonValue();
			}
			
			PForm.copy(id, ut.getMemberId(),this.service);

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}
	
	
	
	
	
	
	@Security(accessType = "1", displayName = "主表单列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {



			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			List<PForm> list=json.getList(PForm.class, "formId,formName,sys,!className,!diyJs",this.service);
			
			for(PForm x:list) {
				
				PForm src=this.service.getMgClient().getById(x.getFormId(), PForm.class);
				
				
				if(src!=null) {
					if(!src.getSys().equals(x.getSys())) {
						x.setClassName(null);
					}
				}
				
				
				x.setFormType(1L);
				this.service.save(x,ut);
				BosConstants.debug(x);
				
				BosConstants.getExpireHash().remove(PForm.class, x.getFormId());
				new ClassInnerNotice().invoke(PForm.class.getSimpleName(), x.getFormId());
			}
			

			json.setSuccess("保存成功");
			

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}	
	

	@Security(accessType = "1", displayName = "主表单设置", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {



			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			PForm obj = json.getObj(PForm.class, "formId,className,allowAdd,allowDelete,allowModify",this.service);

			if(DataChange.isEmpty(obj.getDiyUri())) {
				obj.setDiyService(0L);
			}else {
				obj.setDiyService(1L);
			}
			
			
			if(!DataChange.isEmpty(obj.getCodeFormula()) &&  obj.getCodeScope()==null) {
				json.setUnSuccess(-1, "请选择编码作用域");
				return json.jsonValue();
			}
			
			if(obj.getRowCols()==null) {
				obj.setRowCols(4L);
			}
			
			
			PForm src=this.service.getMgClient().getById(obj.getFormId(), PForm.class);
			if (src == null) {
				json.setUnSuccessForNoRecord(PForm.class,obj.getFormId());
				return json.jsonValue();
			}
			
			
			
			
			Vector<String> dpV=MyString.splitBy(obj.getDataPerType(), ",");
			
			//0:所属用户  1:所属部门   2：所属组织   3:综合所属权   4:数据权限     5:地区     10:无  
			if(dpV.contains("5") ) {
				
				Table table=BosConstants.getTable(obj.getClassName());
				
				if(DataChange.getLongValueWithDefault(table.getIsMap(), 0)==0) {
					json.setUnSuccess(-1, table.getDisplayName()+"不支持地图操作,无法按照所属地区查询");
					return json.jsonValue();
				}
				
			}
			
			
			
			Table table = BosConstants.getTable(obj.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,obj.getClassName());
				return json.jsonValue();
			}
			
			
			
			if(DataChange.isEmpty(obj.getScript())) {
				this.service.getMgClient().deleteByPK(obj.getFormId(), PClob.class,ut,this.service);
				obj.setScriptId(null);
			}else {
				PClob clob=new PClob();
				clob.setClobId(obj.getFormId());
				clob.setContent(obj.getScript());
				clob.addToMust("content");
				this.service.save(clob,ut);
				BosConstants.debug(clob);
				obj.setScriptId(obj.getFormId());
			}
			obj.addToMust("scriptId");
			
			
			if(DataChange.isEmpty(obj.getOnloadScript())) {
				this.service.getMgClient().deleteByPK(obj.getFormId()+"1", PClob.class,ut,this.service);
				obj.setOnloadScriptId(null);
			}else {
				PClob clob=new PClob();
				clob.setClobId(obj.getFormId()+"1");
				clob.setContent(obj.getOnloadScript());
				clob.addToMust("content");
				this.service.save(clob,ut);
				BosConstants.debug(clob);
				obj.setOnloadScriptId(obj.getFormId()+"1");
			}
			obj.addToMust("onloadScriptId");
			
			
			
			if(DataChange.isEmpty(obj.getSubmitScript())) {
				this.service.getMgClient().deleteByPK(obj.getFormId()+"2", PClob.class,ut,this.service);
				obj.setSubmitScriptId(null);
			}else {
				PClob clob=new PClob();
				clob.setClobId(obj.getFormId()+"2");
				clob.setContent(obj.getSubmitScript());
				clob.addToMust("content");
				this.service.save(clob,ut);
				BosConstants.debug(clob);
				obj.setSubmitScriptId(obj.getFormId()+"2");
			}
			obj.addToMust("submitScriptId");
			
			
			//Constants.debug("id="+form.getFormId()+"  content="+form.getScript());
			this.service.save(obj,ut);
			BosConstants.debug(obj);
			
			
			PFormField del=new PFormField();
			del.setFormId(obj.getFormId());
			del.setClassName(src.getClassName());
			Vector<String> fieldV=this.service.getMgClient().getVector(del, "property",this.service);
			
			
			Vector<String> ids = json.showIds("ids");
			List<PFormField> list = json.getList(PFormField.class,"property,!disType,!isNotNuLL,!attribute,!condition,!singleLine,!defaultValue",this.service);
			
			
			long seq = 0;
			for (PFormField x : list) {
				
				x.setFormId(obj.getFormId());
				x.setClassName(src.getClassName());
				
				if(x.getRowspan()==null) {
					x.setRowspan(1L);
				}
				
				if(x.getColspan()==null) {
					x.setColspan(1L);
				}
				
				
				if (ids.contains(x.getPk())) {
					fieldV.remove(x.getProperty());
					x.setSeq(seq++);
					if (DataChange.isEmpty(x.getDisType())) {
						json.setUnSuccess(-1, LabelDisplay.get("属性:", json.getLan())  + x.getProperty() + LabelDisplay.get("未设置显示方式", json.getLan()),true );
						return json.jsonValue();
					}
					this.service.save(x,ut);
					BosConstants.debug(x);
				} else {
					this.service.getMgClient().deleteByPK(x.getPk(), PFormField.class,ut,this.service);
				}
			}
		
			for(String x:fieldV) {
				del=new PFormField();
				del.setFormId(obj.getFormId());
				del.setClassName(src.getClassName());
				del.setProperty(x);
				this.service.getMgClient().delete(del,ut,this.service);
			}
			
			
			
			if(!DataChange.isEmpty(obj.getSearchFields())) {
				
				Vector<String> fv=Field.split(obj.getSearchFields());
				for(String f:fv) {
					if(DataChange.isEmpty(f)) continue;
					if(f.indexOf("|")!=-1) f=f.substring(0, f.indexOf("|"));
					Field field=new Field();
					field.setClassName(obj.getClassName());
					field.setProperty(f);
					field=this.service.getMgClient().get(field,this.service);
					if(field==null) {
						json.setUnSuccess(-1, LabelDisplay.get("自定义查询条件:", json.getLan()) +obj.getSearchFields()+LabelDisplay.get(" 检查，不存在属性:", json.getLan()) +f,true);
						return json.jsonValue();
					}else if(DataChange.getLongValueWithDefault(field.getPropertyType(), 0)==1) {
						PFormField pff=new PFormField();
						pff.setFormId(obj.getFormId());
						pff.setProperty(f);
						pff=this.service.getMgClient().get(pff,this.service);
						if(pff==null) {
							json.setUnSuccess(-1, LabelDisplay.get("自定义查询条件:", json.getLan()) +obj.getSearchFields()+LabelDisplay.get(" 检查，模板中没有属性:", json.getLan()) +f,true);
							return json.jsonValue();
						}
					}
				}
			}
			
			
			
			if(!DataChange.isEmpty(obj.getListFields())) {
				Vector<String> fv=Field.split(obj.getListFields());
				for(String f:fv) {
					Field field=new Field();
					field.setClassName(obj.getClassName());
					field.setProperty(f);
					field=this.service.getMgClient().get(field,this.service);
					if(field==null) {
						json.setUnSuccess(-1, LabelDisplay.get("自定义列表字段:", json.getLan()) +obj.getListFields()+LabelDisplay.get(" 检查，不存在属性:", json.getLan()) +f,true);
						return json.jsonValue();
					}else if(DataChange.getLongValueWithDefault(field.getPropertyType(), 0)==1) {
						PFormField pff=new PFormField();
						pff.setFormId(obj.getFormId());
						pff.setProperty(f);
						pff=this.service.getMgClient().get(pff,this.service);
						if(pff==null) {
							json.setUnSuccess(-1, LabelDisplay.get("自定义列表字段:", json.getLan()) +obj.getListFields()+LabelDisplay.get(" 检查，模板中没有属性:", json.getLan()) +f,true);
							return json.jsonValue();
						}
					}
					
					
				}
			}
			
			
			

			if(!DataChange.isEmpty(obj.getListEditFields())) {
				Vector<String> fv=Field.split(obj.getListEditFields());
				for(String f:fv) {
					Field field=new Field();
					field.setClassName(obj.getClassName());
					field.setProperty(f);
					field=this.service.getMgClient().get(field,this.service);
					if(field==null) {
						json.setUnSuccess(-1, LabelDisplay.get("列表可编辑字段:", json.getLan()) +obj.getListFields()+LabelDisplay.get(" 检查，不存在属性:", json.getLan()) +f,true);
						return json.jsonValue();
					}else if(DataChange.getLongValueWithDefault(field.getPropertyType(), 0)==1) {
						PFormField pff=new PFormField();
						pff.setFormId(obj.getFormId());
						pff.setProperty(f);
						pff=this.service.getMgClient().get(pff,this.service);
						if(pff==null) {
							json.setUnSuccess(-1,LabelDisplay.get("列表可编辑字段:", json.getLan()) +obj.getListFields()+LabelDisplay.get(" 检查，模板中没有属性:", json.getLan()) +f,true);
							return json.jsonValue();
						}
					}
					
					
				}
			}
			
			
			if(true) {
				Vector<String> ids1 = json.showIds("ids1");
				PFormSlave slave = new PFormSlave();
				slave.setFormId(obj.getFormId());
				QueryListInfo<PFormSlave> slaveList=this.service.getMgClient().getList(slave,this.service);
				for(PFormSlave x:slaveList.getList()) {
					if(!ids1.contains(x.getPk())) {
						this.service.getMgClient().deleteByPK(x.getPk(), PFormSlave.class,ut,this.service);
					}
				}
				for(String x:ids1) {
					PFormSlave fs=new PFormSlave();
					fs.setPK(x);
					if(!DataChange.isEmpty(fs.getPk())) {
						this.service.save(fs, ut);
					}
				}
			}
			
			
			if(true) {
				PFormAction old=new PFormAction();
				old.setFormId(obj.getFormId());
				this.service.getMgClient().delete(old,ut,this.service);
				seq=0;
				List<PFormAction> alist = json.getList(PFormAction.class,"action,actionType,actionName,!actionRole,!condition,!attribute",this.service);
				for(PFormAction x:alist) {
					x.setClassName(src.getClassName());
					x.setFormId(obj.getFormId());
					this.service.save(x,null);
				}
			}
			
			
			
			
			
			
			
			

			if(true) {
				PFormInField ds=new PFormInField();
				ds.setId(obj.getFormId());
				this.service.getMgClient().delete(ds,ut,this.service);
				seq=0;
				List<PFormInField> dlist = json.getList(PFormInField.class,"field,formula",this.service);
				for(PFormInField x:dlist) {
					x.setId(obj.getFormId());
					this.service.save(x,null);
				}
			}
			
			
			if(true) {
				PFormQuery ds=new PFormQuery();
				ds.setFormId(obj.getFormId());
				this.service.getMgClient().delete(ds,ut,this.service);
				seq=0;
				List<PFormQuery> dlist = json.getList(PFormQuery.class,"fields,value,type",this.service);
				for(PFormQuery x:dlist) {
					
					Vector<String> v=Field.split(x.getFields());
					for(String f:v) {
						if(table.getDBField(f)==null) {
							json.setUnSuccess(-1, LabelDisplay.get("查询特殊条件:", json.getLan()) +x.getFields()+LabelDisplay.get(" 检查，不存在属性:", json.getLan()) +f,true);
							return json.jsonValue();
						}
					}
					x.setFormId(obj.getFormId());
					x.setSeq(seq++);
					this.service.save(x,null);
				}
			}
			
			
			
			
			
			if(true) {
				PFormQueryOperation ds=new PFormQueryOperation();
				ds.setId(obj.getFormId());
				this.service.getMgClient().delete(ds,ut,this.service);
				seq=0;
				List<PFormQueryOperation> dlist = json.getList(PFormQueryOperation.class,"field,queryOperation",this.service);
				for(PFormQueryOperation x:dlist) {
					x.setId(obj.getFormId());
					this.service.save(x,null);
				}
			}
			
			
			if(true) {
				PFormMsgPush ds=new PFormMsgPush();
				ds.setId(obj.getFormId());
				this.service.getMgClient().delete(ds,ut,this.service);
				seq=0;
				List<PFormMsgPush> dlist = json.getList(PFormMsgPush.class,"templateId,msgType,actionType",this.service);
				for(PFormMsgPush x:dlist) {
					x.setId(obj.getFormId());
					x.setSort(seq++);
					this.service.save(x,null);
				}
			}
			
			
			
			if(true) {
				PFormOutField ds=new PFormOutField();
				ds.setId(obj.getFormId());
				this.service.getMgClient().delete(ds,ut,this.service);
				seq=0;
				List<PFormOutField> dlist = json.getList(PFormOutField.class,"field,!formula",this.service);
				for(PFormOutField x:dlist) {
					x.setId(obj.getFormId());
					this.service.save(x,null);
				}
			}
			
			
			
			List<PFormDiy> dist=json.getList(PFormDiy.class, "action,diyUrl", service);
			for(PFormDiy x:dist) {
				x.setFormId(obj.getFormId());
				this.service.save(x,null);
			}
			
			
			BosConstants.getExpireHash().remove(PForm.class, obj.getFormId());
			new ClassInnerNotice().invoke(PForm.class.getSimpleName(), obj.getFormId());
			
			
			json.setSuccess("保存成功");
			

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "从表单信息", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/slaveInfo", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject slaveInfo(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			PFormSlave form = json.getObj(PFormSlave.class, "formId,className",this.service);
			
			
			PForm formObj=this.service.getMgClient().getById(form.getFormId(),PForm.class);
			if(formObj==null) {
				json.setUnSuccessForNoRecord(PForm.class,form.getFormId());
				return json.jsonValue();
			}
			if(DataChange.isEmpty(formObj.getClassName())) {
				json.setUnSuccess(-1, "流程节点业务类对象未设置");
				return json.jsonValue();
			}
			
			

			Table table = this.service.getMgClient().getTableById(form.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,form.getClassName());
				return json.jsonValue();
			}
			
			if(table.getRegType()==null) {
				table.setRegType(1L);
			}
			
			super.objToJson(table, json);

			PFormSlave obj = this.service.getMgClient().getById(form.getPk(), PFormSlave.class);
			if (obj == null) {
				obj = new PFormSlave();
			}
			obj.setFormId(form.getFormId());
			obj.setClassName(form.getClassName());
			if(!DataChange.replaceNull(obj.getAllowAdd()).equals("0")) {
				obj.setAllowAdd("1");
			}
			if(!DataChange.replaceNull(obj.getAllowDelete()).equals("0")) {
				obj.setAllowDelete("1");
			}
			if(!DataChange.replaceNull(obj.getAllowVisible()).equals("0")) {
				obj.setAllowVisible("1");
			}
			
			super.objToJson(obj, json);
			
			super.selectToJson(CacheTools.getSysCodeSelectBidding("3",json.getLan()), json, PFormSlave.class.getSimpleName(), "allowAdd");
			super.selectToJson(CacheTools.getSysCodeSelectBidding("3",json.getLan()), json, PFormSlave.class.getSimpleName(), "allowDelete");
			super.selectToJson(CacheTools.getSysCodeSelectBidding("3",json.getLan()), json, PFormSlave.class.getSimpleName(), "allowVisible");
			
			//HashMap<String,Field> fHash=new HashMap<String,Field>();
			
			
			QueryListInfo<PFormField> fList=new QueryListInfo<PFormField>();
			
			HashMap<String,PFormField> fieldHash=new HashMap<String,PFormField>();
			
			PFormField field = new PFormField();
			field.setFormId(form.getFormId());
			field.setClassName(form.getClassName());
			QueryListInfo<PFormField> oList = this.service.getMgClient().getList(field, "seq",this.service);
			for (PFormField x : oList.getList()) {
				fieldHash.put(x.getProperty(), x);
				fList.getList().add(x);
			}
			
			
			Vector<String> notV=MyString.splitBy("queryCodeValue,queryCodeValueProperty,queryDateMax,queryDateMin,queryDateProperty,queryNumMax,queryNumMin,queryNumProperty", ",");
			Field sf = new Field();
			sf.setClassName(form.getClassName());
			sf.setCustomer(0L);
			//sf.setPropertyType(1L);
			//sf.setIsKey(0L);
			if(table.getRegType().intValue()==1) {
				sf.setRegType(1L);
			}
			QueryListInfo<Field> fxList=this.service.getMgClient().getTableFieldList(sf, "property");
			for(Field x:fxList.getList()) {
				if(!DataChange.isEmpty(x.getFkClasss()) && x.getFkClasss().equals(formObj.getClassName()) ) continue;
				if(BosConstants._fieldV.contains(x.getProperty())) continue;
				
				
				
				PFormField o=fieldHash.get(x.getProperty());
				if(o==null) {
					if(notV.contains(x.getProperty())) {
						continue;
					}
					o = new PFormField();
					o.setFormId(form.getFormId());
					o.setClassName(form.getClassName());
					o.setProperty(x.getProperty());
					o.setDefaultValue(x.getDefaultValue());
					
					if(o.getIsRead()==null) {
						o.setIsRead(0L);
					}
					if(o.getIsNotNuLL()==null) {
						o.setIsNotNuLL(0L);
					}
					if(DataChange.isEmpty(o.getDisType())){
						o.setDisType("T");
					}

					o.setFormId(form.getFormId());
					o.setDisplay(x._getSimpleDisplay());
					o.setValueClass(x.getValueClass());
					o.setFkClasss(x.getFkClasss());
					o.setDbLen(x.getDbCol());
					o.setPropertyType(x.getPropertyType());
					
					
					fList.getList().add(o);
					
				}else {
					
					
					o.setSelected("checked");
					
					if(o.getIsRead()==null) {
						o.setIsRead(0L);
					}
					if(DataChange.isEmpty(o.getDisType())){
						o.setDisType("T");
					}
					if(DataChange.isEmpty(o.getIsNotNuLL())){
						o.setIsNotNuLL(0L);
					}
					
					o.setFormId(form.getFormId());
					o.setDisplay(x._getSimpleDisplay());
					o.setValueClass(x.getValueClass());
					o.setFkClasss(x.getFkClasss());
					o.setDbLen(x.getDbCol());
					o.setPropertyType(x.getPropertyType());
					
					
					
				}
				
				
				
				//fHash.put(x.getProperty(), x);
			}
			

			Iterator<String> its=fieldHash.keySet().iterator();
			while(its.hasNext()) {
				PFormField ff=fieldHash.get(its.next());
				fList.getList().remove(ff);
				
			}
			
			super.listToJson(fList, json,BosConstants.getTable(PFormField.class));
		
			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "从表单设置", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/slaveSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject slaveSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		
		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			PFormSlave form = json.getObj(PFormSlave.class, "formId,className,orderBy",this.service);
			
			
			
			Table table=BosConstants.getTable(form.getClassName());
			if(table==null) {
				json.setUnSuccessForNoRecord(Table.class,form.getClassName());
				return json.jsonValue();
			}
			
			
			if(!DataChange.isEmpty(form.getOrderBy())) {
				Vector<String> v=Field.split(form.getOrderBy());
				for(String x:v) {
					Field fx=table.getField(x);
					if(fx==null) {
						json.setUnSuccess(-1, LabelDisplay.get("排序字段:", json.getLan()) +x+LabelDisplay.get("不存在", json.getLan()),true);
						return json.jsonValue();
					}
				}
			}
			
			if(DataChange.isEmpty(table.getCodeField())) {
				form.setCodeFormula(null);
				form.setCodeScope(null);
				form.addToMust("codeFormula");
				form.addToMust("codeScope");
			}
			
			
			

			Vector<String> ids = json.showIds();
			
			
			
			
			
			
			Vector<String> v=Field.split(form.getOrderBy());
			for(String s:v) {
				Field field=table.getField(s);
				if(field==null) {
					json.setUnSuccess(-1, LabelDisplay.get("排序字段:", json.getLan()) +s+LabelDisplay.get("不存在", json.getLan()) ,true);
					return json.jsonValue();
				}
			}
			
			
			PFormField del=new PFormField();
			del.setFormId(form.getFormId());
			del.setClassName(form.getClassName());
			Vector<String> fieldV=this.service.getMgClient().getVector(del, "property",this.service);
			
			

			List<PFormField> list = json.getList(PFormField.class,
					"property,!disType,!isNotNuLL,!attribute,!condition,!singleLine,!defaultValue",this.service);

			long seq = 0;
			for (PFormField x : list) {
				x.setFormId(form.getFormId());
				x.setClassName(form.getClassName());
				if (ids.contains(x.getPk())) {
					fieldV.remove(x.getProperty());
					x.setSeq(seq++);
					if (DataChange.isEmpty(x.getDisType())) {
						json.setUnSuccess(-1, LabelDisplay.get("属性:" , json.getLan()) + x.getProperty() + LabelDisplay.get("未设置显示方式", json.getLan()) ,true);
						return json.jsonValue();
					}

					this.service.save(x,ut);
					BosConstants.debug(x);
				} 
			}
			
			for(String x:fieldV) {
				del=new PFormField();
				del.setFormId(form.getFormId());
				del.setClassName(form.getClassName());
				del.setProperty(x);
				this.service.getMgClient().delete(del,ut,this.service);
			}
			
			this.service.save(form,ut);
			BosConstants.debug(form);
			
			
			BosConstants.getExpireHash().remove(PForm.class, form.getFormId());
			new ClassInnerNotice().invoke(PForm.class.getSimpleName(), form.getFormId());
			
			
			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		
		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/dtform/delete", PForm.class.getName(), null,true,this.getService());
			PForm.delete(id, ut.getMemberId(),this.service);

			
			
			
			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}

	
	
	
	
	@Security(accessType = "1", displayName = "删除行为操作", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/deleteAction", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deleteAction(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {



			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/dtform/deleteAction", PFormAction.class.getName(), null,true,this.getService());
			
			PFormAction obj=this.service.getMgClient().getById(id, PFormAction.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PFormAction.class,id);
				return json.jsonValue();
			}
			
			
			PForm form=this.service.getMgClient().getById(obj.getFormId(), PForm.class);
			if(form==null) {
				json.setUnSuccessForNoRecord(PForm.class,obj.getFormId());
				return json.jsonValue();
			}
			

			this.service.getMgClient().deleteByPK(id, PFormAction.class,ut,this.service);
			
			
			BosConstants.getExpireHash().remove(PForm.class, obj.getFormId());
			new ClassInnerNotice().invoke(PForm.class.getSimpleName(), obj.getFormId());

			json.setSuccess("删除成功");
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "删除自定义操作", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/deleteDiy", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deleteDiy(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {


			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/dtform/deleteDiy", PFormAction.class.getName(), null,true,this.getService());
			
			PFormDiy obj=this.service.getMgClient().getById(id, PFormDiy.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PFormAction.class,id);
				return json.jsonValue();
			}
			
			
			PForm form=this.service.getMgClient().getById(obj.getFormId(), PForm.class);
			if(form==null) {
				json.setUnSuccessForNoRecord(PForm.class,obj.getFormId());
				return json.jsonValue();
			}
			

			this.service.getMgClient().deleteByPK(id, PFormDiy.class,ut,this.service);
			
			BosConstants.getExpireHash().remove(PForm.class, obj.getFormId());
			new ClassInnerNotice().invoke(PForm.class.getSimpleName(), obj.getFormId());

			json.setSuccess("删除成功");
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}
	
	
	

	@Security(accessType = "1", displayName = "启用", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/enable", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject enable(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/dtform/enable", PForm.class.getName(), null,true,this.getService());

			PForm obj=this.service.getMgClient().getById(id,PForm.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PForm.class,id);
				return json.jsonValue();
			}
			
			
			BosConstants.getExpireHash().remove(PForm.class, obj.getFormId());
			new ClassInnerNotice().invoke(PForm.class.getSimpleName(), obj.getFormId());
			
			obj.setStatus(1L);
			

			this.service.save(obj,ut);
			
			
			json.setSuccess("启用成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "清理基础数据菜单", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/clearMenu", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject clearMenu(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			
			//
			//BOSDYNCHART
			
			//Vector<String> v=this.service.getRdClient().getKeys("*"+RdClient.splitChar+"BOSDYMENU");
			//for(String x:v) {
				//this.service.getRdClient().remove(x);
			//}
			
			BosConstants.getExpireHash().removeMatch(RdClient.splitChar+"BOSDYMENU");
			new ClassInnerNotice().invoke(ExpiringMap.class.getSimpleName(), RdClient.splitChar+"BOSDYMENU");
			

			json.setSuccess("清理成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}
	
	
	
	@Security(accessType = "1", displayName = "字段的权限显示", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/fieldRoleShow", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fieldRoleShow(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/dtform/fieldRoleShow", PFormField.class.getName(), null,true,this.getService());
			
			PFormField obj=this.service.getMgClient().getById(id, PFormField.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PFormField.class,id);
				return json.jsonValue();
			}
			
			PForm  form=this.service.getMgClient().getById(obj.getFormId(), PForm.class);
			if(form==null) {
				json.setUnSuccessForNoRecord(PForm.class,obj.getFormId());
				return json.jsonValue();
			}
			
			PRoot root=this.service.getMgClient().getById(form.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,form.getSys());
				return json.jsonValue();
			}
			
			super.objToJson(obj, json);
			
			
			
			Table table=this.service.getMgClient().getTableById(form.getClassName());
			if(table==null) {
				json.setUnSuccessForNoRecord(Table.class,form.getClassName());
				return json.jsonValue();
			}
			
			
			if(true) {
				
				QueryListInfo<PRole> list=new QueryListInfo<PRole>();
				
				Vector<String> v=MyString.splitBy(obj.getEditRoles(), ",");
				

				for(String x:MyString.splitBy(table.getMaintainRole(), ",")) {
					PRole role=this.service.getMgClient().getById(x, PRole.class);
					if(role==null) continue;
					if(v.contains(role.getRoleId())) {
						role.setSelected();
					}
					list.getList().add(role);
				}
				
				super.listToJson(list, json, BosConstants.getTable(PRole.class));
				
			}
			
			
			
			if(true) {
				
				Vector<String> v=MyString.splitBy(obj.getVisibleRoles(), ",");
				
				QueryListInfo<PRole> list=new QueryListInfo<PRole>();
				
				for(String x:MyString.splitBy(table.getViewRoles(), ",")) {
					PRole role=this.service.getMgClient().getById(x, PRole.class);
					if(role==null) continue;
					if(v.contains(role.getRoleId())) {
						role.setSelected();
					}
					list.getList().add(role);
				}
				
				super.listToJson(list, json, "PRole1", BosConstants.getTable(PRole.class));
				
				
			}
			
			
			json.setSuccess();


		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}
	
	
	

	@Security(accessType = "1", displayName = "字段的可编辑权限保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/fieldRoleSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fieldRoleSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/dtform/fieldRoleShow", PFormField.class.getName(), null,true,this.getService());
			
			PFormField obj=this.service.getMgClient().getById(id, PFormField.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PFormField.class,id);
				return json.jsonValue();
			}
			
			
			String ids=json.getSimpleData("ids", "可编辑权限", String.class, false,this.getService());
			String ids1=json.getSimpleData("ids1", "可视权限", String.class, false,this.getService());
			

			//39|1,30|1,31|1
			obj.setEditRoles(ids);
			obj.addToMust("editRoles");
			obj.setVisibleRoles(ids1);
			obj.addToMust("visibleRoles");
			obj.setInnerEdit(null);
			obj.addToMust("innerEdit");
			obj.setInnerVisible(null);
			obj.addToMust("innerVisible");
			
			this.service.save(obj,ut);
			
			
			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}
	
	
	
	
	@Security(accessType = "1", displayName = "从表权限设置显示", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/slaveRoleShow", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject slaveRoleShow(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

	
			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/dtform/slaveRoleShow", PFormField.class.getName(), null,true,this.getService());
			
			PFormSlave obj=this.service.getMgClient().getById(id, PFormSlave.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PFormSlave.class,id);
				return json.jsonValue();
			}
			
			PForm  form=this.service.getMgClient().getById(obj.getFormId(), PForm.class);
			if(form==null) {
				json.setUnSuccessForNoRecord(PForm.class,obj.getFormId());
				return json.jsonValue();
			}
			
			PRoot root=this.service.getMgClient().getById(form.getSys(), PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,form.getSys());
				return json.jsonValue();
			}
			
			
			Table table=this.service.getMgClient().getTableById(form.getClassName());
			if(table==null) {
				json.setUnSuccessForNoRecord(Table.class,form.getClassName());
				return json.jsonValue();
			}
			
			
			super.objToJson(obj, json);
				
			
			if(true) {
				
				QueryListInfo<PRole> list=new QueryListInfo<PRole>();
				
				Vector<String> v=MyString.splitBy(obj.getAllowAdd(), ",");
				

				for(String x:MyString.splitBy(table.getMaintainRole(), ",")) {
					PRole role=this.service.getMgClient().getById(x, PRole.class);
					if(role==null) continue;
					if(v.contains(role.getRoleId())) {
						role.setSelected();
					}
					list.getList().add(role);
				}
				
				super.listToJson(list, json, BosConstants.getTable(PRole.class));
				
			}
			
			
			
			if(true) {
				
				Vector<String> v=MyString.splitBy(obj.getAllowDelete(), ",");
				
				QueryListInfo<PRole> list=new QueryListInfo<PRole>();
				
				for(String x:MyString.splitBy(table.getDeleteRole(), ",")) {
					PRole role=this.service.getMgClient().getById(x, PRole.class);
					if(role==null) continue;
					if(v.contains(role.getRoleId())) {
						role.setSelected();
					}
					list.getList().add(role);
				}
				
				super.listToJson(list, json, "PRole1", BosConstants.getTable(PRole.class));
				
			}
			
			
			if(true) {
				
				Vector<String> v=MyString.splitBy(obj.getAllowVisible(), ",");
				
				QueryListInfo<PRole> list=new QueryListInfo<PRole>();
				
				for(String x:MyString.splitBy(table.getViewRoles(), ",")) {
					PRole role=this.service.getMgClient().getById(x, PRole.class);
					if(role==null) continue;
					if(v.contains(role.getRoleId())) {
						role.setSelected();
					}
					list.getList().add(role);
				}
				
				super.listToJson(list, json, "PRole2", BosConstants.getTable(PRole.class));
				
			}
			
			
			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}
	
	
	

	@Security(accessType = "1", displayName = "从表权限设置保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/slaveRoleSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject slaveRoleSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/dtform/slaveRoleShow", PFormField.class.getName(), null,true,this.getService());
			
			PFormSlave obj=this.service.getMgClient().getById(id, PFormSlave.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PFormField.class,id);
				return json.jsonValue();
			}
			
			
			
			String ids=json.getSimpleData("ids", "新增权限", String.class, false,this.getService());
			String ids1=json.getSimpleData("ids1", "删除权限", String.class, false,this.getService());
			String ids2=json.getSimpleData("ids2", "可视权限", String.class, false,this.getService());
			
			obj.setAllowAdd(ids);
			obj.addToMust("allowAdd");
			obj.setAllowDelete(ids1);
			obj.addToMust("allowDelete");
			obj.setAllowVisible(ids2);
			obj.addToMust("allowVisible");
			
			
			
			
			obj.setInnerAdd(null);
			obj.addToMust("innerAdd");
			obj.setInnerVisible(null);
			obj.addToMust("innerVisible");
			obj.setInnerDelete(null);
			obj.addToMust("innerDelete");
			this.service.save(obj,ut);
			
			
			
			BosConstants.getExpireHash().remove(PForm.class, obj.getFormId());
			new ClassInnerNotice().invoke(PForm.class.getSimpleName(), obj.getFormId());
			
			json.setSuccess("保存成功");
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}
	
	
	@Security(accessType = "1", displayName = "禁用", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/disable", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject disable(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/dtform/disable", PForm.class.getName(), null,true,this.getService());

			PForm obj=this.service.getMgClient().getById(id,PForm.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PForm.class,id);
				return json.jsonValue();
			}
			
			
			obj.setStatus(0L);
			this.service.save(obj,ut);
			
			
			BosConstants.getExpireHash().remove(PForm.class, obj.getFormId());
			new ClassInnerNotice().invoke(PForm.class.getSimpleName(), obj.getFormId());
			
			json.setSuccess("禁用成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}
	
	
	
	@Security(accessType = "1", displayName = "行记录操作列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/rowActionList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject rowActionList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {
			

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, "/dtform/rowActionList", PForm.class.getName(), "", true,this.getService());
			
			PForm obj = this.service.getMgClient().getById(id, PForm.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PForm.class,id);
				return json.jsonValue();
			}
			

			PFormRowAction initAction=new PFormRowAction();
			initAction.setFormId(id);
			QueryListInfo<PFormRowAction> aList=this.service.getMgClient().getList(initAction, "sort",this.service);
			super.listToJson(aList, json, BosConstants.getTable(PFormRowAction.class.getName()));


			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}	
	
	
	
	
	
	
	@Security(accessType = "1", displayName = "行记录操作列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "rowActionListSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject rowActionListSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, "/dtform/rowActionList", PForm.class.getName(), "", true,this.getService());
			
			PForm obj = this.service.getMgClient().getById(id, PForm.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PForm.class,id);
				return json.jsonValue();
			}
			
			
			List<PFormRowAction> list=json.getList(PFormRowAction.class, "actionName,condition,masterFields",this.service);
			for(PFormRowAction x:list) {
				x.setClassName(obj.getClassName());
				this.service.save(x,ut);
			}
			
			
			BosConstants.getExpireHash().remove(PForm.class, obj.getFormId());
			new ClassInnerNotice().invoke(PForm.class.getSimpleName(), obj.getFormId());

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}	
	
	@Security(accessType = "1", displayName = "行记录操作删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/rowActionDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject rowActionDelete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, "/dtform/rowActionDelete", PFormRowAction.class.getName(), "", true,this.getService());
			
			PFormRowAction obj=this.service.getMgClient().getById(id, PFormRowAction.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PFormRowAction.class,id);
				return json.jsonValue();
			}
			
			this.service.getMgClient().deleteByPK(id, PFormRowAction.class,ut,this.service);
			
			
			BosConstants.getExpireHash().remove(PForm.class, obj.getFormId());
			new ClassInnerNotice().invoke(PForm.class.getSimpleName(), obj.getFormId());
			
			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}	
	
	
	
	@Security(accessType = "1", displayName = "行记录操作显示新增", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/rowActionShowAdd", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject rowActionShowAdd(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String formId=json.getSelectedId(Constants.current_sys, "/dtform/rowActionList", PForm.class.getName(), "", true,this.getService());
			
			
			PForm form = this.service.getMgClient().getById(formId, PForm.class);
			if (form == null) {
				json.setUnSuccessForNoRecord(PForm.class,formId);
				return json.jsonValue();
			}
			
			if(DataChange.isEmpty(form.getClassName())) {
				json.setUnSuccess(-1, "请设置模板的类对象");
				return json.jsonValue();
			}
			
			PFormRowAction obj=new PFormRowAction();
			obj.setActionId(PFormRowAction.next());
			obj.setFormId(formId);
			
			super.objToJson(obj, json);
			
		
			if(true) {
				
				SelectBidding data=new SelectBidding();
				PDiyUri  dpu=new PDiyUri();
				dpu.setClassName(form.getClassName());
				dpu.setExport(0L);
				QueryListInfo<PDiyUri> dpuList=this.service.getMgClient().getList(dpu,"uri",this.service);
				BosConstants.debug("PDiyUri   "+obj.getClassName()+"   export=1  size="+dpuList.size());
				for(PDiyUri x:dpuList.getList()) {
					data.put(x.getUri(), x.getUri()+"["+x.getDisplay()+"]");
				}
				super.selectToJson(data, json, PFormRowAction.class, "diyUri");
			}
			
			
			//查询字段列表
			Field sf = new Field();
			sf.setClassName(form.getClassName());
			sf.setRegType(1L);
			sf.setIsKey(0L);
			QueryListInfo<Field> fxList=this.service.getMgClient().getTableFieldList(sf, "property");
			super.listToJson(fxList, json, BosConstants.getTable(Field.class.getName()));
			
			
			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}	
	
	
	
	
	@Security(accessType = "1", displayName = "行记录操作详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/rowActionInfo", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject rowActionInfo(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {


			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, "/dtform/rowActionInfo", PFormRowAction.class.getName(), "", true,this.getService());
			
			
			PFormRowAction obj=this.service.getMgClient().getById(id, PFormRowAction.class);
			
			if(obj==null) {
				json.setUnSuccessForNoRecord(PFormRowAction.class,id);
				return json.jsonValue();
			}
			
			
			
			
			
			if(obj.getDiyService()==null) {
				obj.setDiyService(0L);
			}
			
			
			PForm form = this.service.getMgClient().getById(obj.getFormId(), PForm.class);
			if (form == null) {
				json.setUnSuccessForNoRecord(PForm.class,obj.getFormId());
				return json.jsonValue();
			}
			
			
			if(DataChange.isEmpty(form.getClassName())) {
				json.setUnSuccess(-1, "请设置模板的类对象");
				return json.jsonValue();
			}
			
			super.objToJson(obj, json);
			
			
			
			if(true) {
				SelectBidding data=new SelectBidding();
				PDiyUri  dpu=new PDiyUri();
				dpu.setClassName(form.getClassName());
				dpu.setExport(0L);
				QueryListInfo<PDiyUri> dpuList=this.service.getMgClient().getList(dpu,"uri",this.service);
				BosConstants.debug("PDiyUri   "+obj.getClassName()+"   export=1  size="+dpuList.size());
				for(PDiyUri x:dpuList.getList()) {
					data.put(x.getUri(), x.getUri()+"["+x.getDisplay()+"]");
				}
				super.selectToJson(data, json, PFormRowAction.class, "diyUri");
			}
			
			
			Field sf = new Field();
			sf.setClassName(form.getClassName());
			sf.setRegType(1L);
			sf.setIsKey(0L);
			QueryListInfo<Field> fxList=this.service.getMgClient().getTableFieldList(sf, "property");
			super.listToJson(fxList, json, BosConstants.getTable(Field.class.getName()));
			

			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();

	}	
	
	
	
	@Security(accessType = "1", displayName = "行记录操作单个保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/rowActionSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject rowActionSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String formId=json.getSelectedId(Constants.current_sys, "/dtform/rowActionList", PForm.class.getName(), "", true,this.getService());
			
			PForm form = this.service.getMgClient().getById(formId, PForm.class);
			if (form == null) {
				json.setUnSuccessForNoRecord(PForm.class,formId);
				return json.jsonValue();
			}
			
			
			PFormRowAction obj=json.getObj(PFormRowAction.class, "actionName,condition,masterFields,!initScript,!submitScript,!remark,!diyUri",this.service);
			
			if(DataChange.isEmpty(obj.getDiyUri())) {
				obj.setDiyService(0L);
			}else {
				obj.setDiyService(1L);
			}
			
			obj.setClassName(obj.getClassName());
			obj.setFormId(formId);
			
			this.service.save(obj,ut);
			
			
			BosConstants.getExpireHash().remove(PForm.class, obj.getFormId());
			new ClassInnerNotice().invoke(PForm.class.getSimpleName(), obj.getFormId());
			
			json.setSuccess("保存成功");

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

