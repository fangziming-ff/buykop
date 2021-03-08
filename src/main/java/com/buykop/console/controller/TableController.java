package com.buykop.console.controller;

import java.math.BigDecimal;
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

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.TableService;
import com.buykop.console.thread.util.SynToEs;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.BizFieldModifyTrack;
import com.buykop.framework.entity.FileUpload;
import com.buykop.framework.entity.HotKeyWord;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.mysql.Import;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.DBIndexExec;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.FieldCalculationFormula;
import com.buykop.framework.scan.Index;
import com.buykop.framework.scan.PClob;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PFormField;
import com.buykop.framework.scan.InputCheck;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.LableLanDisplay;
import com.buykop.framework.scan.MQProcessing;
import com.buykop.framework.scan.PLanguage;
import com.buykop.framework.scan.PMapTJConfig;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysCodeType;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.SeqValue;
import com.buykop.framework.scan.Statement;
import com.buykop.framework.scan.Table;
import com.buykop.framework.scan.TableESConfig;
import com.buykop.framework.scan.TableESPropertyConfig;
import com.buykop.framework.scan.TableRedicSubConfig;
import com.buykop.framework.scan.Where;
import com.buykop.framework.scan.WhereGroup;
import com.buykop.framework.scan.WhereOptimize;
import com.buykop.framework.thread.SynToRedisJson;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.AddressJson;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.JsonUtil;
import com.buykop.framework.util.type.BaseChartEntity;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.ListData;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;
import com.buykop.console.util.Constants;


@Module(display = "数据表", sys = Constants.current_sys)
@RestController
@RequestMapping(TableController.URI)
public class TableController extends BaseController {

	protected final static String URI = "/table";

	private static Logger logger = LoggerFactory.getLogger(TableController.class);

	@Autowired
	private TableService service;

	@Security(accessType = "1", displayName = "服务节点上数据缓存列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dataCacheClear", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dataCacheClear(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/dataCacheClear", Table.class.getName(),
					null, true,this.getService());
			Table obj = this.service.getMgClient().getTableById(id);
			if (obj == null) {
				json.setUnSuccessForNoRecord(Table.class, id);
				return json.jsonValue();
			}

			new ClassInnerNotice().invoke(ListData.class.getSimpleName(), id);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	

	@Security(accessType = "1", displayName = "字段外键设置", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/fieldFK", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fieldFK(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String fieldId = json.getSelectedId(Constants.current_sys, "/table/fieldFK", Field.class.getName(), null,
					true,this.getService());

			Field obj = this.service.getMgClient().getById(fieldId, Field.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(Field.class,fieldId);
				return json.jsonValue();
			}

			if (!DataChange.isEmpty(obj.getFkClasss())) {
				Table fk = this.service.getMgClient().getTableById(obj.getFkClasss());
				if (fk != null) {
					obj.setSys(fk.getSys());
				}
			}
			
			if(obj.getCaseSensitive()==null) {
				obj.setCaseSensitive(0);
			}

			super.selectToJson(Field.getJsonForSelectWithFK(obj.getClassName(), 1L, null, this.service), json,
					Field.class, "pbFileSrcProperty");

			super.selectToJson(PSysCodeType.getJsonForSelect(ut.getMemberId(), this.service), json,
					Field.class.getSimpleName(), "codeType");

			super.selectToJson(Field.getJsonForParentSelect(obj.getClassName(), obj.getValueClass(), this.service),
					json, Field.class, "parentField");

			super.selectToJson(InputCheck.getJsonForSelect(this.service), json, Field.class.getSimpleName(), "check");

			super.objToJson(obj, json);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "字段外键设置", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/fieldFKSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fieldFKSave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		// Constants.debug("***************1\n"+json.stringValue());
		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			Field obj = json.getObj(Field.class,
					"className,property,!sys,!fkClasss,!pbFileSrcProperty,!codeType,encryption,!label", this.service);

			Field src = this.service.getMgClient().getById(obj.getPk(), Field.class);
			if (src == null) {
				json.setUnSuccessForNoRecord(Field.class,obj.getPk());
				return json.jsonValue();
			}

			Table table = this.service.getMgClient().getTableById(obj.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,obj.getClassName());
				return json.jsonValue();
			}

			if (src.getIsKey() != null && src.getIsKey().longValue() == 0 && !DataChange.isEmpty(obj.getFkClasss())
					&& obj.getFkClasss().endsWith(obj.getClassName())) {
				table.setTreeParentField(obj.getProperty());
				this.service.save(table, ut);
			}

			if (!DataChange.replaceNull(obj.getFkClasss()).equals(FileUpload.class.getName())) {
				obj.setPbFileSrcProperty(null);
			}

			// 设为分组的只能是外键或者数据字典
			if (obj.getGroup() != null && obj.getGroup().intValue() == 1) {
				if (!DataChange.isEmpty(obj.getFkClasss())) {
					// 暂时不支持树形结构作为分组树形 关联 PlaceSummaryController.currentListWithOIS
					Table fkTable = BosConstants.getTable(obj.getFkClasss());
					if (fkTable != null && !DataChange.isEmpty(fkTable.getTreeParentField())) {
						json.setUnSuccess(-1, "外键对象为树形结构,暂时不支持设置为分组字段");
						return json.jsonValue();
					}
				} else if (obj.getCodeType() != null) {

				} else {
					json.setUnSuccess(-1, "分组字段必须设置为具有外键或数据字典的字段");
					return json.jsonValue();
				}
			} else {
				obj.setGroup(0L);
			}

			this.service.save(obj,ut);

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "实体类与数据库字段比对", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/fieldComparison", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fieldComparison(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/fieldComparison",
					Table.class.getName(), null, true,this.getService());
			Table obj = this.service.getMgClient().getTableById(className);
			if (obj == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			super.objToJson(obj, json);

			QueryListInfo<Field> list = new QueryListInfo<Field>();

			HashMap<String, Field> fieldHash = new HashMap<String, Field>();

			Field field = new Field();
			field.setClassName(className);
			field.setPropertyType(1L);
			QueryListInfo<Field> fList = this.service.getMgClient().getList(field,this.service);
			for (Field x : fList.getList()) {
				fieldHash.put(x.getProperty(), x);
			}

			Import importDB = new Import(this.service.getBaseDao());
			List<Field> dbFieldList = importDB.getFieldList(obj);
			for (Field x : dbFieldList) {
				x.setClassName(className);
				Field cf = fieldHash.get(x.getProperty());
				if (cf == null)
					continue;
				x.setComparisonLen(cf.getDbLen());
				if (cf.getValueClass().equals(Date.class.getName())) {
					x.setComparisonLen(null);
				}
				x.setComparisonValueClass(cf.getValueClass());
				fieldHash.remove(x.getProperty());
			}

			Iterator<String> its = fieldHash.keySet().iterator();
			while (its.hasNext()) {
				dbFieldList.add(fieldHash.get(its.next()));
			}

			for (Field x : dbFieldList) {
				list.getList().add(x);
			}

			super.listToJsonForChart(list, json, Field.class.getSimpleName(),
					"className,property,valueClass,dbLen,comparisonLen,comparisonValueClass,pk",
					BosConstants.getTable(Field.class),false);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "删除数据库字段", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/deleteFieldFromDB", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deleteFieldFromDB(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/deleteFieldFromDB", Field.class.getName(),
					null, true,this.getService());
			Field obj = this.service.getMgClient().getById(id, Field.class);
			if (obj != null && DataChange.getLongValueWithDefault(obj.getPropertyType(), 0) == 1) {
				json.setUnSuccess(-1, "实体类的属性不能删除");
				return json.jsonValue();
			}

			obj = new Field();
			obj.setPK(id);

			if (DataChange.isEmpty(obj.getClassName()) || DataChange.isEmpty(obj.getProperty())) {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}

			Table table = this.service.getMgClient().getTableById(obj.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,obj.getClassName());
				return json.jsonValue();
			}

			Import importDB = new Import(this.service.getBaseDao());
			importDB.dropField(table.getSys(), table.getCode(), obj.getProperty());

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "表对象详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/hotList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject hotList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, URI + "/hotList", Table.class.getName(),
					null, true,this.getService());

			PageInfo page = json.getPageInfo(HotKeyWord.class);

			HotKeyWord search = new HotKeyWord();
			search.setClassName(className);
			QueryFetchInfo<HotKeyWord> list = this.service.getMgClient().getFetch(search, "!searchNum",
					page.getCurrentPage(), page.getPageSize(),this.service);
			super.fetchToJson(list, json, BosConstants.getTable(HotKeyWord.class));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "热搜列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/hotListSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject hotListSave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, URI + "/hotList", Table.class.getName(),
					null, true,this.getService());

			List<HotKeyWord> list = json.getList(HotKeyWord.class, null, this.service);
			for (HotKeyWord x : list) {
				x.setClassName(className);
				this.service.save(x,ut);
			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "表对象详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/info", Table.class.getName(), null,
					true,this.getService());

			Table obj = this.service.getMgClient().getTableById(className);
			if (obj == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			obj.setSyn(0L);

			// Constants.debug("isChart="+obj.getIsChart());

			PRoot root = this.service.getMgClient().getById(obj.getSys(), PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, obj.getSys());
				return json.jsonValue();
			}

			super.objToJson(obj, json);
			super.selectToJson(PForm.getJsonForSelect(ut.getMemberId(), className, 1L, this.service), json, Table.class,
					"formId");

			super.selectToJson(Table.getTreeParentFieldJsonForSelect(className, this.service), json, Table.class,
					"treeParentField");
			super.selectToJson(Field.getJsonForSelectOwnerFieldWithFK(className, BosConstants.userClassName,1L, this.service), json,
					Table.class, "ownerUserIdField");
			super.selectToJson(Field.getJsonForSelectOwnerFieldWithFK(className, BosConstants.orgClassName,1L, this.service), json,
					Table.class, "ownerOrgIdField");
			super.selectToJson(Field.getJsonForSelectOwnerFieldWithFK(className, BosConstants.memberClassName,1L, this.service),
					json, Table.class, "ownerMemberIdField");
			super.selectToJson(Field.getJsonForSelectOwnerFieldWithFK(className, BosConstants.memberClassName,1L, this.service),
					json, Table.class, "ownerOisIdField");
			
			
			super.selectToJson(Field.getJsonForSelectViewNum(className, this.service), json, Table.class,
					"clickNumField");
			super.selectToJson(Field.getJsonForSelectViewNum(className, this.service), json, Table.class,
					"recommendField");

			// SelectBidding roleSB=PRole.getJsonForSys(obj.getSys(),true,this.service);
			// System.out.println(roleSB.showJSON().toJSONString());
			super.selectToJson(PRole.getJsonForSys(obj.getSys(), true, this.service), json, Table.class,
					"maintainRole");
			super.selectToJson(PRole.getJsonForSys(obj.getSys(), true, this.service), json, Table.class, "deleteRole");
			super.selectToJson(PRole.getJsonForSys(obj.getSys(), true, this.service), json, Table.class, "viewRoles");
			super.selectToJson(Table.getMasterJsonForSelect(root.getCode(), className, 0L, this.service), json,
					Table.class, "master");
			// super.selectToJson(cn.powerbos.framework.util.Util.getIcoJson(Table.class.getName(),
			// obj.getClassName()), json, Table.class.getSimpleName(), "icoId");

			Vector<String> uniqueV = new Vector<String>();

			Index index = new Index();
			index.setClassName(obj.getClassName());
			QueryListInfo<Index> idxList = this.service.getMgClient().getTableIndexList(index, "seq");
			for (Index x : idxList.getList()) {
				if (DataChange.getLongValueWithDefault(x.getUnique(), 0) != 1)
					continue;
				String fs = x.getFields();
				if (DataChange.isEmpty(fs))
					continue;
				if (fs.indexOf(",") == -1) {
					uniqueV.add(fs);
				}

			}
			super.listToJson(idxList, json, BosConstants.getTable(Index.class));

			SelectBidding disSB = new SelectBidding();
			SelectBidding disSC = new SelectBidding();
			SelectBidding seqSB = new SelectBidding();
			SelectBidding diyListSB = new SelectBidding();
			SelectBidding trackListSB = new SelectBidding();
			SelectBidding groupSB = new SelectBidding();
			SelectBidding hotSearchListSB = new SelectBidding();

			PFormField pf = new PFormField();
			pf.setClassName(obj.getClassName());
			Vector<String> pfV = this.service.getMgClient().getVector(pf, "property",this.service);

			Vector<String> chartV = Field.split(BaseChartEntity.fieldsChart);
			Vector<String> mapV = Field.split(BaseChartEntity.fieldsMap);
			Vector<String> ownerV = Field.split(BaseChartEntity.fieldsOwner);

			Field field = new Field();
			field.setClassName(obj.getClassName());
			field.setCustomer(0L);
			QueryListInfo<Field> fList = this.service.getMgClient().getTableFieldList(field,
					"!isKey,!propertyType,property");
			for (Field x : fList.getList()) {

				// 扩展字段不需要标记是否在模板里面
				if (!BosConstants._sysV.contains(obj.getSys()) && !pfV.contains(x.getProperty())
						&& DataChange.getLongValueWithDefault(x.getPropertyType(), 0) == 1) {
					if (!chartV.contains(x.getProperty()) && !mapV.contains(x.getProperty())
							&& !ownerV.contains(x.getProperty())) {
						x.setNotInTemplate(1L);
					}

				} // 手工的系统,不在模板中的属性，标注一下，方便用户判断是否可以删除该属性

				// if(DataChange.getLongValueWithDefault(x.getPropertyType(), 0).intValue()==1)
				// {
				diyListSB.put(x.getProperty(),
						x.getDisplay() + "[" + x.getProperty() + "]   "
								+ CacheTools.getSysCodeDisplay("5",
										String.valueOf(DataChange.getLongValueWithDefault(x.getPropertyType(), 0)),
										json.getLan()));
				// }
				if (DataChange.getLongValueWithDefault(x.getPropertyType(), 0) == 1) {
					trackListSB.put(x.getProperty(),
							x.getDisplay() + "[" + x.getProperty() + "]   "
									+ CacheTools.getSysCodeDisplay("5",
											String.valueOf(DataChange.getLongValueWithDefault(x.getPropertyType(), 0)),
											json.getLan()));
				}

				if ( DataChange.getLongValueWithDefault(x.getPropertyType(), 0) == 1 && x.getValueClass().equals(String.class.getName())
						&& (DataChange.getLongValueWithDefault(x.getIsLike(), 0) > 0
								|| !DataChange.isEmpty(x.getFkClasss()) || !DataChange.isEmpty(x.getCodeType()))) {
					hotSearchListSB.put(x.getProperty(),
							x.getDisplay() + "[" + x.getProperty() + "]   "
									+ CacheTools.getSysCodeDisplay("5",
											String.valueOf(DataChange.getLongValueWithDefault(x.getPropertyType(), 0)),
											json.getLan()));
				}

				// if(DataChange.getLongValueWithDefault(x.getPropertyType(), 0).intValue()==1
				// && uniqueV.contains(x.getProperty())) {
				// uniqueSB.put(x.getProperty(), x.getDisplay()+"["+x.getProperty()+"]");
				// }

				if (DataChange.getLongValueWithDefault(x.getGroup(), 0) == 1) {
					groupSB.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "]");
				}

				if (DataChange.getLongValueWithDefault(x.getPropertyType(), 0) == 1
						&& x.getValueClass().equals(String.class.getName())) {
					disSB.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "]");
					disSC.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "]");
				}
				if (DataChange.getLongValueWithDefault(x.getPropertyType(), 0) == 1
						&& x.getValueClass().equals(Long.class.getName())) {
					seqSB.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "]");
				}
			}

			super.selectToJson(seqSB, json, Table.class, "seqField");
			super.selectToJson(disSB, json, Table.class, "disField");
			super.selectToJson(disSC, json, Table.class, "codeField");
			super.selectToJson(groupSB, json, Table.class, "groupField");// 目录分组树形
			// super.selectToJson(uniqueSB, json, Table.class, "uniqueField");
			super.selectToJson(trackListSB, json, Table.class, "trackFields");
			

			super.listToJson(fList, json, BosConstants.getTable(Field.class));

			super.selectToJson(InputCheck.getJsonForSelect(this.service), json, Field.class.getSimpleName(), "check");

			Statement stm = new Statement();
			stm.setClassName(obj.getClassName());
			QueryListInfo<Statement> stmList = this.service.getMgClient().getTableStatementList(stm, "id");
			super.listToJson(stmList, json, BosConstants.getTable(Statement.class));

			WhereGroup wg = new WhereGroup();
			wg.setClassName(obj.getClassName());
			QueryListInfo<WhereGroup> wgList = this.service.getMgClient().getTableWhereGroupList(wg, null);
			super.listToJson(wgList, json, BosConstants.getTable(WhereGroup.class));
			
			if(true) {
				WhereOptimize wo=new WhereOptimize();
				wo.setClassName(obj.getClassName());
				QueryListInfo<WhereOptimize> woList = this.service.getMgClient().getList(wo,"!type,fields", this.service);
				super.listToJson(woList, json, BosConstants.getTable(WhereOptimize.class));
			}
			
			//设置redis缓存的从表
			if(true) {
				
				TableRedicSubConfig rsc=new TableRedicSubConfig();
				rsc.setTable(obj.getClassName());
				QueryListInfo<TableRedicSubConfig> rscList=service.getList(rsc, "subClass");
				
				HashMap<String, Field> fkHash=Table.getFKHash(obj.getSys(), obj.getClassName(),this.service);
				
				BosConstants.debug("sys="+obj.getSys()+"   classname="+obj.getClassName()+"  fkhash size1="+fkHash.size());
				
				SelectBidding fkB = new SelectBidding();
				Iterator<String> its=fkHash.keySet().iterator();
				while(its.hasNext()) {
					String cl=its.next();
					Table fkt=BosConstants.getTable(cl);
					if(fkt==null) continue;
					fkB.put(cl, fkt.getDisplayName()+"["+fkt.getSimpleName()+"]");
				}
				
				
				BosConstants.debug("sys="+obj.getSys()+"   classname="+obj.getClassName()+"  fkhash size2="+fkB.size());
				
				Vector<TableRedicSubConfig> delV=new Vector<TableRedicSubConfig>();
				
				for(TableRedicSubConfig x:rscList.getList()) {
					if(!fkHash.containsKey(x.getSubClass())) {
						delV.add(x);
					}
					
					Field fx=new Field();
					fx.setClassName(x.getSubClass());
					fx.setFkClasss(obj.getClassName());
					QueryListInfo<Field> fxList=this.service.getList(fx, "property");
					SelectBidding fxb = new SelectBidding();
					for(Field x1:fxList.getList()) {
						fxb.put(x1.getProperty(), x1.getDisplay()+"["+x1.getProperty()+"]");
					}
					//加入二级
					super.selectToJson2(fxb, json, x, "subField");
				}
				
				for(TableRedicSubConfig x:delV) {
					rscList.getList().remove(x);
					this.service.deleteById(x.getPk(), TableRedicSubConfig.class.getName(), ut,true);
				}
				
				super.listToJson(rscList, json, rsc.showTable());
				super.selectToJson(fkB, json, TableRedicSubConfig.class, "subClass");
			}
			
			
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "属性的多语种设置", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/fieldListForLan", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fieldListForLan(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/fieldListForLan",
					Table.class.getName(), null, true,this.getService());

			Table obj = this.service.getMgClient().getTableById(className);
			if (obj == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}
			super.objToJson(obj, json);

			HashMap<String, QueryListInfo<Field>> fhash = new HashMap<String, QueryListInfo<Field>>();

			PLanguage lan = new PLanguage();
			lan.setStatus(1L);
			QueryListInfo<PLanguage> llist = this.service.getMgClient().getList(lan, "seq",this.service);
			if (llist.size() <= 0) {
				json.setUnSuccess(-1, "系统未配置多语言");
				return json.jsonValue();
			}

			SelectBidding sb = new SelectBidding();
			for (PLanguage x : llist.getList()) {
				fhash.put(x.getLan(), new QueryListInfo<Field>());
				sb.put(x.getLan(), x.getRemark());
			}

			if (true) {

				Field search = new Field();
				search.setClassName(className);
				QueryListInfo<Field> list = this.service.getMgClient().getList(search, "!isKey,!propertyType,property",this.service);
				for (Field x : list.getList()) {

					HashMap<String, String> dhash = new HashMap<String, String>();

					String labelId = Field.class.getSimpleName() + "_" + x.getPk() + "_display";
					LableLanDisplay s = new LableLanDisplay();
					s.setLabelId(labelId);
					QueryListInfo<LableLanDisplay> dlist = this.service.getMgClient().getList(s, "lan",this.service);
					for (LableLanDisplay x1 : dlist.getList()) {
						dhash.put(x1.getLan() + "_display", x1.getDisplay());
					}

					labelId = Field.class.getSimpleName() + "_" + x.getPk() + "_label";
					s = new LableLanDisplay();
					s.setLabelId(labelId);
					dlist = this.service.getMgClient().getList(s, "lan",this.service);
					for (LableLanDisplay x1 : dlist.getList()) {
						dhash.put(x1.getLan() + "_label", x1.getDisplay());
					}

					for (PLanguage xl : llist.getList()) {

						QueryListInfo<Field> fList = fhash.get(xl.getLan());
						Field field = new Field();
						field.setClassName(className);
						field.setProperty(x.getProperty());
						field.setValue(x.getDisplay());
						field.setPropertyType(x.getPropertyType());

						if (dhash.containsKey(xl.getLan() + "_display")) {
							field.setDisplay(dhash.get(xl.getLan() + "_display"));
						}

						if (dhash.containsKey(xl.getLan() + "_label")) {
							field.setLabel(dhash.get(xl.getLan() + "_label"));
						}

						fList.getList().add(field);
					}
				}

			}

			for (PLanguage x : llist.getList()) {
				QueryListInfo<Field> fList = fhash.get(x.getLan());
				super.listToJson(fList, json, Field.class.getSimpleName() + x.getLan(),
						"className,property,propertyType,display,label,value", BosConstants.getTable(Field.class));
			}

			json.getData().put("lanList", sb.showJSON());

			if (true) {

				String labelId = Table.class.getSimpleName() + "_" + obj.getPk() + "_displayName";

				QueryListInfo<LableLanDisplay> lanDisList = new QueryListInfo<LableLanDisplay>();

				HashMap<String, LableLanDisplay> hash = new HashMap<String, LableLanDisplay>();

				if (true) {
					LableLanDisplay s = new LableLanDisplay();
					s.setLabelId(labelId);
					QueryListInfo<LableLanDisplay> dlist = this.service.getMgClient().getList(s, "lan",this.service);
					for (LableLanDisplay x : dlist.getList()) {
						hash.put(x.getLan(), x);
					}
				}

				if (true) {
					for (PLanguage x : llist.getList()) {
						LableLanDisplay dis = hash.get(x.getLan());
						if (dis == null) {
							dis = new LableLanDisplay();
							dis.setLabelId(labelId);
							dis.setClassName(Table.class.getName());
						}
						dis.setLan(x.getLan());
						dis.setRemark(x.getRemark());
						lanDisList.getList().add(dis);
					}
				}

				super.listToJson(lanDisList, json, LableLanDisplay.class.getSimpleName(),
						BosConstants.getTable(LableLanDisplay.class));

			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "属性的多语种设置保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/fieldListForLanSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fieldListForLanSave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/fieldListForLan",
					Table.class.getName(), null, true,this.getService());

			Table obj = this.service.getMgClient().getTableById(className);
			if (obj == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			PLanguage lan = new PLanguage();
			lan.setStatus(1L);
			QueryListInfo<PLanguage> llist = this.service.getMgClient().getList(lan, "seq",this.service);
			if (llist.size() <= 0) {
				json.setUnSuccess(-1, "系统未配置多语言");
				return json.jsonValue();
			}

			if (true) {
				// 保存类对象显示名的多语言设置
				String labelId = Table.class.getSimpleName() + "_" + obj.getPk() + "_displayName";
				List<BosEntity> labelList = json.getList(LableLanDisplay.class.getSimpleName(),
						LableLanDisplay.class.getName(), "lan,!display", null, 0, this.service);
				for (BosEntity x : labelList) {
					x.putValue("labelId", labelId);
					x.putValue("className", Table.class.getName());
					String display = x.propertyValueString("display");
					if (DataChange.isEmpty(display)) {
						this.service.getMgClient().deleteByPK(x.getPk(), LableLanDisplay.class.getName(),ut,true,this.service);
					} else {
						this.service.save(x,ut);
					}
				}
				BosConstants.getExpireHash().removeJSONObject(LableLanDisplay.class.getSimpleName() + "_" + labelId);
				new ClassInnerNotice().invoke(LableLanDisplay.class.getSimpleName(), labelId);
			}

			if (true) {

				// 保存字段的多语言设置
				for (PLanguage x : llist.getList()) {

					List<BosEntity> fList = json.getList(Field.class.getSimpleName() + x.getLan(),
							Field.class.getName(), "property,!display,!label", null, 0, this.service);
					for (BosEntity f : fList) {
						f.putMustValue("className", className);

						Vector<String> v = Field.split("display,label");

						for (String x1 : v) {

							String value = f.propertyValueString(x1);
							String labelId = Field.class.getSimpleName() + "_" + f.getPk() + "_" + x1;
							LableLanDisplay display = new LableLanDisplay();
							display.setLabelId(labelId);
							display.setLan(x.getLan());
							if (!DataChange.isEmpty(value)) {
								display.setClassName(Field.class.getName());
								display.setDisplay(value);
								this.service.save(display, ut);
							} else {
								this.service.getMgClient().deleteByPK(display.getPk(), LableLanDisplay.class,ut,this.service);
							}

							BosConstants.getExpireHash()
									.removeJSONObject(LableLanDisplay.class.getSimpleName() + "_" + labelId);
							new ClassInnerNotice().invoke(LableLanDisplay.class.getSimpleName(), labelId);
						}

					}

				}

			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	// ----------------------------------------------------------------

	@Security(accessType = "1", displayName = "表对象新增", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/showAdd", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject showAdd(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/table/fetch", PRoot.class.getName(), null,
					true,this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			Table obj = new Table();
			obj.setSys(sys);
			obj.setRegType(1L);
			obj.setClassName(root.getPackagePath() + ".model.");

			super.objToJson(obj, json);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "更新redis", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/refreshRedis", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject refreshRedis(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/refreshRedis",
					Table.class.getName(), null, true,this.getService());

			Table table = this.service.getMgClient().getTableById(className);
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			
			SynToRedisJson call = new SynToRedisJson(className);
			Thread t1 = new Thread(call);
			t1.start();
			

			json.setSuccess("操作成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "重建索引", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/rebuildIndex", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject rebuildIndex(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/rebuildIndex",
					Table.class.getName(), "", true,this.getService());

			Table obj = BosConstants.getTable(className);
			if (obj == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			if (obj.getCache().intValue() == 0) {
				DBIndexExec exec = new DBIndexExec();
				exec.setClassName(className);
				this.service.getMgClient().delete(exec,ut,this.service);
				Import importDB = new Import(service.getBaseDao());
				importDB.createIndex(obj, service.getBaseDao().getDbType(), true);
			}else if (obj.getCache().intValue() == 2) {
				this.service.getMgClient().synIndex(className,this.service);
			}
			
			
			
			

			if (DataChange.getLongValueWithDefault(obj.getSynEs(), 0) == 1) {

				Table table = this.service.getMgClient().getTableById(className);
				long esversion = DataChange.getLongValueWithDefault(table.getEsIdxVersion(), 0);
				table.setEsIdxVersion(esversion + 1);
				obj.setEsIdxVersion(table.getEsIdxVersion());
				this.service.save(table, ut);
				new ClassInnerNotice().invoke(BosConstants.innerNotice_synTable, className);
				this.service.getEsClient().indexSyn(obj, this.service);

				// 发起线程进行同步
				SynToEs call = new SynToEs(className);
				Thread t1 = new Thread(call);
				t1.start();
			}

			json.setSuccess("操作成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "同步某个系统的所有表结构", needLogin = true, isEntAdmin = false, isSysAdmin = true, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/synAll", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject synAll(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/synAll", PRoot.class.getName(), "", true,this.getService());

			StringBuffer sb = new StringBuffer();

			Table table = new Table();
			table.setSys(id);
			table.setRegType(1L);
			QueryListInfo<Table> list = this.service.getMgClient().getList(table,this.service);
			for (Table obj : list.getList()) {

				try {

					Vector<Field> fieldV = obj.addedField(this.service);
					for (Field f : fieldV) {
						f.setClassName(obj.getClassName());
						this.service.save(f,ut);
						BosConstants.debug(f);
					}

					// conn.removeTable(obj.getClassName());
					BosConstants.removeTable(obj.getClassName());

					DBIndexExec exec = new DBIndexExec();
					exec.setClassName(obj.getClassName());
					this.service.getMgClient().delete(exec,ut,this.service);

					this.service.syn(obj.getClassName(), ut.getMemberId(), ut.getUserId());
					obj.setRefreshTime(NetWorkTime.getCurrentDatetime());
					this.service.save(obj, ut);

					new ClassInnerNotice().invoke(BosConstants.innerNotice_synTable, obj.getClassName());
				} catch (Exception e) {
					sb.append(e.getMessage());
				}
			}

			String msg = sb.toString();
			if (DataChange.isEmpty(msg)) {
				msg = "同步成功";
			}

			json.setSuccess(msg);

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "重建默认函数", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/rebuildFun", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject rebuildFun(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, URI + "/rebuildFun", Table.class.getName(),
					"", true,this.getService());

			Table.createBaseSQLFunction(className, true, ut.getUserId(), this.service);

			json.setSuccess("重建成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	
	
	

	@Security(accessType = "1", displayName = "表对象同步", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/clearData", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject clearData(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/clearData", Table.class.getName(), "",
					true,this.getService());

			
			Table table=this.service.getMgClient().getTableById(className);
			
			if(table==null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}
			
			
			if(BosConstants.runTimeMode()) {
				json.setUnSuccess(-1, "运行模式不能清理表数据");
				return json.jsonValue();
			}
			
			if(table.table()) {
				this.service.getBaseDao().delete(new TableJson(className), ut,this.service);
			}else if(table.mongo()) {
				this.service.getMgClient().dropTable(className);
			}else if(table.hbase()) {
				
			}
			

			new ClassInnerNotice().invoke(BosConstants.innerNotice_synTable, className);

			json.setSuccess("操作成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}
	
	
	
	
	@Security(accessType = "1", displayName = "表对象同步", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/syn", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject syn(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/syn", Table.class.getName(), "",
					true,this.getService());

			Table obj = this.service.getMgClient().getTableById(className);
			if (obj == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			if (obj.getCache() == null)
				obj.setCache(0L);

			Vector<Field> fieldV = obj.addedField(this.service);
			for (Field f : fieldV) {
				f.setClassName(obj.getClassName());
				this.service.save(f, ut);
				BosConstants.debug(f);
			}

			// conn.removeTable(obj.getClassName());
			BosConstants.removeTable(obj.getClassName());

			DBIndexExec exec = new DBIndexExec();
			exec.setClassName(obj.getClassName());
			this.service.getMgClient().delete(exec,ut,this.service);

			this.service.syn(obj.getClassName(), ut.getMemberId(), ut.getUserId());
			obj.setRefreshTime(NetWorkTime.getCurrentDatetime());
			this.service.save(obj, ut);
			
			this.service.getRdClient().putClassNameBySimpleName(obj);

			new ClassInnerNotice().invoke(BosConstants.innerNotice_synTable, obj.getClassName());

			json.setSuccess("操作成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "表对象保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			Table obj = json.getObj(Table.class, "simpleName,sys,code,displayName,cache", this.service);

			obj.setSimpleName(obj.getSimpleName().trim());

			if (!obj.getSimpleName().substring(0, 1).toUpperCase().equals(obj.getSimpleName().substring(0, 1))) {
				json.setUnSuccess(-1, "类名的首字母必须大写");
				return json.jsonValue();
			}
			
			
			if(obj.getCodeScope()!=null) {
				//0:所属部门 1:所属机构 2:所属上级机构 3:所属运营方
				if(obj.getCodeScope()==0) {
					if(DataChange.isEmpty(obj.getOwnerOrgIdField())) {
						json.setUnSuccess(-1, "因为自动编码作用域配置,未设置数权组织字段");
						return json.jsonValue();
					}
					
				}else if(obj.getCodeScope()==1 ||  obj.getCodeScope()==2) {
					if(DataChange.isEmpty(obj.getOwnerMemberIdField())) {
						json.setUnSuccess(-1, "因为自动编码作用域配置,未设置数权机构字段");
						return json.jsonValue();
					}
					
				}else if(obj.getCodeScope()==3) {
					if(DataChange.isEmpty(obj.getOwnerMemberIdField())) {
						json.setUnSuccess(-1, "因为自动编码作用域配置,未设置数运营方字段");
						return json.jsonValue();
					}
				}
			}
			
			

			PRoot root = this.service.getMgClient().getById(obj.getSys(), PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, obj.getSys());
				return json.jsonValue();
			}

			Long tracKNum = PSysParam.paramLongValue("1", BosConstants.paramTrackFieldNum);
			if (tracKNum == null) {
				tracKNum = 6L;
			}
			
			
			Vector<String> trackV=Field.split(obj.getTrackFields());

			if (trackV.size() > tracKNum.intValue()) {
				json.setUnSuccess(-1, LabelDisplay.get("轨迹留痕字段个数限制:", json.getLan()) + tracKNum.intValue(), true);
				return json.jsonValue();
			}

			Table src = this.service.getMgClient().getTableById(obj.getClassName());
			if (src == null) {
				json.setUnSuccessForNoRecord(Table.class,obj.getClassName());
				return json.jsonValue();
			}

			if (obj.getCreateBiddingForm() != null && obj.getCreateBiddingForm().intValue() == 1
					&& !DataChange.isEmpty(obj.getDisField())) {

				String formId = "";
				if (!DataChange.isEmpty(src.getTreeParentField())) {
					formId = "TREE_" + src.getCode().toUpperCase();
				} else {
					formId = "LIST_" + src.getCode().toUpperCase();
				}

				PTreeForm tree = this.service.getMgClient().getById(formId, PTreeForm.class);
				if (tree == null) {
					tree = new PTreeForm();
					tree.setCode(formId);
				}
				if (DataChange.isEmpty(tree.getName())) {
					if (!DataChange.isEmpty(src.getTreeParentField())) {
						tree.setName(obj.getDisplayName() + "树形结构");
					} else {
						tree.setName(obj.getDisplayName() + "列表");
					}
				}
				if (DataChange.isEmpty(tree.getOrderBy())) {
					tree.setOrderBy(obj.getSortField());
				}
				tree.setSys(src.getSys());
				tree.setClassName(src.getClassName());
				if (tree.getStatus() == null) {
					tree.setStatus(0L);
				}

				tree.setRegType(1L);

				this.service.save(tree,ut);

			}

			// Constants.debug("X isChart="+obj.getIsChart());

			obj.setRegType(src.getRegType());
			if (obj.getRegType() != null && obj.getRegType().intValue() == 1) {// 人工表一定是存储于mysql 0:自动 1:人工
				obj.setCache(0L);
			}

			StringBuffer allFieldsSB = new StringBuffer();

			if (!BosConstants._sysV.contains(root.getCode())) {

				if (obj.getRegType().longValue() == 0) {
					json.setUnSuccess(-1, "系统自动注册表表对象,不能被编辑");
					return json.jsonValue();
				}

				if (src.getCache().longValue() != obj.getCache().longValue()) {
					// 查询一下表记录
					if (this.service.getAllCount(obj.getClassName()) > 0) {
						json.setUnSuccess(-1, obj.getClassName() + LabelDisplay.get("存储方式改变,请先清理数据", json.getLan()),
								true);
						return json.jsonValue();
					}
				}

				// if(!obj.getClassName().startsWith(root.getPackagePath()+".model.")) {
				// json.setUnSuccess(-1, PLabelDisplay.get("类名:", json.getLan())
				// +obj.getClassName()+PLabelDisplay.get("打包路径必须是 ", json.getLan())
				// +root.getPackagePath()+".model",true);
				// return json.jsonValue();
				// }

				String simpleName = obj.getClassName().substring(obj.getClassName().lastIndexOf(".") + 1,
						obj.getClassName().length());
				if (!simpleName.substring(0, 1).equals(simpleName.substring(0, 1).toUpperCase())) {
					json.setUnSuccess(-1, LabelDisplay.get("类名:", json.getLan()) + obj.getClassName()
							+ LabelDisplay.get(",命名不规范", json.getLan()), true);
					return json.jsonValue();
				}

				List<Field> fList = json.getList(Field.class,
						"className,property,propertyType,!dbLen,display,valueClass,isKey", this.service);
				for (Field x : fList) {
					if (x.getDbLen() == null)
						x.setDbLen("");
					x.setProperty(x.getProperty().trim());
					x.setCustomer(0L);
					
					
					if(x.getProperty().equals(DataChange.replaceNull(obj.getOwnerMemberIdField()))) {
						//x.setFkClasss(BosConstants.memberClassName);
					}else if(x.getProperty().equals(DataChange.replaceNull(obj.getOwnerOisIdField()))) {
						//x.setFkClasss(BosConstants.memberClassName);
					}else if(x.getProperty().equals(DataChange.replaceNull(obj.getOwnerOrgIdField()))) {
						//x.setFkClasss(BosConstants.orgClassName);
					}else if(x.getProperty().equals(DataChange.replaceNull(obj.getOwnerPlaceIdField()))) {
						//x.setFkClasss(BosConstants.placeClassName);
					}else if(x.getProperty().equals(DataChange.replaceNull(obj.getOwnerUserIdField()))) {
						//x.setFkClasss(BosConstants.userClassName);
					}
					
					if (x.getValueClass().equals(Date.class.getName())) {
						x.setDbLen("0");
					} else if (x.getValueClass().equals(PClob.class.getName())) {
						json.setUnSuccess(-1, "不支持PClob类型,请通过设置外键指向PClob");
					} else if (x.getValueClass().equals(String.class.getName())) {
						if (x.getDbLen().indexOf(",") != -1) {
							json.setUnSuccess(-1,
									LabelDisplay.get("属性:", json.getLan()) + x.getProperty()
											+ LabelDisplay.get(" 是字符串，但长度=", json.getLan()) + x.getDbLen()
											+ LabelDisplay.get(" 格式不正确", json.getLan()),
									true);
							return json.jsonValue();
						}
					} else if (x.getValueClass().equals(JSONObject.class.getName())) {
						x.setDbLen("");
						x.setIsKey(0L);
						
					}else if (x.getValueClass().equals(AddressJson.class.getName())) {
							x.setDbLen("1024");
							x.setIsKey(0L);
					} else if (x.getValueClass().equals(JSONArray.class.getName())) {
						x.setDbLen("");
						x.setIsKey(0L);
					} else if (x.getValueClass().equals(Long.class.getName())) {
						if (x.getDbLen().indexOf(",") != -1) {
							json.setUnSuccess(-1,
									LabelDisplay.get("属性:", json.getLan()) + x.getProperty()
											+ LabelDisplay.get(" 是整型，但长度=", json.getLan()) + x.getDbLen()
											+ LabelDisplay.get(" 格式不正确", json.getLan()),
									true);
							return json.jsonValue();
						} else {
							int len = DataChange.StringToInteger(x.getDbLen());
							if (len > 18 || len <= 0) {
								json.setUnSuccess(-1,
										LabelDisplay.get("属性:", json.getLan()) + x.getProperty()
												+ LabelDisplay.get(" 是整型，长度=", json.getLan()) + x.getDbLen()
												+ LabelDisplay.get("不能高于18位或小于等于0", json.getLan()),
										true);
								return json.jsonValue();
							}
						}
					} else if (x.getValueClass().equals(BigDecimal.class.getName())) {
						if (x.getDbLen().indexOf(",") == -1) {
							int len = DataChange.StringToInteger(x.getDbLen());
							if (len > 18 || len <= 0) {
								json.setUnSuccess(-1,
										"属性:" + x.getProperty() + " 是数字型，长度=" + x.getDbLen() + "不能高于18位或小于等于0");
								return json.jsonValue();
							}
						}
						x.setIsKey(0L);
					}

					x.setRegType(1L);
					allFieldsSB.append(x.getProperty() + ",");
					this.service.save(x, ut);
					BosConstants.debug(x);
				}

				if (!DataChange.isEmpty(obj.getDisFieldFormula())) {
					Vector<String> v = obj.pickUpKey(obj.getDisFieldFormula());
					for (String f : v) {
						Field property = new Field();
						property.setClassName(obj.getClassName());
						property.setProperty(f);
						property = this.service.getMgClient().get(property,this.service);
						if (property == null) {
							json.setUnSuccess(-1, LabelDisplay.get("显示字段规则:", json.getLan()) + obj.getDisFieldFormula()
									+ LabelDisplay.get(" 不存在 属性:", json.getLan()) + f, true);
							return json.jsonValue();
						}
					}

				}

			}

			if (!DataChange.isEmpty(obj.getDescribe())) {
				Vector<String> v = obj.pickUpKey(obj.getDescribe());
				for (String f : v) {

					Field property = new Field();
					property.setClassName(obj.getClassName());
					property.setProperty(f);
					property = this.service.getMgClient().get(property,this.service);
					if (property == null) {
						json.setUnSuccess(-1, "对象描述规则 不存在 属性:" + f);
						return json.jsonValue();
					}
				}

			}

			// 先设置好主键
			if (true) {
				Field property = new Field();
				property.setClassName(obj.getClassName());
				property.setPropertyType(1L);
				property.setIsKey(1L);
				QueryListInfo<Field> fList1 = this.service.getMgClient().getTableFieldList(property, "property");
				BosConstants.debug("-----------主键字段长度：" + fList1.size());
				StringBuffer sb = new StringBuffer();
				for (Field x : fList1.getList()) {
					sb.append(x.getProperty() + ",");
				}
				String idx = sb.toString();
				if (idx.length() > 0) {
					idx = idx.substring(0, idx.length() - 1);
				}
				obj.setKeyField(idx);
				if (obj.judgePK()) {
					obj.setIsMaster(1L);
				} else {
					obj.setIsMaster(0L);
				}

			}

			Vector<Field> fieldV = obj.addedField(this.service);
			for (Field f : fieldV) {
				f.setClassName(obj.getClassName());
				this.service.save(f,ut);
				BosConstants.debug(f);
			}

			BosConstants.debug("PK=" + obj.judgePK());

			// ExcelTemplateField tf=new ExcelTemplateField();
			// tf.setTemplateId(obj.getClassName());
			// Vector<String> tfV=this.service.getMgClient().getVector(tf, "property");

			Vector<String> checkV = Field.split("sortField,diyAppListFields,diyExpertFields,diyImportFields");
			for (String check : checkV) {

				Field dbF = BosConstants.getTable(Table.class).showDBField(check);
				if (dbF == null)
					continue;
				String value = DataChange.objToString(obj.propertyValue(check));
				if (DataChange.isEmpty(value))
					continue;
				Vector<String> fv = Field.split(value);
				long seq = 0;
				for (String f : fv) {
					Field field = new Field();
					field.setClassName(obj.getClassName());
					field.setProperty(f);
					field = this.service.getMgClient().get(field,this.service);
					if (field == null) {
						json.setUnSuccess(-1, dbF.getDisplay() + LabelDisplay.get("检查，不存在属性:", json.getLan()) + f,
								true);
						return json.jsonValue();
					}
				}

			}


			if (DataChange.isEmpty(obj.getDiyExpertFields())) {
				String diyExpertFields = allFieldsSB.toString();
				if (diyExpertFields.length() > 0)
					diyExpertFields = diyExpertFields.substring(0, diyExpertFields.length() - 1);
				obj.setDiyExpertFields(diyExpertFields);
			}

			// 自动表也支持index whereGroup where Statement的自定义
			long idxSeq = 0;
			Index si = new Index();
			si.setClassName(obj.getClassName());
			this.service.getMgClient().delete(si,ut,this.service);
			List<Index> idxList = json.getList(Index.class, "name,unique,fields,!prompt", this.service);
			for (Index x : idxList) {
				x.setClassName(obj.getClassName());
				x.setSeq(idxSeq++);
				this.service.save(x, ut);
				BosConstants.debug(x);
			}

			Statement sti = new Statement();
			sti.setClassName(obj.getClassName());
			sti.setRegType(1L);// 删除手工的
			this.service.getMgClient().delete(sti,ut,this.service);
			List<Statement> stmList = json.getList(Statement.class,
					"id,note,sql,!where,!orderBy,!groupBy,!makeup,!selectTop", this.service);
			for (Statement x : stmList) {
				x.setId(x.getId().trim());
				x.setClassName(obj.getClassName());
				if (x.getSql().toLowerCase().indexOf(" where ") == -1 && DataChange.isEmpty(x.getWhere())) {
					x.setWhere("where 1=1");
				}
				this.service.save(x,ut);
				BosConstants.debug(x);
			}

			WhereGroup wg = new WhereGroup();
			wg.setClassName(obj.getClassName());
			wg.setRegType(1L);// 删除手工的
			this.service.getMgClient().delete(wg,ut,this.service);
			List<WhereGroup> wgList = json.getList(WhereGroup.class, "ids", this.service);
			for (WhereGroup x : wgList) {
				if (DataChange.isEmpty(x.getGroupId()))
					x.setGroupId(WhereGroup.next());
				x.setClassName(obj.getClassName());
				this.service.save(x,ut);
				BosConstants.debug(x);
			}
			
			
			
			if(true) {
				TableRedicSubConfig trsc=new TableRedicSubConfig();
				trsc.setTable(obj.getClassName());
				service.getMgClient().delete(trsc,ut,this.service);
				
				List<TableRedicSubConfig> list=json.getList(TableRedicSubConfig.class, "subClass,redis,!orderBy,!init", service);
				for(TableRedicSubConfig x:list) {
					x.setTable(obj.getClassName());
					service.getMgClient().save(x, ut, service);
				}
			}
			


			this.service.save(obj,ut);

			if (DataChange.getLongValueWithDefault(obj.getSyn(), 0) == 1) {

				// conn.removeTable(obj.getClassName());
				BosConstants.removeTable(obj.getClassName());

				DBIndexExec exec = new DBIndexExec();
				exec.setClassName(obj.getClassName());
				this.service.getMgClient().delete(exec,ut,this.service);

				this.service.syn(obj.getClassName(), ut.getMemberId(), ut.getUserId());
				obj.setRefreshTime(NetWorkTime.getCurrentDatetime());
				this.service.save(obj,ut);

			} else {

				Table cache = Table.initByMongo(obj.getClassName(), this.service);
				if (cache != null) {
					cache.setRefreshTime(NetWorkTime.getCurrentDatetime());
					BosConstants.tableHash.put(cache.getClassName(), cache);
				}

			}

			// 自动注册的表,重新加载
			if (DataChange.getLongValueWithDefault(obj.getRegType(), 1) == 0) {
				Table x = Table.initByMongo(obj.getClassName(), this.service);
				if (x != null) {
					x.setRefreshTime(NetWorkTime.getCurrentDatetime());
					BosConstants.tableHash.put(obj.getClassName(), x);
				}
			}
			
			this.service.getRdClient().putClassNameBySimpleName(obj.getSys(), obj.getClassName());

			// 通知服务器更新类对象结构
			new ClassInnerNotice().invoke(BosConstants.innerNotice_synTable, obj.getClassName());

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "自动编码记录", needLogin = true, isEntAdmin = false, isSysAdmin = true, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/seqValueFetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject seqValueFetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/seqValueFetch",
					PForm.class.getName(), "", true,this.getService());
			Table table = this.service.getMgClient().getTableById(className);
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}
			if (DataChange.isEmpty(table.getCodeField())) {
				json.setUnSuccess(-1, "类对象没有设置编码属性");
				return json.jsonValue();
			}

			PageInfo page = json.getPageInfo(SeqValue.class);

			SeqValue search = json.getSearch(SeqValue.class, null, ut, this.service);
			search.setClassName(className);
			QueryFetchInfo<SeqValue> fetch = this.service.getMgClient().getFetch(search,
					"codeFormula,codeScope,scopeId", page.getCurrentPage(), page.getPageSize(),this.service);
			super.fetchToJson(fetch, json, BosConstants.getTable(SeqValue.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "自动编码记录保存", needLogin = true, isEntAdmin = false, isSysAdmin = true, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/seqValueSaveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject seqValueSaveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/seqValueFetch",
					PForm.class.getName(), "", true,this.getService());
			Table table = this.service.getMgClient().getTableById(className);
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}
			if (DataChange.isEmpty(table.getCodeField())) {
				json.setUnSuccess(-1, "类对象没有设置编码属性");
				return json.jsonValue();
			}

			List<SeqValue> list = json.getList(SeqValue.class, null, this.service);
			for (SeqValue x : list) {
				this.service.save(x, ut);
			}

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "字段列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/fieldList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fieldList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/fieldList", Field.class.getName(),
					"className", true,this.getService());

			Field field = new Field();
			field.setClassName(className);
			field.setCustomer(0L);
			QueryListInfo<Field> list = this.service.getMgClient().getTableFieldList(field, "!propertyType,property");
			super.listToJson(list, json, BosConstants.getTable(Field.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "字段计算规则", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/fieldCalculationFormula", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fieldCalculationFormula(@RequestBody HttpEntity json, HttpServletRequest request)
			throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/fieldCalculationFormula",
					FieldCalculationFormula.class.getName(), null, true,this.getService());
			Field field = this.service.getById(id, Field.class);
			if (field == null) {
				this.service.deleteById(id, FieldCalculationFormula.class.getName(), ut);
				json.setUnSuccessForNoRecord(Field.class,id);
				return json.jsonValue();
			}

			FieldCalculationFormula obj = this.service.getById(id, FieldCalculationFormula.class);
			if (obj == null) {
				obj = new FieldCalculationFormula();
				obj.setId(id);
			}
			
			
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, FieldCalculationFormula.class.getName(), "sys");
			
			if(!DataChange.isEmpty(obj.getSys())) {
				super.selectToJson(Table.getJsonForSelect(obj.getSys(), null,this.service), json, FieldCalculationFormula.class, "className");
			}
			
			
			
			if(!DataChange.isEmpty(obj.getClassName())) {
				super.selectToJson(Statement.getJsonForSelect(obj.getClassName(), service), json, FieldCalculationFormula.class, "mapId");
				super.selectToJson(Field.getJsonForSelect(obj.getClassName(), 1L, json.getLan(), service), json, FieldCalculationFormula.class, "property");
				
				
				SelectBidding sb=new SelectBidding();
				
				Field fx=new Field();
				fx.setClassName(obj.getClassName());
				fx.setFkClasss(field.getClassName());
				QueryListInfo<Field> fxList=this.service.getList(fx, "property");
				for(Field x:fxList.getList()) {
					sb.put(x.getProperty(), x.getDisplay()+"["+x.getProperty()+"]");
				}
				
				//处理类似 bizView bizCollection的业务字段
				Table table=BosConstants.getTable(obj.getClassName());
				if(table!=null && !DataChange.isEmpty(table.getBizIdField())) {
					Field fx1=table.getDBField(table.getBizIdField());
					if(fx1!=null) {
						sb.put(fx1.getProperty(), fx1.getDisplay()+"["+fx1.getProperty()+"]");
					}
				}
				super.selectToJson(sb, json, FieldCalculationFormula.class, "idProperty");
			}
			//
			//json.getData().put(key, value);
			
			
			super.objToJson(obj, json);
			
			if(!DataChange.isEmpty(obj.getProperty())) {
				Field fx=BosConstants.getTable(obj.getClassName()).getField(obj.getProperty());
				JSONObject oo=json.getData().getJSONObject(obj.showTable().getSimpleName());
				JSONObject x=new JSONObject();
				x.put("_display", fx.getDisplay());
				oo.put("_property_json",x );
			}
			
			
			
			if(!DataChange.isEmpty(obj.getIdProperty())) {
				Field fx=BosConstants.getTable(obj.getClassName()).getField(obj.getIdProperty());
				JSONObject oo=json.getData().getJSONObject(obj.showTable().getSimpleName());
				JSONObject x=new JSONObject();
				x.put("_display", fx.getDisplay());
				oo.put("_idProperty_json",x );
			}
			
			
			if(!DataChange.isEmpty(obj.getIdProperty())) {
				Field fx=BosConstants.getTable(obj.getClassName()).getField(obj.getIdProperty());
				JSONObject oo=json.getData().getJSONObject(obj.showTable().getSimpleName());
				JSONObject x=new JSONObject();
				x.put("_display", fx.getDisplay());
				oo.put("_idProperty_json",x );
			}
			
			

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "字段计算规则保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/fieldCalculationFormulaSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fieldCalculationFormulaSave(@RequestBody HttpEntity json, HttpServletRequest request)
			throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/fieldCalculationFormula",
					FieldCalculationFormula.class.getName(), null, true,this.getService());
			Field field = this.service.getById(id, Field.class);
			if (field == null) {
				json.setUnSuccessForNoRecord(Field.class,id);
				this.service.deleteById(id, FieldCalculationFormula.class.getName(), ut);
				return json.jsonValue();
			}

			FieldCalculationFormula obj = json.getObj(FieldCalculationFormula.class,
					"!sys,!className,!property,!tjType,!idProperty,!mapId", this.service);
			obj.setId(id);
			obj.setfClassName(field.getClassName());
			obj.setfProperty(field.getProperty());
			this.service.save(obj, ut);

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "索引列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/indexList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject indexList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/indexList", Table.class.getName(),
					null, true,this.getService());

			Table table = this.service.getMgClient().getTableById(className);
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			List<Index> idxList = this.service.getBaseDao().getIndexList(table,this.service);

			Field field = new Field();
			field.setClassName(className);
			field.setCustomer(0L);

			QueryListInfo<Index> list = new QueryListInfo<Index>();
			list.setList(idxList);
			super.listToJson(list, json, BosConstants.getTable(Index.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "删除数据库索引", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/indexDropForDB", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject indexDropForDB(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/indexList", Field.class.getName(),
					null, true,this.getService());

			Table table = this.service.getMgClient().getTableById(className);
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			String idx = json.getSelectedId(Constants.current_sys, "/table/indexDropForDB", Index.class.getName(),
					"name", true,this.getService());
			this.service.getBaseDao().dropIndex(table, idx);

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "字段列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbFieldList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbFieldList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/dbFieldList", Field.class.getName(),
					"className", true,this.getService());

			Field field = new Field();
			field.setClassName(className);
			field.setPropertyType(1L);
			field.setCustomer(0L);
			QueryListInfo<Field> list = this.service.getMgClient().getTableFieldList(field, "!propertyType,property");
			super.listToJson(list, json, BosConstants.getTable(Field.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "扩展字段列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/exFieldList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject exFieldList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/exFieldList", Field.class.getName(),
					"className", true,this.getService());

			Field field = new Field();
			field.setClassName(className);
			field.setCustomer(0L);
			QueryListInfo<Field> list = this.service.getMgClient().getTableFieldList(field, "!propertyType,property");
			super.listToJson(list, json, BosConstants.getTable(Field.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "sqlmap保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/table/fetch", PRoot.class.getName(), null,
					true,this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			List<Table> list = json.getList(Table.class, "simpleName,sys,displayName,cache,regType,synEs", this.service);
			for (Table t : list) {

				if (t == null)
					continue;

				String changeName = null;

				// 0:自动 1:人工
				if (t.getRegType().intValue() == 1) {
					t.setCache(0L);
				}

				if (!t.getSimpleName().substring(0, 1).toUpperCase().equals(t.getSimpleName().substring(0, 1))) {
					json.setUnSuccess(-1, "类名首字母必须大写");
					return json.jsonValue();
				}

				Table src = this.service.getMgClient().getTableById(t.getClassName());

				if (src != null) {


					if (src.getCache().longValue() != t.getCache().longValue()) {
						// 查询一下表记录
						if (src.getCache().longValue() == 0) {
							if (this.service.getBaseDao().getAllCount(t.getClassName()) > 0) {
								json.setUnSuccess(-1,
										t.getClassName() + LabelDisplay.get("存储方式改变,请先清理数据", json.getAppId()), true);
								return json.jsonValue();
							}
						} else {
							if (this.service.getMgClient().getCount(t.getClassName()) > 0) {
								json.setUnSuccess(-1,
										t.getClassName() + LabelDisplay.get("存储方式改变,请先清理数据", json.getLan()), true);
								return json.jsonValue();
							}
						}
					}

					// if(!src.getClassName().startsWith(root.getPackagePath()+".model.")) {
					// json.setUnSuccess(-1,PLabelDisplay.get("类名:", json.getLan())
					// +src.getClassName()+PLabelDisplay.get("打包路径必须是 ", json.getLan())
					// +root.getPackagePath()+".model",true);
					// return json.jsonValue();
					// }

				} else {

					t.setSimpleName(t.getSimpleName().trim());
					if(DataChange.isEmpty(root.getPackagePath())) {
						t.setClassName(root.getCode().toLowerCase() + ".model." + t.getSimpleName());
					}else {
						t.setClassName(root.getPackagePath() + ".model." + t.getSimpleName());
					}
				}
				
				if(t.getSynEs().intValue()==0) {
					t.setQueryEs(0L);
				}

				//t.setMemberId(ut.getMemberId());
				this.service.save(t,ut);
				
				this.service.getRdClient().putClassNameBySimpleName(t.getSys(),t.getClassName());

				if (!DataChange.isEmpty(changeName)) {
					// 更换了名字
					Table.changeClassName(t.getClassName(), changeName, this.service);
				}

			}

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	// @Menu(name = "数据表", trunk = "基础信息", js = "table")
	@Security(accessType = "1", displayName = "数据表列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/list", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject list(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/table/fetch", PRoot.class.getName(), null,
					true,this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}
			root.setSerialCode(ut.getSerialCode());
			super.objToJson(root, json);

			Table search = json.getSearch(Table.class, null, ut, this.service);
			if (search.getCache() == null) {
				search.setCache(0L);
			}
			search.setSys(sys);

			QueryListInfo<Table> list = this.service.getMgClient().getList(search, "className",this.service);

			super.listToJson(list, json, BosConstants.getTable(Table.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据表列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/table/fetch", PRoot.class.getName(), null,
					true,this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}
			root.setSerialCode(ut.getSerialCode());
			super.objToJson(root, json);

			PageInfo page = json.getPageInfo(Table.class);

			Table search = json.getSearch(Table.class, null, ut, this.service);
			search.setSys(sys);
			QueryFetchInfo<Table> fetch = this.service.getMgClient().getFetch(search, "className",
					page.getCurrentPage(), page.getPageSize(),this.service);

			super.fetchToJson(fetch, json, BosConstants.getTable(Table.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据表列表(唯一主键)", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/pkList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject pkList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/table/pkList", PRoot.class.getName(), null,
					true,this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			Vector<Table> delV = new Vector<Table>();

			Table search = new Table();
			search.setSys(sys);
			QueryListInfo<Table> list = this.service.getMgClient().getList(search, "className",this.service);
			for (Table t : list.getList()) {
				if (!t.judgePK()) {
					delV.add(t);
					continue;
				}
			}

			for (Table t : delV) {
				list.getList().remove(t);
			}

			super.listToJson(list, json, BosConstants.getTable(Table.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "消息列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/mqList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject mqList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String entity=json.getSelectedId(Constants.current_sys, URI+"/mqList", MQProcessing.class.getName(), null, true, service);
			
			MQProcessing search=new MQProcessing();
			search.setEntity(entity);
			QueryListInfo<MQProcessing> list=this.service.getList(search, "className,display");
			super.listToJson(list, json, search.showTable());
			
			
			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "消息列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/mqDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject mqDelete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			//String entity=json.getSelectedId(Constants.current_sys, URI+"/mqList", MQProcessing.class.getName(), null, true, service);
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/mqDelete", MQProcessing.class.getName(), null, true, service);
			
			
			this.service.deleteById(id, MQProcessing.class.getName(), ut);
			
			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}
	
	

	@Security(accessType = "1", displayName = "数据表列表(树形结构)", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/treeList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject treeList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/table/treeList", PRoot.class.getName(), null,
					true,this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			Vector<Table> delV = new Vector<Table>();

			Table search = new Table();
			search.setSys(sys);
			QueryListInfo<Table> list = this.service.getMgClient().getList(search, "className",this.service);
			for (Table t : list.getList()) {
				if (DataChange.isEmpty(t.getTreeParentField())) {
					delV.add(t);
					continue;
				}
			}

			for (Table t : delV) {
				list.getList().remove(t);
			}

			super.listToJson(list, json, BosConstants.getTable(Table.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}
	
	
	
	
	
	
	
	
	
	

	@Security(accessType = "1", displayName = "同步到ES", needLogin = true, isEntAdmin = false, isSysAdmin = true, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/synToEsForTable", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject synToEsForTable(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/synToEsForTable", Table.class.getName(),
					null, true,this.getService());

			Table obj = BosConstants.getTable(id);

			if (DataChange.getLongValueWithDefault(obj.getCache(), 0) == 5) {
				json.setUnSuccess(-1, obj.getDisplayName() + "存储于ES,无需同步");
				return json.jsonValue();
			}

			if (DataChange.getLongValueWithDefault(obj.getSynEs(), 0) != 1) {
				json.setUnSuccess(-1, "不支持es同步数据");
				return json.jsonValue();
			}

			// 发起线程进行同步
			SynToEs call = new SynToEs(id);
			Thread t1 = new Thread(call);
			t1.start();

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据列表ES", needLogin = true, isEntAdmin = false, isSysAdmin = true, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dataListForEs", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dataListForEs(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/dataListForEs", Table.class.getName(),
					null, true,this.getService());

			String objId = "DataEs";

			String searchKey = null;
			if (json.getData().containsKey(objId + "Search")) {
				JSONObject search = json.getData().getJSONObject(objId + "Search");
				if (search.containsKey("searchKeyWord"))
					searchKey = search.getString("searchKeyWord");
			}

			BosConstants.debug("------------searchKey=" + searchKey + "---------------------");

			Table obj = BosConstants.getTable(id);

			if (DataChange.getLongValueWithDefault(obj.getSynEs(), 0) != 1) {
				json.setUnSuccess(-1, "不支持es同步数据");
				return json.jsonValue();
			}

			PageInfo page = json.getPageInfo(objId);
			BosEntity search = new TableJson(obj.getClassName());
			search.setSearchKeyWord(searchKey);
			QueryFetchInfo<BosEntity> fetch = this.service.getEsClient().getFetch(search, obj.getSortField(),
					page.getCurrentPage(), page.getPageSize(),this.service);

			super.fetchToJson(fetch, json, objId, obj.getAllDBField(), obj);

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dataList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dataList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/dataList", Table.class.getName(), null,this.getService());

			String objId = "Data";

			
			JSONObject s=new JSONObject();
			if (json.getData().containsKey(objId + "Search")) {
				s = json.getData().getJSONObject(objId + "Search");
			}

			

			if (!DataChange.isEmpty(id)) {

				Table obj = BosConstants.getTable(id);
				
				BosEntity search = new TableJson(obj.getClassName());
				search.setSearchKeyWord(s.getString("searchKeyWord"));
				search.setQueryCodeValue(s.getString("queryCodeValue"));
				search.setQueryCodeValueProperty(s.getString("queryCodeValueProperty"));
				search.setQueryDateMax(DataChange.stringToDate(s.getString("queryDateMax")));
				search.setQueryDateMin(DataChange.stringToDate(s.getString("queryDateMin")));
				search.setQueryDateProperty(s.getString("queryDateProperty"));
				search.setQueryNumMax(DataChange.StringToLong(s.getString("queryNumMax")));
				search.setQueryNumMin(DataChange.StringToLong(s.getString("queryNumMin")));
				search.setQueryNumProperty(s.getString("queryNumProperty"));
				
				BosConstants.debug("------------searchKey=" + search.getSearchKeyWord() + "---------------------");
				

				PageInfo page = json.getPageInfo(objId);
				// String searchKey=json.getSearchKeyWord(objId);
				QueryFetchInfo<BosEntity> fetch=new QueryFetchInfo<BosEntity>();
				
				if(obj.table()) {
					fetch = this.service.getRBaseDao().getFetch(search, obj.getSortField(), page.getCurrentPage(), page.getPageSize(),this.service);
				}else if(obj.mongo()) {
					fetch = this.service.getMgClient().getFetch(search, obj.getSortField(), page.getCurrentPage(), page.getPageSize(),this.service);
				}else {
					fetch = this.service.getEsClient().getFetch(search, obj.getSortField(), page.getCurrentPage(), page.getPageSize(),this.service);
				}

				if (obj.judgePK()) {
					super.fetchToJson(fetch, json, objId, obj.getAllDBField(), obj);
					// json.getData().put("fields_list",obj.getListFields()+",isValid");
				} else {
					super.fetchToJson(fetch, json, objId, obj.getAllDBField(), obj);
					// json.getData().put("fields_list",obj.getListFields());
				}

			}

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}
	
	
	
	

	@Security(accessType = "1", displayName = "redis缓存值", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/redisJson", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject redisJson(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			
			Table table = BosConstants.getTable(
					json.getSelectedId(Constants.current_sys, "/table/dataList", Table.class.getName(), null,this.getService()));

			String id = json.getSelectedId(Constants.current_sys, "/table/redisJson", table.getClassName(), null,
					true,this.getService());
			
			
			BosEntity obj=this.service.getById(id, table.getClassName());
			if(obj==null) {
				json.setUnSuccess(-1, "对象不存在");
				return json.jsonValue();
			}
			
			
			obj.synData(this.service);
			
			String v =this.service.getRdClient().get(RdClient.getDisplayKey(id, table.getClassName()));

			if(DataChange.isEmpty(v)) {
				json.setUnSuccess(-1, "无redis缓存");
				return json.jsonValue();
			}
			
			json.getData().put("cache",JsonUtil.string2Json(v));
				
			json.setSuccess();

		} catch (Exception e) {
			logger.error("获取缓存", e);
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	

	@Security(accessType = "1", displayName = "数据详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dataInfo", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dataInfo(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String objId = "Data";

			String id = json.getSelectedId(Constants.current_sys, "/table/dataInfo", Table.class.getName(), null,
					true,this.getService());

			Table table = BosConstants.getTable(
					json.getSelectedId(Constants.current_sys, "/table/dataList", Table.class.getName(), null,this.getService()));

			BosEntity obj = this.service.getById(id, table.getClassName());

			super.objToJson(obj, table.getAllDBField(), json, objId);

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	
	
	
	
	@Security(accessType = "1", displayName = "数据清理", needLogin = true, isEntAdmin = false, isSysAdmin = true)
	@RequestMapping(value = "/dataClear", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dataClear(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, "/table/dataClear", Table.class.getName(),
					null, true,this.getService());

			Table table = BosConstants.getTable(className);
			if (table == null) {
				json.setUnSuccess(-1, "表对象不存在");
				return json.jsonValue();
			}

			if (!table.mongo()) {
				json.setUnSuccess(-1, "非mong数据表,不能清零");
				return json.jsonValue();
			}

			QueryListInfo<BosEntity> list = this.service.getMgClient().getAll(className);
			for (BosEntity j : list.getList()) {
				// Constants.debug("id="+j.get_id()+" pk="+j.getData().getString("pk"));
				String id = j.getPk();
				if (DataChange.isEmpty(id))
					continue;
				BosConstants.debug(j.getData().toString());
			}

			json.setSelectedId(Constants.current_sys, "/table/dataList", className);

			json.setSuccess("操作成功");

			return this.dataList(json, request);

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

	}

	@Security(accessType = "1", displayName = "数据删除", needLogin = true, isEntAdmin = false, isSysAdmin = true)
	@RequestMapping(value = "/dataDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dataDelete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/dataDelete", String.class.getName(), null,this.getService());

			String className = json.getSelectedId(Constants.current_sys, "/table/dataList", Table.class.getName(),
					null,this.getService());

			if (DataChange.isEmpty(id) || DataChange.isEmpty(className)) {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}

			Table table = BosConstants.getTable(className);
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			if (table.table()) {

				if (table.judgePK()) {
					BosEntity bo = this.service.getById(id, className);
					if (bo != null && bo.getIsValid() != null && bo.getIsValid().intValue() == 0) {
						// 硬删除
						this.service.getBaseDao().deleteByPK(id, className, ut, true,this.service);
					} else {
						this.service.getBaseDao().deleteByPK(id, className, ut,this.service);
					}
				} else {
					this.service.getBaseDao().deleteByPK(id, className, ut, true,this.service);
				}

			} else if (table.mongo()) {
				this.service.getMgClient().deleteByPK(id, className,ut,true,this.service);
			}

			json.setSuccess("删除成功");

			return this.dataList(json, request);

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

	}

	@Security(accessType = "1", displayName = "数据恢复", needLogin = true, isEntAdmin = false, isSysAdmin = true)
	@RequestMapping(value = "/dataValid", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dataValid(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/dataValid", String.class.getName(), null,this.getService());

			String className = json.getSelectedId(Constants.current_sys, "/table/dataList", Table.class.getName(),
					null,this.getService());

			if (DataChange.isEmpty(id) || DataChange.isEmpty(className)) {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}

			Table table = BosConstants.getTable(className);
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			if (table.table() && table.judgePK()) {
				BosEntity bo = this.service.getById(id, className);
				if (bo != null && bo.getIsValid() != null && bo.getIsValid().intValue() == 0) {
					bo.setIsValid(1);
					this.service.save(bo, ut);
				}
			}

			json.setSuccess("恢复成功");

			return this.dataList(json, request);

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

	}

	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/delete", Table.class.getName(), null,
					true,this.getService());

			Table table = this.service.getMgClient().getTableById(id);

			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,id);
				return json.jsonValue();
			}

			long total=0;
			try {
				total = this.service.getAllCount(id);
			}catch(Exception e) {
				e.printStackTrace();
			}

			if (total > 0) {
				if (BosConstants.runTimeMode()) {
					json.setUnSuccess(-1,
							table.getDisplayName() + LabelDisplay.get(" 含有数据,请手工删除数据后，再删除表对象", json.getLan()), true);
					return json.jsonValue();
				} else {
					json.setUnSuccess(-1, table.getClassName() + "[" + table.getDisplayName() + "] "
							+ LabelDisplay.get("含有数据,请手工删除数据后，再删除表对象", json.getLan()), true);
					return json.jsonValue();
				}
			}

			if (table.getRegType().intValue() == 1) {
				Field field = new Field();
				field.setClassName(id);
				field.setRegType(1L);
				field.setCustomer(0L);
				if (this.service.getMgClient().getCount(field,this.service) > 1) {
					if (BosConstants.runTimeMode()) {
						json.setUnSuccess(-1,
								table.getDisplayName() + LabelDisplay.get(" 含有字段,请删除字段后，再删除表对象", json.getLan()), true);
						return json.jsonValue();
					} else {
						json.setUnSuccess(-1, table.getClassName() + "[" + table.getDisplayName() + "] "
								+ LabelDisplay.get("含有字段,请删除字段后，再删除表对象", json.getLan()), true);
						return json.jsonValue();
					}
				}
			}

			Table.delete(id, this.service);

			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/deleteMapId", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deleteMapId(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/deleteMapId", WhereGroup.class.getName(),
					null, true,this.getService());

			WhereGroup group = this.service.getMgClient().getById(id, WhereGroup.class);
			if (group == null) {
				json.setUnSuccessForNoRecord(WhereGroup.class,id);
				return json.jsonValue();
			}

			Table table = this.service.getMgClient().getTableById(group.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,group.getClassName());
				return json.jsonValue();
			}


			WhereGroup w = new WhereGroup();
			w.setGroupId(id);

			Where ww = new Where();
			ww.setGroupId(w.getGroupId());
			if (this.service.getMgClient().getCount(ww,this.service) > 0) {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}

			this.service.getMgClient().delete(w,ut,this.service);

			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/deletefieldId", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deletefieldId(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/deletefieldId", Field.class.getName(),
					null, true,this.getService());

			Field field = this.service.getMgClient().getById(id, Field.class);
			if (field == null) {
				json.setUnSuccessForNoRecord(Field.class,id);
				return json.jsonValue();
			}

			Table table = this.service.getMgClient().getTableById(field.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,field.getClassName());
				return json.jsonValue();
			}


			this.service.getMgClient().deleteByPK(id, Field.class,ut,this.service);

			// 删除模板的字段
			PFormField ff = new PFormField();
			ff.setClassName(field.getClassName());
			ff.setProperty(field.getProperty());
			this.service.getMgClient().delete(ff,ut,this.service);

			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/deleteIndexId", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deleteIndexId(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/deleteIndexId", Index.class.getName(),
					null, true,this.getService());

			Index idx = this.service.getMgClient().getById(id, Index.class);
			if (idx == null) {
				json.setUnSuccessForNoRecord(Index.class,id);
				return json.jsonValue();
			}

			Table table = this.service.getMgClient().getTableById(idx.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class);
				return json.jsonValue();
			}


			this.service.getMgClient().deleteByPK(id, Index.class,ut,this.service);

			json.setSuccess("删除成功");

			return json.jsonValue();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

	}

	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/deleteStatementId", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deleteStatementId(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/deleteStatementId",
					Statement.class.getName(), null, true,this.getService());

			Statement idx = this.service.getMgClient().getById(id, Statement.class);
			if (idx == null) {
				json.setUnSuccessForNoRecord(Statement.class,id);
				return json.jsonValue();
			}

			Table table = this.service.getMgClient().getTableById(idx.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,idx.getClassName());
				return json.jsonValue();
			}


			this.service.getMgClient().deleteByPK(id, Statement.class,ut,this.service);

			return json.jsonValue();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

	}

	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/deleteWhereId", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deleteWhereId(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/deleteWhereId", Where.class.getName(),
					null, true,this.getService());

			Where idx = this.service.getMgClient().getById(id, Where.class);
			if (idx == null) {
				json.setUnSuccessForNoRecord(Where.class,id);
				return json.jsonValue();
			}

			Table table = this.service.getMgClient().getTableById(idx.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,idx.getClassName());
				return json.jsonValue();
			}


			this.service.getMgClient().deleteByPK(id, Where.class,ut,this.service);

			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "sqlmap保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/saveMapId", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveMapId(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/table/getMapId", WhereGroup.class.getName(),
					null, true,this.getService());

			WhereGroup wg = this.service.getMgClient().getById(id, WhereGroup.class);
			if (wg == null) {
				json.setUnSuccessForNoRecord(WhereGroup.class,id);
				return json.jsonValue();
			}

			Table table = this.service.getMgClient().getTableById(wg.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,wg.getClassName());
				return json.jsonValue();
			}

			List<Where> list = json.getList(Where.class, "where,makeup,groupId", this.service);
			for (Where where : list) {
				BosConstants.debug(where.getWhere());
				where.setGroupId(id);
				where.setClassName(wg.getClassName());
				this.service.save(where,ut);
			}

			json.setSuccess("操作成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}
		return this.getMapId(json, request);

	}

	@Security(accessType = "1", displayName = "sqlmap获取", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/getMapId", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject getMapId(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			String id = json.getSelectedId(Constants.current_sys, "/table/getMapId", WhereGroup.class.getName(),
					null, true,this.getService());

			WhereGroup group = this.service.getMgClient().getById(id, WhereGroup.class);
			if (group == null) {
				json.setUnSuccessForNoRecord(WhereGroup.class,id);
				return json.jsonValue();
			}

			super.objToJson(group, json);

			Table table = this.service.getMgClient().getTableById(group.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,group.getClassName());
				return json.jsonValue();
			}

			super.objToJson(table, json);

			Where w = new Where();
			w.setGroupId(id);

			QueryListInfo<Where> list = this.service.getMgClient().getTableWhereList(w, "makeup");

			super.listToJson(list, json, BosConstants.getTable(Where.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	
	//es配置列表
	@Security(accessType = "1", displayName = "全文检索设置总览", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/esConfigSummray", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject esConfigSummray(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id = json.getSelectedId(Constants.current_sys, URI+"/esConfigSummray", Table.class.getName(),
					null, true,this.getService());
			
			
			Table table=this.service.getMgClient().getTableById(id);
			if(table==null) {
				json.setUnSuccessForNoRecord(Table.class,id);
				return json.jsonValue();
			}
			
			super.objToJson(table, json);


			TableESConfig tc=new TableESConfig();
			tc.setTable(id);
			Vector<String> tcV=this.service.getMgClient().getVector(tc, "fkProperty",this.service);
			
			
			QueryListInfo<TableESConfig> configList=new QueryListInfo<TableESConfig>();
			
			SelectBidding hotSearchListSB = new SelectBidding();
			
			
			
			if(true) {
				Field field = new Field();
				field.setClassName(id);
				field.setCustomer(0L);
				field.setPropertyType(1L);
				QueryListInfo<Field> fList = this.service.getMgClient().getTableFieldList(field,
						"!isKey,!propertyType,property");
				for (Field x : fList.getList()) {
					
					if(!DataChange.isEmpty(x.getFkClasss())) {
						TableESConfig config=new TableESConfig();
						config.setTable(id);
						config.setFkProperty(x.getProperty());
						config.setFkClass(x.getFkClasss());
						config.setDisplay(x.getDisplay());
						if(tcV.contains(x.getProperty())) {
							config.setStatus(1);
						}else {
							//删除关联
							TableESPropertyConfig ts=new TableESPropertyConfig();
							ts.setTable(id);
							ts.setClassName(x.getFkClasss());
							this.service.delete(ts,ut,true);
							//删除其子类
							Table t=BosConstants.getTable(x.getFkClasss());
							if(t!=null) {
								Vector<String> v=Table.getSlaverV(x.getClassName());
								for(String cl:v) {
									ts=new TableESPropertyConfig();
									ts.setTable(id);
									ts.setClassName(cl);
									this.service.delete(ts,ut,true);
								}
							}
							
							config.setStatus(0);
						}
						configList.getList().add(config);
					}
					
					if ( DataChange.getLongValueWithDefault(x.getPropertyType(), 0) == 1 && x.getValueClass().equals(String.class.getName())
							&& (DataChange.getLongValueWithDefault(x.getIsLike(), 0) > 0
									|| !DataChange.isEmpty(x.getFkClasss()) || !DataChange.isEmpty(x.getCodeType()))) {
						hotSearchListSB.put(x.getProperty(),
								x.getDisplay() + "[" + x.getProperty() + "]   "
										+ CacheTools.getSysCodeDisplay("5",
												String.valueOf(DataChange.getLongValueWithDefault(x.getPropertyType(), 0)),
												json.getLan()));
					}
					
				}
				super.listToJson(fList, json, field.showTable());
			
			}
			
			
			
			
			super.selectToJson(Field.getJsonForSelectViewNum(id, this.service), json, Table.class,
					"hotSearchField");
			
			
			super.listToJson(configList, json, BosConstants.getTable(TableESConfig.class));
			
			
			
			if(true) {
				
				
				QueryListInfo<Table> subList=new QueryListInfo<Table>();
				
				//TableESSubConfig esc=new TableESSubConfig();
				//esc.setTable(id);
				//Vector<String> v=this.service.getVector(esc, "subClass");
				
				//所有从表
				TableRedicSubConfig sub=new TableRedicSubConfig();
				sub.setTable(id);
				sub.setEs(1);
				QueryListInfo<TableRedicSubConfig> sList=this.service.getList(sub, "subClass");
				for(TableRedicSubConfig x:sList.getList()) {
					if(DataChange.isEmpty(x.getSubClass())) continue;
					if(DataChange.isEmpty(x.getSubField())) continue;
					Table tx=this.service.getMgClient().getTableById(x.getSubClass());
					if(tx==null) continue;
					subList.getList().add(tx);
				}
				
				super.listToJson(subList, json, "subTable", BosConstants.getTable(Table.class).listDBFields(false), BosConstants.getTable(Table.class));
			}
			
			
			if(true) {
				
				QueryListInfo<TableESPropertyConfig> tpcList=new QueryListInfo<TableESPropertyConfig>();
				
				HashMap<String,TableESPropertyConfig> fv=new HashMap<String,TableESPropertyConfig>();
				TableESPropertyConfig ts=new TableESPropertyConfig();
				ts.setTable(id);
				QueryListInfo<TableESPropertyConfig> tcList=this.service.getMgClient().getList(ts,"className,property",this.service);
				for(TableESPropertyConfig x:tcList.getList()) {
					fv.put(x.getPk(),x);
				}
				//super.listToJson(list, json, ts.showTable());
				
				
				
				//
				Vector<String> v=new Vector<String>();
				//本类的外键
				Vector<String> vx=Table.getSlaverV(table.getClassName());
				
				System.out.println(id+"  外键表 size="+vx.size());
				for(String x1:vx) {
					if(!v.contains(x1)) {
						v.add(x1);
					}
				}
				
				BosConstants.debug("-----------"+table.getClassName()+":FK size="+v.size()+"----------");
				
				
				
				//类选择
				TableESConfig  tec=new TableESConfig();
				tec.setTable(id);
				QueryListInfo<TableESConfig> list=this.service.getMgClient().getList(tec, "fkProperty",this.service);
				BosConstants.debug("-----------"+table.getClassName()+":TableESConfig size="+list.size()+"----------");
				for(TableESConfig x:list.getList()) {
					Field field=new Field();
					field.setClassName(id);
					field.setProperty(x.getFkProperty());
					field=this.service.getById(field.getPk(), Field.class);
					if(field==null) {
						this.service.getMgClient().deleteByPK(x.getPk(), TableESConfig.class,ut,this.service);
						continue;
					}
					if(DataChange.isEmpty(field.getFkClasss())) {
						this.service.getMgClient().deleteByPK(x.getPk(), TableESConfig.class,ut,this.service);
						continue;
					}
					
					Table t=BosConstants.getTable(field.getFkClasss());
					if(t==null) {
						this.service.getMgClient().deleteByPK(x.getPk(), TableESConfig.class,ut,this.service);
						continue;
					}
					
					v.add(t.getClassName());
					
					vx=Table.getSlaverV(t.getClassName());
					BosConstants.debug("-----------"+t.getClassName()+":FK size="+vx.size()+"----------");
					
					for(String x1:vx) {
						if(!v.contains(x1)) {
							v.add(x1);
						}
					}
				}

				for(String className:v) {
					
					Table esTable=BosConstants.getTable(className);
					
					
					
					//查询属性
					Field fs=new Field();
					fs.setClassName(className);
					fs.setCustomer(0L);
					fs.setPropertyType(1L);
					QueryListInfo<Field> fList=this.service.getMgClient().getList(fs, "property",this.service);
					for(Field x:fList.getList()) {
						
						//0:外键  1:数据字典   2:字符串   3:主键
						TableESPropertyConfig config=new TableESPropertyConfig();
						config.setTable(id);
						config.setClassName(className);
						config.setDisplay(x.getDisplay());
						
						if(!DataChange.isEmpty(x.getFkClasss())) {
							//list.getList().add(x);
							config.setProperty(x.getProperty());
							config.setFieldType(0);
						}else if(!DataChange.isEmpty(x.getCodeType())) {
							config.setProperty(x.getProperty());
							config.setFieldType(1);
						}else {
							if(esTable.judgePK() &&  DataChange.getLongValueWithDefault(x.getIsKey(), 0)==1) {
								config.setProperty(x.getProperty());
								config.setFieldType(3);
							}else if(x.getProperty().equals(DataChange.replaceNull(esTable.getDisField()))) {
								config.setProperty(x.getProperty());
								config.setFieldType(2);
							}else if(DataChange.getLongValueWithDefault(x.getSynRedis(), 0)==1) {
								config.setProperty(x.getProperty());
								config.setFieldType(2);
							}else if(DataChange.getLongValueWithDefault(x.getIsLike(), 0)>0) {
								config.setProperty(x.getProperty());
								config.setFieldType(2);
							}
						}
						
						
						
						if(DataChange.isEmpty(config.getProperty())) continue;
						
						TableESPropertyConfig old=fv.get(config.getPk());
						if(old!=null) {
							config.setSelected();
							config.setQueryType(old.getQueryType());
							config.setBiddingField(old.getBiddingField());
							config.setFieldType(old.getFieldType());
						}
						tpcList.getList().add(config);
					}
					
					
					
					
				}
				
				super.listToJson(tpcList, json, TableESPropertyConfig.class.getSimpleName(), BosConstants.getTable(TableESPropertyConfig.class).listDBFields(false)+",display", BosConstants.getTable(TableESPropertyConfig.class));
				
				
				SelectBidding sb=Field.getJsonForSelect(id, 0L, String.class.getName(), null, service);
				super.selectToJson(sb, json, TableESPropertyConfig.class, "biddingField");
				
				
			}
			
			
			
	
			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "全文检索保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/esConfigSummraySave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject esConfigSummraySave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id = json.getSelectedId(Constants.current_sys, URI+"/esConfigSummray", Table.class.getName(),
					null, true,this.getService());
			
			
			
			
			
			Table obj=json.getObj(Table.class, "!hotSearchField,!hotSearchCalculationFormula,queryEs", this.service);
			obj.setClassName(id);
			this.service.save(obj, ut);
			
			
			List<TableESConfig> list=json.getList(TableESConfig.class, "fkProperty,status", service);
			for(TableESConfig x:list) {
				x.setTable(id);
				if(x.getStatus().intValue()==1) {
					this.service.save(x, ut);
				}else {
					this.service.deleteById(x.getPk(), TableESConfig.class.getName(), ut);
				}
			}
			
			
			
			Vector<String> ids=json.showIds();
			
			List<TableESPropertyConfig> list1=json.getList(TableESPropertyConfig.class, "className,property,!queryType,!biddingField", service);
			for(TableESPropertyConfig x:list1) {
				x.setTable(id);
				if(ids.contains(x.getPk())) {
					
					if(x.getQueryType()==null ||  DataChange.isEmpty(x.getBiddingField())) {
						json.setUnSuccess(-1, x.getClassName()+":"+x.getProperty()+"信息不完整");
						return json.jsonValue();
					}
					
					this.service.save(x, ut);
				}else {
					this.service.deleteById(x.getPk(), TableESPropertyConfig.class.getName(), ut);
				}
			}
			
			
			
			
			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	@Security(accessType = "1*", displayName = "修改轨迹记录(根据类名)", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/trackFetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject trackFetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		// Constants.debug("***************1\n"+json.stringValue());


		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String className = json.getSelectedId(Constants.current_sys, URI+"/info", Table.class.getName(), "", true,this.getService());
			Table table = this.service.getMgClient().getTableById(className);
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class,className);
				return json.jsonValue();
			}

			if (DataChange.isEmpty(table.getTrackFields())) {
				json.setUnSuccess(-1, table.getDisplayName() + LabelDisplay.get("未设置铭感属性", json.getLan()), true);
				return json.jsonValue();
			}

			HashMap<String, String> hash = new HashMap<String, String>();

			Vector<String> fv = Field.split(table.getTrackFields());
			if (fv.size() > 0) {
				SelectBidding sb = new SelectBidding();
				Field field = new Field();
				field.setClassName(className);
				field.setPropertyType(1L);
				QueryListInfo<Field> fList = this.service.getMgClient().getList(field, "property",this.service);
				for (Field x : fList.getList()) {
					if (!fv.contains(x.getProperty()))
						continue;
					hash.put(x.getProperty(), x.getDisplay());
					sb.put(x.getProperty(), x.getDisplay() + "[" + x.getProperty() + "]");
				}
				super.selectToJson(sb, json, BizFieldModifyTrack.class.getSimpleName(), "property");
			}

			String idValue = json.getSelectedId(Constants.current_sys, URI+"/trackFetch", className, "",
					true,this.getService());

			PageInfo page = json.getPageInfo(BizFieldModifyTrack.class);
			
			BizFieldModifyTrack search = json.getSearch(BizFieldModifyTrack.class, null, ut, this.service);
			search.setClassName(className);
			search.setIdValue(idValue);
			QueryFetchInfo<BizFieldModifyTrack> fetch = this.service.getMgClient().getFetch(search,
					"property,!changeDate", page.getCurrentPage(), page.getPageSize(),this.service);

			super.fetchToJson(fetch, json, BosConstants.getTable(BizFieldModifyTrack.class));

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
