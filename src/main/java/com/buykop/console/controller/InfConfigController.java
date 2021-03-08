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
import com.buykop.console.entity.product.Product;
import com.buykop.console.entity.product.ProductPage;
import com.buykop.console.entity.product.RPageInf;
import com.buykop.console.service.InfService;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.DiyInfField;
import com.buykop.framework.entity.DiyInfMsgPush;
import com.buykop.framework.entity.DiyInfOutField;
import com.buykop.framework.entity.DiyInfQuery;
import com.buykop.framework.entity.DiyInfQueryOperation;
import com.buykop.framework.entity.DiyInfSub;
import com.buykop.framework.entity.DiyInfSynField;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.FieldCalculationFormula;
import com.buykop.framework.scan.MQProcessing;
import com.buykop.framework.scan.MsgTemplate;
import com.buykop.framework.scan.PFormQueryOperation;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysCode;
import com.buykop.framework.scan.Statement;
import com.buykop.framework.scan.Table;
import com.buykop.framework.scan.ChartForm;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.query.BaseQuery;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;

@Module(display = "通用接口配置", sys = Constants.current_sys)
@RestController
public class InfConfigController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(InfConfigController.class);

	protected static final String URI = "/inf/config";

	@Autowired
	private InfService service;

	@Menu(js = "inf", name = "通用接口", trunk = "开发服务,接口管理")
	@Security(accessType = "1", displayName = "通用接口列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = URI + "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			DiyInf search = json.getSearch(DiyInf.class, null, ut, this.service);
			PageInfo page = json.getPageInfo(DiyInf.class);
			QueryFetchInfo<BosEntity> fetch = this.service.getMgClient().getFetch(search, "sys,className,code",
					page.getCurrentPage(), page.getPageSize(),this.service);
			fetch.initBiddingForSysClassName(this, json, "sys", "className", null);
			super.fetchToJson(fetch, json, BosConstants.getTable(DiyInf.class.getName()));
			//super.fetchToJson(fetch, json, DiyInf.class.getSimpleName(), "code,className,sys,title,jsonKey,infType,status,createUserId,userId", BosConstants.getTable(DiyInf.class.getName()));
			
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, DiyInf.class.getName(),
					"sys");

			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_tech, ut.getMemberId(), service), json,
					DiyInf.class.getName(), "userId");

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "通用接口列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech
			+ "," + BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			List<DiyInf> list = json.getList(DiyInf.class, "code,title,sys,className,status", this.service);
			for (DiyInf x : list) {
				x.setCode(x.getCode().toUpperCase());
				this.service.save(x, ut);
				DiyInf xo = this.service.getById(x.getCode(), DiyInf.class);
				if (xo!=null && DataChange.isEmpty(xo.getJsonKey())) {
					xo.setJsonKey(BosConstants.getTable(xo.getClassName()).getSimpleName());
					this.service.save(xo, ut);
				}
				
				CacheTools.removeDiyInf(x.getCode());
				
				new ClassInnerNotice().invoke(DiyInf.class.getSimpleName(), xo.getCode());
			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "通用接口对象", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech
			+ "," + BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/getObj", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject getObj(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/getObj", DiyInf.class.getName(), null, true,
					this.getService());
			DiyInf obj = this.service.getMgClient().getById(id, DiyInf.class);
			if (obj != null) {
				
				obj.setSimpleName(BosConstants.getTable(obj.getClassName()).getSimpleName());

				if (obj.getVisitor() == null) {
					obj.setVisitor(0);
				}
				if (obj.getPerson() == null) {
					obj.setPerson(0);
				}

				if (obj.getMemberAdmin() == null) {
					obj.setMemberAdmin(0);
				}

				if (obj.getSysAdmin() == null) {
					obj.setSysAdmin(0);
				}

				Table table = BosConstants.getTable(obj.getClassName());
				if (table == null) {
					json.setUnSuccessForNoRecord(Table.class,obj.getClassName());
					return json.jsonValue();
				}

				if (obj.getLimit() == null) {
					obj.setLimit(10);
				}

				if (DataChange.isEmpty(obj.getJsonKey())) {
					obj.setJsonKey(table.getSimpleName());
					this.service.save(obj, ut);
				}
				
			}

			

			super.objToJson(obj, json);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	

	@Security(accessType = "1", displayName = "通用接口详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech
			+ "," + BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/info", DiyInf.class.getName(), null, true,
					this.getService());
			DiyInf obj = this.service.getMgClient().getById(id, DiyInf.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(DiyInf.class,id);
				return json.jsonValue();
			}
			
			Table table = BosConstants.getTable(obj.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,obj.getClassName());
				return json.jsonValue();
			}
			
			
			obj.setSimpleName(table.getSimpleName());
			

			if (obj.getVisitor() == null) {
				obj.setVisitor(0);
			}
			if (obj.getPerson() == null) {
				obj.setPerson(0);
			}

			if (obj.getMemberAdmin() == null) {
				obj.setMemberAdmin(0);
			}

			if (obj.getSysAdmin() == null) {
				obj.setSysAdmin(0);
			}

		

			if (obj.getLimit() == null) {
				obj.setLimit(10);
			}

			if (DataChange.isEmpty(obj.getJsonKey())) {
				obj.setJsonKey(table.getSimpleName());
				this.service.save(obj, ut);
			}

			super.selectToJson(PSysCode.getForSelect("126", this.service), json, DiyInf.class.getSimpleName(),
					"dataPerType");
			super.selectToJson(Statement.getJsonForSelect(obj.getClassName(), this.service), json,
					DiyInf.class.getSimpleName(), "mapId");
			super.selectToJson(PRole.getJsonForSys(obj.getSys(), true, this.service), json, DiyInf.class, "roles");

			
			

			if (obj.getInfType().intValue() == 29) {
				super.selectToJson(MQProcessing.getJsonForSelect(obj.getClassName(), this.service), json, DiyInf.class,
						"mqId");
			}
			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_tech, ut.getMemberId(), service), json,
					DiyInf.class.getName(), "userId");

			super.selectToJson(Field.getNumberFieldJsonForSelect(obj.getClassName(), this.service), json,
					DiyInf.class.getSimpleName(), "tjProperty");

			if (DataChange.isEmpty(obj.getSys())) {
				json.setUnSuccess(-1, "请设置所属系统");
				return json.jsonValue();
			}

			if (DataChange.isEmpty(obj.getClassName())) {
				json.setUnSuccess(-1, "请设置绑定业务类");
				return json.jsonValue();
			}
			
			
			
			SelectBidding sbin = new SelectBidding();
			SelectBidding sbout = new SelectBidding();
			//SelectBidding sbQuery = new SelectBidding();
			
			
			
			HashMap<String,Field> fkFieldHash=new HashMap<String,Field>();
			
			
			if(true) {
				
				
				HashMap<String,Integer> opHash=new HashMap<String,Integer>();
				HashMap<String,DiyInfField> infHash=new HashMap<String,DiyInfField>();
				
				DiyInfQueryOperation qo=new DiyInfQueryOperation();
				qo.setId(id);
				QueryListInfo<DiyInfQueryOperation> list=this.service.getList(qo, "field");
				for(DiyInfQueryOperation x:list.getList()) {
					if(x.getQueryOperation()==null) continue;
					if(DataChange.isEmpty(x.getField())) continue;
					opHash.put(x.getField(), x.getQueryOperation());
				}
				
				DiyInfField dif = new DiyInfField();
				dif.setId(id);
				QueryListInfo<DiyInfField> dList = this.service.getList(dif, "field");
				for(DiyInfField x:dList.getList()) {
					infHash.put(x.getField(), x);
				}
				
				
				Vector<String> v = Field.split(obj.getInfFields());

				Vector<String> inputV = MyString.splitBy(obj.getFieldCheck(), ",");

				
				
				Field sf = new Field();
				sf.setClassName(obj.getClassName());
				sf.setCustomer(0L);
				QueryListInfo<Field> fList = this.service.getMgClient().getTableFieldList(sf,
						"!isKey,!propertyType,property");
				for (Field x : fList.getList()) {
					
					
					
					if (v.contains(x.getProperty())) {
						x.setSelected();
						if(!DataChange.isEmpty(x.getFkClasss())) {
							fkFieldHash.put(x.getProperty(), x);
						}
					}
					
					x.setQueryOperation(opHash.get(x.getProperty()));

					if (inputV.contains(x.getProperty())) {
						x.setInputCheck(1);
					} else if (inputV.contains("!" + x.getProperty())) {
						x.setInputCheck(-1);
					} else {
						x.setInputCheck(0);
					}
					
					
					DiyInfField inf=infHash.get(x.getProperty());
					if(inf!=null) {
						x.setValueBiddingJs(inf.getFormula());
						x.setValueBidding(inf.getValueBidding());
					}
					

					sbin.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "  "
							+ CacheTools.getSysCodeDisplay("5", String.valueOf(x.getPropertyType()), json.getLan()) + " ]");
					sbout.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "  "
							+ CacheTools.getSysCodeDisplay("5", String.valueOf(x.getPropertyType()), json.getLan()) + " ]");
					
					//if(x.judgeDB()) {
						//sbQuery.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "  "
								//+ CacheTools.getSysCodeDisplay("5", String.valueOf(x.getPropertyType()), json.getLan()) + " ]");
					//}
				}
				super.listToJson(fList, json, sf.showTable());
				
				
			}
			

			

			if (obj.getInfType() >= 20 && obj.getInfType() < 30) {
				
				super.selectToJson(MQProcessing.getJsonForSelect(obj.getClassName(), this.service), json, DiyInf.class,
						"mqId");
				
			}
			
			
			//if(true) {
				//PFormQueryOperation qo=new PFormQueryOperation();
				//qo.setId(id);
				//QueryListInfo<PFormQueryOperation> list=this.service.getList(qo, "field");
				//super.listToJson(list, json, qo.showTable());
				//super.selectToJson(sbQuery, json, PFormQueryOperation.class, "field");
			//}
			
			
			/**if(true) {
				DiyInfField dif = new DiyInfField();
				dif.setId(id);
				QueryListInfo<DiyInfField> dList = this.service.getList(dif, "field");
				super.listToJson(dList, json, dif.showTable());
				super.selectToJson(sbin, json, DiyInfField.class, "field");
			}*/
				
			// 0:列表  1:翻页 2:树形   3:统计列表  11:单个(主键)  12:单个(条件) 15:单个不存在提示  17:判断单个对象已存在提示 
			if(obj.getInfType() >= 0 && obj.getInfType() < 20) {
				DiyInfOutField dif = new DiyInfOutField();
				dif.setId(id);
				QueryListInfo<DiyInfOutField> dList = this.service.getList(dif, "field");
				for(DiyInfOutField x:dList.getList()) {
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
				
				Iterator<String> its=fkFieldHash.keySet().iterator();
				while(its.hasNext()) {
					String f=its.next();
					Field field=fkFieldHash.get(f);
					
					DiyInfOutField added=new DiyInfOutField();
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
				}
				
				super.listToJson(dList, json, dif.showTable());
				super.selectToJson(sbout, json, DiyInfOutField.class, "field");
				
				
				
				
			}
			
			
			if((obj.getInfType() >= 0 && obj.getInfType() < 10 ) ||  obj.getInfType()==23 ||  obj.getInfType()==31   ) {//
				DiyInfQuery bq=new DiyInfQuery();
				bq.setCode(id);
				QueryListInfo<DiyInfQuery> dList=service.getList(bq, "seq");
				super.listToJson(dList, json, bq.showTable());
			}
			
			
			if(true) {
				
				DiyInfMsgPush bq=new DiyInfMsgPush();
				bq.setId(id);
				QueryListInfo<DiyInfQuery> dList=service.getList(bq, "sort");
				super.listToJson(dList, json, bq.showTable());
				super.selectToJson(MsgTemplate.getJsonForSelect(obj.getClassName(),this.service), json, DiyInfMsgPush.class, "templateId");
				
			}
			
			
			
			if(true) {
				
				HashMap<String,DiyInfSynField> hash=new HashMap<String,DiyInfSynField>();
				
				if(true) {
					DiyInfSynField synField=new DiyInfSynField();
					synField.setId(id);
					QueryListInfo<DiyInfSynField> subList = this.service.getList(synField, "field");
					for(DiyInfSynField x:subList.getList()) {
						hash.put(x.getField(), x);
					}
				}
				
				
				QueryListInfo<DiyInfSynField> subList =new QueryListInfo<DiyInfSynField>();
				
				//拉出有外键的属性
				Field ff=new Field();
				ff.setClassName(obj.getClassName());
				ff.setCustomer(0L);
				ff.setPropertyType(1L);
				QueryListInfo<Field> fxList=this.service.getList(ff, "property");
				for(Field x:fxList.getList()) {
					if(DataChange.isEmpty(x.getFkClasss())) continue;

					//检查一下外键表是否存在计算公司
					FieldCalculationFormula fcf=new FieldCalculationFormula();
					fcf.setfClassName(x.getFkClasss());
					long count=this.service.getCount(fcf);
					if(count==0) continue;
					
					DiyInfSynField synField=hash.get(x.getProperty());
					if(synField==null) {
						synField=new DiyInfSynField();
						synField.setId(id);
						synField.setDisplay(x.getDisplay());
						synField.setField(x.getProperty());
						//synField.setStatus(0);
					}
					synField.setDisplay(x.getDisplay());
					subList.getList().add(synField);
					
				}
				
				super.listToJson(subList, json, BosConstants.getTable(DiyInfSynField.class));
				
			}
			

			DiyInfSub sub = new DiyInfSub();
			sub.setCode(id);
			QueryListInfo<DiyInfSub> subList = this.service.getList(sub, "seq");
			for(DiyInfSub x:subList.getList()) {
				
				if(DataChange.isEmpty(x.getSubClass())) continue;
				Table subtable=BosConstants.getTable(x.getSubClass());
				if(subtable==null) continue;
				
				SelectBidding sb=new SelectBidding();
				for(Field fx:subtable.getFields()) {
					if(!fx.judgeDB()) continue;
					sb.put(fx.getProperty(), fx.getDisplay()+"["+fx.getProperty()+"]");
					
				}
				super.selectToJson2(sb, json, x, "infFields");
			}
			
			super.listToJson(subList, json, sub.showTable());
			super.selectToJson(Table.getFKJsonForSelect(table.getSys(), table.getClassName(), null, service), json,
					DiyInfSub.class, "subClass");

			super.objToJson(obj, json);
			
			
			boolean allow=false;
			if(ut.getUserId().equals(obj.getUserId())) {
				allow=true;
			}
			
			if(ut.getUserId().equals("1")) {
				allow=true;
			}
			
			
			//判断是佛可以修改 userId=1 or 产品的开发leader 本人
			if(!allow) {
				Vector<String> prv=new Vector<String>();
				RPageInf pi=new RPageInf();
				pi.setInfCode(id);
				Vector<String> pageV=this.service.getVector(pi, "pageId");
				for(String page:pageV) {
					ProductPage pp=this.service.getById(page, ProductPage.class);
					if(pp==null || DataChange.isEmpty(pp.getProductId())) {
						continue;
					}
					if(prv.contains(pp.getProductId())) continue;
					prv.add(pp.getProductId());
				}
				if(prv.size()>0) {
					Product ps=new Product();
					BaseQuery bq=new BaseQuery();
					bq.setFields("productId");
					bq.setType(1);
					bq.setValue(MyString.CombinationBy(prv, ","));
					ps.getQueryList().add(bq);
					prv=this.service.getVector(ps, "leaderId");
					if(prv.contains(ut.getUserId())) {
						allow=true;
					}
				}	
			}
			
			
			if(allow) {
				json.getData().put("allow", 1);
			}else {
				json.getData().put("allow",0);
			}
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "通通用接口删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech
			+ "," + BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/delete", DiyInf.class.getName(), null, true,
					this.getService());
			DiyInf obj = this.service.getMgClient().getById(id, DiyInf.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(DiyInf.class,id);
				return json.jsonValue();
			}

			this.service.deleteById(id, DiyInf.class.getName(), ut);
			
			
			RPageInf rpi=new RPageInf();
			rpi.setInfCode(id);
			this.service.delete(rpi, ut, true);
			
			CacheTools.removeDiyInf(id);
			
			new ClassInnerNotice().invoke(DiyInf.class.getSimpleName(), id);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "通用接口单个保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech
			+ "," + BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			DiyInf obj = json.getObj(DiyInf.class, "sys,className", this.service);

			Vector<String> ids = json.showIds();
			for (int i = 0; i < ids.size(); i++) {
				if (ids.get(i).indexOf(BosEntity.keySplit) != -1) {
					ids.set(i, ids.get(i).substring(ids.get(i).indexOf(BosEntity.keySplit) + 1, ids.get(i).length()));
				}
			}
			
			
			Table table=BosConstants.getTable(obj.getClassName());

			obj.setInfFields(MyString.CombinationBy(ids, ","));

			if (true) {
				
				StringBuffer sb = new StringBuffer();
				List<Field> fList = json.getList(Field.class, null, service);
				
				for (Field x : fList) {
					if(!DataChange.isEmpty(x.getValueBidding()) && !DataChange.isEmpty(x.getValueBiddingJs())) {
						json.setUnSuccess(-1, x.getProperty()+":输入绑定(js返回字符串) 及  输入绑定(常量) 只能二选一");
						return json.jsonValue();
					}
					
					if(DataChange.replaceNull(x.getValueBiddingJs()).indexOf("${json")!=-1) {
						json.setUnSuccess(-1, x.getProperty()+":输入绑定(js返回字符串)不能含有${json");
						return json.jsonValue();
					}
					
					if(DataChange.replaceNull(x.getValueBidding()).indexOf("${json")!=-1) {
						json.setUnSuccess(-1, x.getProperty()+":输入绑定(常量)不能含有${json");
						return json.jsonValue();
					}
					
				}
				
				
				
				DiyInfQueryOperation qo=new DiyInfQueryOperation();
				qo.setId(obj.getCode());
				this.service.delete(qo, ut);
				
				
				
				DiyInfField dif = new DiyInfField();
				dif.setId(obj.getCode());
				this.service.delete(dif, ut,true);
				
				
				long sort=0;
				for (Field x : fList) {
					
					if(DataChange.getIntValueWithDefault(x.getInputCheck() , 0)!=0) {
						
						if (x.getInputCheck() == 1) {
							sb.append(x.getProperty() + ",");
						} else if (x.getInputCheck() == -1) {
							sb.append("!" + x.getProperty() + ",");
						}
					}
					
					
					Field fx=table.getDBField(x.getProperty());
					if(fx==null) continue;
					
					
					if(x.getQueryOperation()!=null) {
						qo=new DiyInfQueryOperation();
						qo.setId(obj.getCode());
						qo.setField(x.getProperty());
						qo.setQueryOperation(x.getQueryOperation());
						qo.addToMust("queryOperation");
						this.service.save(qo, ut);
					}
					
					
					if(!DataChange.isEmpty(x.getValueBidding()) || !DataChange.isEmpty(x.getValueBiddingJs()) ) {
						dif = new DiyInfField();
						dif.setId(obj.getCode());
						dif.setField(x.getProperty());
						dif.setFormula(x.getValueBiddingJs());
						dif.addToMust("formula");
						dif.setValueBidding(x.getValueBidding());
						dif.addToMust("valueBidding");
						dif.setSort(sort++);
						service.save(dif, ut);
					}
					
					
					
				}

				String ret = sb.toString();
				if (ret.length() > 0) {
					ret = ret.substring(0, ret.length() - 1);
				}
				obj.setFieldCheck(ret);
				obj.addToMust("fieldCheck");
			}

			this.service.save(obj, ut);

			
			
			
			/**if (true) {//obj.getInfType() >= 20 && obj.getInfType() < 30
				DiyInfField dif = new DiyInfField();
				dif.setId(obj.getCode());
				this.service.delete(dif, ut,true);
				long seq=0;
				List<DiyInfField> fList = json.getList(DiyInfField.class, "field,formula", service);
				for (DiyInfField x : fList) {
					if(x.getFormula().indexOf("${json")!=-1) {
						json.setUnSuccess(-1, "输入绑定js脚本不能含有${json");
						return json.jsonValue();
					}
					x.setId(obj.getCode());
					x.setSort(seq++);
					this.service.save(x, ut);
				}
			}*/
			
			
			if(true) {
				
				DiyInfMsgPush bq=new DiyInfMsgPush();
				bq.setId(obj.getCode());
				this.service.delete(bq, ut,true);
				List<DiyInfMsgPush> fList = json.getList(DiyInfMsgPush.class, "templateId,msgType", service);
				long seq=0;
				for (DiyInfMsgPush x : fList) {
					x.setId(obj.getCode());
					x.setSort(seq++);
					this.service.save(x, ut);
				}
			}
			
			
			if (true) {
				DiyInfOutField dif = new DiyInfOutField();
				dif.setId(obj.getCode());
				this.service.delete(dif, ut,true);
				long seq=0;
				List<DiyInfOutField> fList = json.getList(DiyInfOutField.class, "field,!formula,!fkRedisFields", service);
				for (DiyInfOutField x : fList) {
					
					if(DataChange.isEmpty(x.getFormula()) &&  DataChange.isEmpty(x.getFkRedisFields())) {
						continue;
					}
					
					if(!DataChange.isEmpty(x.getFormula()) && x.getFormula().indexOf("${json")!=-1) {
						json.setUnSuccess(-1, "输出绑定js脚本不能含有${json");
						return json.jsonValue();
					}
					x.setId(obj.getCode());
					x.setSort(seq++);
					
					this.service.save(x, ut);
				}
			}
			
			if (true) {
				DiyInfQuery bq=new DiyInfQuery();
				bq.setCode(obj.getCode());
				this.service.delete(bq,ut,true);
				long seq=0;
				List<DiyInfQuery> fList = json.getList(DiyInfQuery.class, "fields,value,type", service);
				for (DiyInfQuery x : fList) {
					
					Vector<String> fv=Field.split(x.getFields());
					for(String x1:fv) {
						if(table.getDBField(x1)==null) {
							json.setUnSuccess(-1, "属性:"+x.getFields()+",但"+x1+"不存在");
							return json.jsonValue();
						}
					}
					
					if(x.getType().intValue()==3) {
						Vector<String> vv=Field.split(x.getValue());
						if(fv.size()!=vv.size()) {
							json.setUnSuccess(-1, "属性:"+x.getFields()+"及属性值个数不一致");
							return json.jsonValue();
						}
						
					}
					
					if(x.getValue().indexOf("${json")!=-1) {
						json.setUnSuccess(-1, "属性:"+x.getFields()+" value不能含有${json");
						return json.jsonValue();
					}
					
					x.setCode(obj.getCode());
					x.setSeq(seq++);
					this.service.save(x, ut);
				}
			}
			
			
			
			if(true) {
				
				DiyInfSub del = new DiyInfSub();
				del.setCode(obj.getCode());
				this.service.delete(del, ut);

				long seq = 0;
				List<DiyInfSub> subList = json.getList(DiyInfSub.class, "subClass", service);
				for (DiyInfSub x : subList) {
					x.setSeq(seq++);
					x.setCode(obj.getCode());
					x.setSys(obj.getSys());
					this.service.save(x, ut);
				}
				
			}
			
			
			if(true) {
				DiyInfSynField synField=new DiyInfSynField();
				synField.setId(obj.getCode());
				this.service.delete(synField, ut);
				List<DiyInfSynField> subList = json.getList(DiyInfSynField.class, "field,!activeCheck", service);
				for(DiyInfSynField x:subList) {
					x.setId(obj.getCode());
					this.service.save(x, ut);
				}
			}
			
			
			
			CacheTools.removeDiyInf(obj.getCode());
			
			new ClassInnerNotice().invoke(DiyInf.class.getSimpleName(), obj.getCode());

			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	@Override
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}

}
