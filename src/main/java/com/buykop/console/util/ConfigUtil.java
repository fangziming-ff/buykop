package com.buykop.console.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.apache.commons.io.FileUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.DiyInfBatch;
import com.buykop.framework.entity.DiyInfBatchItem;
import com.buykop.framework.entity.DiyInfField;
import com.buykop.framework.entity.DiyInfMsgPush;
import com.buykop.framework.entity.DiyInfOutField;
import com.buykop.framework.entity.DiyInfQuery;
import com.buykop.framework.entity.DiyInfQueryOperation;
import com.buykop.framework.entity.DiyInfSub;
import com.buykop.framework.entity.DiyInfSynField;
import com.buykop.framework.entity.FileUpload;
import com.buykop.framework.entity.SynTableDataConfig;
import com.buykop.framework.entity.SynTableDataItem;
import com.buykop.framework.entity.wf.WorkFlow;
import com.buykop.framework.entity.wf.WorkFlowCC;
import com.buykop.framework.entity.wf.WorkFlowNode;
import com.buykop.console.entity.product.Product;
import com.buykop.console.entity.product.ProductMenuDiy;
import com.buykop.console.entity.product.ProductPage;
import com.buykop.console.entity.product.RPageInf;
import com.buykop.console.entity.product.RPageInfBatch;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PMemberType;
import com.buykop.framework.oauth2.PRRoleFun;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.DBScript;
import com.buykop.framework.scan.ExportTemplate;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.FieldCalculationFormula;
import com.buykop.framework.scan.PFormInField;
import com.buykop.framework.scan.PFormMsgPush;
import com.buykop.framework.scan.PFormOutField;
import com.buykop.framework.scan.PFormQuery;
import com.buykop.framework.scan.PFormQueryOperation;
import com.buykop.framework.scan.Index;
import com.buykop.framework.scan.ChartForm;
import com.buykop.framework.scan.PClob;
import com.buykop.framework.scan.PDBFun;
import com.buykop.framework.scan.PDBProc;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PFormAction;
import com.buykop.framework.scan.PFormDiy;
import com.buykop.framework.scan.PFormField;
import com.buykop.framework.scan.PFormRowAction;
import com.buykop.framework.scan.PFormSlave;
import com.buykop.framework.scan.InputCheck;
import com.buykop.framework.scan.MsgTemplate;
import com.buykop.framework.scan.PMapTJConfig;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PServiceUri;
import com.buykop.framework.scan.PSysCode;
import com.buykop.framework.scan.PSysCodeType;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.Statement;
import com.buykop.framework.scan.TreeSelectForm;
import com.buykop.framework.scan.Where;
import com.buykop.framework.scan.WhereGroup;
import com.buykop.framework.scan.Table;
import com.buykop.framework.scan.TableESConfig;
import com.buykop.framework.scan.TableESPropertyConfig;
import com.buykop.framework.scan.TableRedicSubConfig;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.JsonUtil;
import com.buykop.framework.util.query.BaseQuery;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;

public class ConfigUtil {
	
	private static Logger  logger=LoggerFactory.getLogger(ConfigUtil.class);
	
	
	public static SelectBidding getIcoJson(String className,String idValue,ServiceInf service) throws Exception{
		
		SelectBidding  bidding=new SelectBidding();
		
		FileUpload fu=new FileUpload();
		fu.setClassName(className);
		fu.setIdValue(idValue);
		QueryListInfo<FileUpload> list=service.getList(fu,"srcName");
		
		for(FileUpload f:list.getList()) {
			bidding.put(f.getFileId(), f.getSrcName());
		}
		
		return bidding;
	}

	public static String getModelClass(String sys, String model, ServiceInf service) throws Exception {
		PRoot root = service.getMgClient().getById(sys, PRoot.class);
		if (root == null)
			throw new Exception("子系统:" + sys + " 不存在");
		if (DataChange.isEmpty(root.getPackagePath()))
			new Exception("子系统:" + sys + "打包路径未定义");
		return root.getPackagePath() + "." + model;
	}

	public static JSONObject saveToJson(String sys, ServiceInf service) throws Exception {

		PRoot root = service.getMgClient().getById(sys, PRoot.class);
		if (root == null) {
			return new JSONObject(true);
		}

		JSONObject json = new JSONObject(true);
		
		
		StringBuffer sb=new StringBuffer();

		if (true) {
			JSONArray xArr = new JSONArray();
			xArr.add(root.getSimpleDBJson(false));
			json.put(PRoot.class.getName(), xArr);
		}

		List<PClob> clList = new ArrayList<PClob>();

		if (!BosConstants._sysV.contains(sys)) {

			QueryListInfo<PSysCode> cList = new QueryListInfo<PSysCode>();

			PSysCodeType s = new PSysCodeType();
			s.setSys(sys);
			QueryListInfo<PSysCodeType> xList = service.getMgClient().getList(s,service);
			BosConstants.debug("PSysCodeType 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			for (PSysCodeType x : xList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
				PSysCode psc = new PSysCode();
				psc.setType(x.getType());
				QueryListInfo<PSysCode> tList = service.getMgClient().getList(psc,service);
				for (PSysCode t : tList.getList()) {
					cList.getList().add(t);
				}
			}
			if (xArr.size() > 0) {
				json.put(PSysCodeType.class.getName(), xArr);
			}

			//
			xArr = new JSONArray();
			for (PSysCode x : cList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(PSysCode.class.getName(), xArr);
			}
		}
		
		
		List<FieldCalculationFormula> fcfList=new ArrayList<FieldCalculationFormula>();
		List<TableESConfig> esList = new ArrayList<TableESConfig>();
		List<TableESPropertyConfig> espList = new ArrayList<TableESPropertyConfig>();
		List<TableRedicSubConfig> rdsList = new ArrayList<TableRedicSubConfig>();
		
		
		
		
		

	/**	if (BosConstants._sysV.contains(sys)) {
			
			JSONArray tableArr = new JSONArray();
			// 不允许自动建表
			Table search = new Table();
			search.setSys(sys);
			QueryListInfo<Table> list = service.getMgClient().getList(search,service);
			for (Table t : list.getList()) {
				
				
				if(true) {
					FieldCalculationFormula s=new FieldCalculationFormula();
					s.setfClassName(t.getClassName());
					QueryListInfo<FieldCalculationFormula> fList=service.getList(s, null);
					for(FieldCalculationFormula x1:fList.getList()) {
						fcfList.add(x1);
					}
					
				}
				
				
				if (true) {

					TableESConfig s1 = new TableESConfig();
					s1.setTable(t.getClassName());
					QueryListInfo<TableESConfig> s1List = service.getMgClient().getList(s1,service);
					for (TableESConfig x : s1List.getList()) {
						esList.add(x);
					}

				}

				if (true) {

					TableESPropertyConfig s1 = new TableESPropertyConfig();
					s1.setTable(t.getClassName());
					QueryListInfo<TableESPropertyConfig> s1List = service.getMgClient().getList(s1,service);
					for (TableESPropertyConfig x : s1List.getList()) {
						espList.add(x);
					}

				}

				
				
				
				if (true) {
					TableRedicSubConfig s1 = new TableRedicSubConfig();
					s1.setTable(t.getClassName());
					QueryListInfo<TableRedicSubConfig> s1List = service.getMgClient().getList(s1,service);
					for (TableRedicSubConfig x : s1List.getList()) {
						rdsList.add(x);
					}

				}
				
				
				
				try {

					t = Table.initByMongo(t.getClassName(), service);
					BosConstants.debug(
							"  " + t.getCode() + "   " + t.getDisplayName() + "  fields=" + t.getFields().size());
					BosConstants.debug(t._jsonValue());

					tableArr.add(t._jsonValue());

				} catch (Exception e) {
					BosConstants.debug(t.getClassName() + " 存储JSON表结构失败： " + e.getLocalizedMessage());
				}
				
				
			}
			
			
			BosConstants.debug("-------------存储本系统的表对象 len=" + list.size() + "--------------------");
			if (tableArr.size() > 0) {
				json.put(Table.class.getName(), tableArr);
			}
			
			
			
			

		} else {// 系统配置的系统,不允许建表
			
		*/
		if(true) {
			
			QueryListInfo<Field> fieldList = new QueryListInfo<Field>();
			QueryListInfo<WhereGroup> groupList = new QueryListInfo<WhereGroup>();
			QueryListInfo<Where> whereList = new QueryListInfo<Where>();
			QueryListInfo<Statement> stList = new QueryListInfo<Statement>();
			QueryListInfo<Index> idxList = new QueryListInfo<Index>();
		

			JSONArray tableArr = new JSONArray();
			
			Table search = new Table();
			search.setSys(sys);
			QueryListInfo<Table> list = service.getMgClient().getList(search,service);
			
			sb.append("table size="+list.size()+"\n");
			for (Table t : list.getList()) {
				// 只是部分人工部分

				if (true) {
					Field s1 = new Field();
					s1.setClassName(t.getClassName());
					s1.setRegType(1L);
					QueryListInfo<Field> s1List = service.getMgClient().getTableFieldList(s1, "property");
					for (Field x : s1List.getList()) {
						fieldList.getList().add(x);
					}
				}
				
				
				if(true) {
					FieldCalculationFormula s=new FieldCalculationFormula();
					s.setfClassName(t.getClassName());
					QueryListInfo<FieldCalculationFormula> fList=service.getList(s, null);
					for(FieldCalculationFormula x1:fList.getList()) {
						fcfList.add(x1);
					}
					
				}
				
				
				
				if (true) {

					TableESConfig s1 = new TableESConfig();
					s1.setTable(t.getClassName());
					QueryListInfo<TableESConfig> s1List = service.getMgClient().getList(s1,service);
					for (TableESConfig x : s1List.getList()) {
						esList.add(x);
					}

				}

				if (true) {

					TableESPropertyConfig s1 = new TableESPropertyConfig();
					s1.setTable(t.getClassName());
					QueryListInfo<TableESPropertyConfig> s1List = service.getMgClient().getList(s1,service);
					for (TableESPropertyConfig x : s1List.getList()) {
						espList.add(x);
					}

				}

				
				
				
				if (true) {
					TableRedicSubConfig s1 = new TableRedicSubConfig();
					s1.setTable(t.getClassName());
					QueryListInfo<TableRedicSubConfig> s1List = service.getMgClient().getList(s1,service);
					for (TableRedicSubConfig x : s1List.getList()) {
						rdsList.add(x);
					}

				}
				
				

				if (true) {
					WhereGroup s1 = new WhereGroup();
					s1.setClassName(t.getClassName());
					s1.setRegType(1L);
					QueryListInfo<WhereGroup> s1List = service.getMgClient().getTableWhereGroupList(s1, "ids");
					for (WhereGroup x : s1List.getList()) {
						groupList.getList().add(x);
					}
				}

				if (true) {
					Where s1 = new Where();
					s1.setClassName(t.getClassName());
					s1.setRegType(1L);
					QueryListInfo<Where> s1List = service.getMgClient().getTableWhereList(s1, "makeup");
					for (Where x : s1List.getList()) {
						whereList.getList().add(x);
					}
				}

				if (true) {
					Statement s1 = new Statement();
					s1.setClassName(t.getClassName());
					s1.setRegType(1L);
					QueryListInfo<Statement> s1List = service.getMgClient().getTableStatementList(s1, "id");
					for (Statement x : s1List.getList()) {
						stList.getList().add(x);
					}
				}

				if (true) {
					Index s1 = new Index();
					s1.setClassName(t.getClassName());
					s1.setRegType(1L);
					QueryListInfo<Index> s1List = service.getMgClient().getTableIndexList(s1, "seq");
					for (Index x : s1List.getList()) {
						idxList.getList().add(x);
					}
				}

				
				
				
				try {

					t = Table.initByMongo(t.getClassName(), service);
					BosConstants.debug(
							"  " + t.getCode() + "   " + t.getDisplayName() + "  fields=" + t.getFields().size());
					BosConstants.debug(t._jsonValue());

					tableArr.add(t._jsonValue());

				} catch (Exception e) {
					BosConstants.debug(t.getClassName() + " 存储JSON表结构失败： " + e.getLocalizedMessage());
				}
				

			}
			
			
			
			BosConstants.debug("-------------存储本系统的表对象 len=" + list.size() + "--------------------");
			if (tableArr.size() > 0) {
				json.put(Table.class.getName(), tableArr);
			}
			

			if (true) {
				JSONArray arr = new JSONArray();
				for (Index x : idxList.getList()) {
					arr.add(x.getSimpleDBJson(false));
				}
				if (arr.size() > 0) {
					json.put(Index.class.getName(), arr);
				}
			}

			if (true) {
				JSONArray arr = new JSONArray();
				for (Statement x : stList.getList()) {
					arr.add(x.getSimpleDBJson(false));
				}
				if (arr.size() > 0) {
					json.put(Statement.class.getName(), arr);
				}
			}

			if (true) {
				JSONArray arr = new JSONArray();
				for (Where x : whereList.getList()) {
					arr.add(x.getSimpleDBJson(false));
				}
				if (arr.size() > 0) {
					json.put(Where.class.getName(), arr);
				}
			}

			if (true) {
				JSONArray arr = new JSONArray();
				for (WhereGroup x : groupList.getList()) {
					arr.add(x.getSimpleDBJson(false));
				}
				if (arr.size() > 0) {
					json.put(WhereGroup.class.getName(), arr);
				}
			}

			
			if (true) {
				JSONArray arr = new JSONArray();
				for (Field x : fieldList.getList()) {
					arr.add(x.getSimpleDBJson(false));
				}
				if (arr.size() > 0) {
					json.put(Field.class.getName(), arr);
				}
			}

		}
		
		
		if(fcfList.size()>0) {
			JSONArray arr = new JSONArray();
			for(FieldCalculationFormula x1:fcfList) {
				arr.add(x1.getSimpleDBJson(false));
			}
			json.put(FieldCalculationFormula.class.getName(), arr);
		}
		
		
		if (true) {
			JSONArray arr = new JSONArray();
			for (TableESConfig x : esList) {
				arr.add(x.getSimpleDBJson(false));
			}
			if (arr.size() > 0) {
				json.put(TableESConfig.class.getName(), arr);
			}
		}

		if (true) {
			JSONArray arr = new JSONArray();
			for (TableESPropertyConfig x : espList) {
				arr.add(x.getSimpleDBJson(false));
			}
			if (arr.size() > 0) {
				json.put(TableESPropertyConfig.class.getName(), arr);
			}
		}


		
		if (true) {
			JSONArray arr = new JSONArray();
			for (TableRedicSubConfig x : rdsList) {
				arr.add(x.getSimpleDBJson(false));
			}
			if (arr.size() > 0) {
				json.put(TableRedicSubConfig.class.getName(), arr);
			}
		}
		
		
		
		
		
		//---------------------------------------
		if(true) {
			
			List<ProductMenuDiy> menuList=new ArrayList<ProductMenuDiy>();
			List<ProductPage> pageList=new ArrayList<ProductPage>();
			List<RPageInf> infList=new ArrayList<RPageInf>();
			List<RPageInfBatch> infBatchList=new ArrayList<RPageInfBatch>();
			
			
			JSONArray parr = new JSONArray();
			
			Product p=new Product();
			p.setSys(sys);
			QueryListInfo<Product> list=service.getList(p, null);
			
			sb.append("Product size="+list.size()+"\n");
			
			for(Product x:list.getList()) {
				
				if(true) {//菜单
					ProductMenuDiy s=new ProductMenuDiy();
					s.setProductId(x.getProductId());
					s.setStatus(1L);
					QueryListInfo<ProductMenuDiy> list1=service.getList(s, "sort");
					for(ProductMenuDiy x1:list1.getList()) {
						menuList.add(x1);
					}
					
				}
				
				if(true) {
					ProductPage  s=new ProductPage();
					s.setProductId(x.getProductId());
					QueryListInfo<ProductPage> list1=service.getList(s, "pageCode");
					for(ProductPage x1:list1.getList()) {
						pageList.add(x1);
					}
				}
				
				if(true) {
					RPageInf  s=new RPageInf();
					s.setProductId(x.getProductId());
					QueryListInfo<RPageInf> list1=service.getList(s, "infCode");
					for(RPageInf x1:list1.getList()) {
						infList.add(x1);
					}
				}
				
				
				if(true) {
					RPageInfBatch  s=new RPageInfBatch();
					s.setProductId(x.getProductId());
					QueryListInfo<RPageInfBatch> list1=service.getList(s, "infCode");
					for(RPageInfBatch x1:list1.getList()) {
						infBatchList.add(x1);
					}
				}
				
				
				parr.add(x.getSimpleDBJson(false));

				
			}
			
		
			if (true) {
				if (parr.size() > 0) {
					json.put(Product.class.getName(), parr);
				}
			}
			
			
			if (true) {
				JSONArray arr = new JSONArray();
				for (ProductMenuDiy x : menuList) {
					arr.add(x.getSimpleDBJson(false));
				}
				if (arr.size() > 0) {
					json.put(ProductMenuDiy.class.getName(), arr);
				}
			}
			
			
			if (true) {
				JSONArray arr = new JSONArray();
				for (ProductPage x : pageList) {
					arr.add(x.getSimpleDBJson(false));
				}
				if (arr.size() > 0) {
					json.put(ProductPage.class.getName(), arr);
				}
			}
			
			
			
			if (true) {
				JSONArray arr = new JSONArray();
				for (RPageInf x : infList) {
					arr.add(x.getSimpleDBJson(false));
				}
				if (arr.size() > 0) {
					json.put(RPageInf.class.getName(), arr);
				}
			}
			
			
			if (true) {
				JSONArray arr = new JSONArray();
				for (RPageInfBatch x : infBatchList) {
					arr.add(x.getSimpleDBJson(false));
				}
				if (arr.size() > 0) {
					json.put(RPageInfBatch.class.getName(), arr);
				}
			}
			
			
			
		}
		
		
		

		// ---------------------------------------------------------------------------------------
		/**if (true) {
			PServiceUri uri = new PServiceUri();
			uri.setSys(sys);
			uri.setRegType(1L);
			QueryListInfo<PServiceUri> uriList = service.getMgClient().getList(uri,service);
			BosConstants.debug("PServiceUri 导出  记录   size=" + uriList.size());
			JSONArray uriArr = new JSONArray();
			for (PServiceUri x : uriList.getList()) {
				uriArr.add(x.getSimpleDBJson(false));
			}
			if (uriArr.size() > 0) {
				json.put(PServiceUri.class.getName(), uriArr);
			}
		}*/
		
		
		
		

		if (true) {
			PSysParam param = new PSysParam();
			param.setMemberId("1");
			param.setSys(sys);
			param.setRegType(1L);
			QueryListInfo<PSysParam> xlist = service.getMgClient().getList(param,service);
			
			sb.append("PSysParam size="+xlist.size()+"\n");
			
			JSONArray xArr = new JSONArray();
			for (PSysParam x : xlist.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(PSysParam.class.getName(), xArr);
			}
		}
		
		

		if (true) {
			DBScript param = new DBScript();
			param.setSys(sys);
			QueryListInfo<DBScript> xlist = service.getMgClient().getList(param,service);
			JSONArray xArr = new JSONArray();
			for (DBScript x : xlist.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(DBScript.class.getName(), xArr);
			}
		}

		if (true) {
			MsgTemplate param = new MsgTemplate();
			param.setSys(sys);
			QueryListInfo<MsgTemplate> xlist = service.getMgClient().getList(param,service);
			JSONArray xArr = new JSONArray();
			for (MsgTemplate x : xlist.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(MsgTemplate.class.getName(), xArr);
			}
		}

		if (true) {
			PTreeForm param = new PTreeForm();
			param.setSys(sys);
			QueryListInfo<PTreeForm> xlist = service.getMgClient().getList(param,service);
			JSONArray xArr = new JSONArray();
			for (PTreeForm x : xlist.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(PTreeForm.class.getName(), xArr);
			}
		}

		if (true) {
			TreeSelectForm param = new TreeSelectForm();
			param.setSys(sys);
			QueryListInfo<TreeSelectForm> xlist = service.getMgClient().getList(param,service);
			JSONArray xArr = new JSONArray();
			for (TreeSelectForm x : xlist.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(TreeSelectForm.class.getName(), xArr);
			}
		}

		if (true) {
			InputCheck s = new InputCheck();
			s.setSys(sys);
			s.setRegType(1L);
			QueryListInfo<InputCheck> xList = service.getMgClient().getList(s,service);
			BosConstants.debug("PInputCheck 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			for (InputCheck x : xList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(InputCheck.class.getName(), xArr);
			}
		}

		if (true) {

			PMemberType mt = new PMemberType();
			mt.setSys(sys);
			QueryListInfo<PMemberType> xList = service.getMgClient().getList(mt,service);
			BosConstants.debug("PMemberType 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			for (PMemberType x : xList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(PMemberType.class.getName(), xArr);
			}

		}

		if (true) {

			QueryListInfo<PRRoleFun> fList = new QueryListInfo<PRRoleFun>();

			PRole mt = new PRole();
			mt.setSys(sys);
			QueryListInfo<PRole> xList = service.getMgClient().getList(mt,service);
			BosConstants.debug("PRole 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			for (PRole x : xList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
				PRRoleFun fun = new PRRoleFun();
				fun.setRoleId(x.getRoleId());
				QueryListInfo<PRRoleFun> funList = service.getMgClient().getList(fun,service);
				for (PRRoleFun x1 : funList.getList()) {
					fList.getList().add(x1);
				}
			}
			if (xArr.size() > 0) {
				json.put(PRole.class.getName(), xArr);
			}

			xArr = new JSONArray();
			for (PRRoleFun x1 : fList.getList()) {
				xArr.add(x1.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(PRRoleFun.class.getName(), xArr);
			}

		}
		
		
		
		
		if(true) {
			
			List<SynTableDataItem> itemList=new ArrayList<SynTableDataItem>();
			
			
			JSONArray xArr = new JSONArray();
			SynTableDataConfig config=new SynTableDataConfig();
			BaseQuery bq=new BaseQuery();
			bq.setType(0);
			bq.setFields("srcSys,sys");
			bq.setValue(sys);
			config.getQueryList().add(bq);
			QueryListInfo<SynTableDataConfig> list=service.getList(config, null);
			for(SynTableDataConfig x:list.getList()) {
				
				SynTableDataItem item=new SynTableDataItem();
				item.setClassName(x.getSrcClassName());
				QueryListInfo<SynTableDataItem> iList=service.getList(item, null);
				for(SynTableDataItem s:iList.getList()) {
					itemList.add(s);
				}
				xArr.add(x.getSimpleDBJson(false));
			}
			
			if(xArr.size()>0) {
				json.put(SynTableDataConfig.class.getName(), xArr);
			}
			
			
			if(itemList.size()>0) {
				xArr = new JSONArray();
				for (SynTableDataItem x1 : itemList) {
					xArr.add(x1.getSimpleDBJson(false));
				}
				if(xArr.size()>0) {
					json.put(SynTableDataItem.class.getName(), xArr);
				}
			}
		}
		
		
		
		
		

		if (true) {
			PDBFun s = new PDBFun();
			s.setSys(sys);
			QueryListInfo<PDBFun> xList = service.getMgClient().getList(s,service);
			BosConstants.debug("PDBFun 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			for (PDBFun x : xList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(PDBFun.class.getName(), xArr);
			}
		}

		if (true) {
			PDBProc s = new PDBProc();
			s.setSys(sys);
			QueryListInfo<PDBProc> xList = service.getMgClient().getList(s,service);
			BosConstants.debug("PDBProc 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			for (PDBProc x : xList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(PDBProc.class.getName(), xArr);
			}
		}

		if (true) {
			ExportTemplate s = new ExportTemplate();
			s.setSys(sys);
			QueryListInfo<ExportTemplate> xList = service.getMgClient().getList(s,service);
			BosConstants.debug("ExcelExportTemplate 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			for (ExportTemplate x : xList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(ExportTemplate.class.getName(), xArr);
			}
		}
		
		
		if(true) {
			
			
			
			List<WorkFlowCC> ccList = new ArrayList<WorkFlowCC>();

			List<WorkFlowNode> nodeList = new ArrayList<WorkFlowNode>();
			
			
			JSONArray xArr = new JSONArray();
			
			WorkFlow s = new WorkFlow();
			s.setSys(sys);
			QueryListInfo<WorkFlow> xList = service.getMgClient().getList(s,service);
			
			sb.append("WorkFlow size="+xList.size()+"\n");
			
			for(WorkFlow x:xList.getList()) {
				
				xArr.add(x.getSimpleDBJson(false));
				
				if(true) {
					WorkFlowCC ds=new WorkFlowCC();
					ds.setFlowId(x.getFlowId());
					QueryListInfo<WorkFlowCC> dList=service.getList(ds, null);
					for(WorkFlowCC x1:dList.getList()) {
						ccList.add(x1);
					}
				}
				
				
				
				if(true) {
					WorkFlowNode ds=new WorkFlowNode();
					ds.setFlowId(x.getFlowId());
					QueryListInfo<WorkFlowNode> dList=service.getList(ds, null);
					for(WorkFlowNode x1:dList.getList()) {
						nodeList.add(x1);
					}
				}
				
				
			}
			
			
			
			if (xArr.size() > 0) {
				json.put(WorkFlow.class.getName(), xArr);
			}
			
			
			
			if (ccList.size() > 0) {
				xArr = new JSONArray();
				for (WorkFlowCC x : ccList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(WorkFlowCC.class.getName(), xArr);
				}
			}
			
			
			
			if (nodeList.size() > 0) {
				xArr = new JSONArray();
				for (WorkFlowNode x : nodeList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(WorkFlowNode.class.getName(), xArr);
				}
			}
			
		}
		
		
		
		
		
		
		
		
		
		
		
		
		

		if (true) {

			List<PFormField> fieldList = new ArrayList<PFormField>();

			List<PFormSlave> slaveList = new ArrayList<PFormSlave>();

			List<PFormAction> actionList = new ArrayList<PFormAction>();
			List<PFormRowAction> rowActionList = new ArrayList<PFormRowAction>();

			List<PFormDiy> diyList = new ArrayList<PFormDiy>();

			List<PFormInField> inputList = new ArrayList<PFormInField>();
			List<PFormOutField> outputList = new ArrayList<PFormOutField>();
			
			List<PFormQuery> qList = new ArrayList<PFormQuery>();
			
			
			List<PFormQuery> bqList = new ArrayList<PFormQuery>();
			List<PFormQueryOperation> operationList = new ArrayList<PFormQueryOperation>();
			List<PFormMsgPush> msgList = new ArrayList<PFormMsgPush>();

			

			PForm s = new PForm();
			s.setSys(sys);
			QueryListInfo<PForm> xList = service.getMgClient().getList(s,service);
			
			sb.append("PForm size="+xList.size()+"\n");
			
			BosConstants.debug("PForm 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			for (PForm x : xList.getList()) {

				if (!DataChange.isEmpty(x.getScriptId())) {
					PClob content = service.getMgClient().getById(x.getScriptId(), PClob.class);
					if (content != null && !DataChange.isEmpty(content.getContent())) {
						clList.add(content);
					}
				}

				if (!DataChange.isEmpty(x.getOnloadScriptId())) {
					PClob content = service.getMgClient().getById(x.getOnloadScriptId(), PClob.class);
					if (content != null && !DataChange.isEmpty(content.getContent())) {
						clList.add(content);
					}
				}

				if (!DataChange.isEmpty(x.getSubmitScriptId())) {
					PClob content = service.getMgClient().getById(x.getSubmitScriptId(), PClob.class);
					if (content != null && !DataChange.isEmpty(content.getContent())) {
						clList.add(content);
					}
				}

				xArr.add(x.getSimpleDBJson(false));
				
				
				if(true) {
					PFormInField ds=new PFormInField();
					ds.setId(x.getFormId());
					QueryListInfo<PFormInField> dList=service.getList(ds, null);
					for(PFormInField x1:dList.getList()) {
						inputList.add(x1);
					}
				}
				
				
				if(true) {
					PFormOutField ds=new PFormOutField();
					ds.setId(x.getFormId());
					QueryListInfo<PFormOutField> dList=service.getList(ds, null);
					for(PFormOutField x1:dList.getList()) {
						outputList.add(x1);
					}
				}
				
				
				
				if (true) {// 查询条件
					PFormQuery bq = new PFormQuery();
					bq.setId(x.getFormId());
					QueryListInfo<PFormQuery> dList = service.getList(bq, "seq");
					for(PFormQuery x1:dList.getList()) {
						bqList.add(x1);
					}
				}

				

				if (true) {// 属性操作类型
					PFormQueryOperation bq = new PFormQueryOperation();
					bq.setId(x.getFormId());
					QueryListInfo<PFormQueryOperation> dList = service.getList(bq, "field");
					for(PFormQueryOperation x1:dList.getList()) {
						operationList.add(x1);
					}
				}

				if (true) {// 消息推送
					PFormMsgPush bq = new PFormMsgPush();
					bq.setId(x.getFormId());
					QueryListInfo<PFormMsgPush> dList = service.getList(bq, "sort");
					for(PFormMsgPush x1:dList.getList()) {
						msgList.add(x1);
					}
				}
				
				

				PFormField fs = new PFormField();
				fs.setFormId(x.getFormId());
				QueryListInfo<PFormField> fList = service.getMgClient().getList(fs, "seq",service);
				for (PFormField x1 : fList.getList()) {
					fieldList.add(x1);
				}

				PFormSlave ss = new PFormSlave();
				ss.setFormId(x.getFormId());
				QueryListInfo<PFormSlave> sList = service.getMgClient().getList(ss,service);
				for (PFormSlave x1 : sList.getList()) {
					slaveList.add(x1);
				}

				PFormAction pa = new PFormAction();
				pa.setFormId(x.getFormId());
				QueryListInfo<PFormAction> paList = service.getMgClient().getList(pa, "seq",service);
				for (PFormAction x1 : paList.getList()) {
					actionList.add(x1);
				}

				PFormDiy diy = new PFormDiy();
				diy.setFormId(x.getFormId());
				QueryListInfo<PFormDiy> diList = service.getMgClient().getList(diy, "action",service);
				for (PFormDiy x1 : diList.getList()) {
					diyList.add(x1);
				}

				PFormRowAction ra = new PFormRowAction();
				ra.setFormId(x.getFormId());
				QueryListInfo<PFormRowAction> raList = service.getMgClient().getList(ra, "sort",service);
				for (PFormRowAction x1 : raList.getList()) {
					rowActionList.add(x1);
				}
				
				
				
				if(true) {
					PFormQuery dif = new PFormQuery();
					dif.setFormId(x.getFormId());
					QueryListInfo<PFormQuery> dList = service.getList(dif, "seq");
					for (PFormQuery x1 : dList.getList()) {
						qList.add(x1);
					}
				}
				
				

			}
			
			if (xArr.size() > 0) {
				json.put(PForm.class.getName(), xArr);
			}
			
			
			
			
			
			if (inputList.size() > 0) {
				xArr = new JSONArray();
				for (PFormInField x : inputList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormInField.class.getName(), xArr);
				}
			}
			
			if (outputList.size() > 0) {
				xArr = new JSONArray();
				for (PFormOutField x : outputList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormOutField.class.getName(), xArr);
				}
			}
			
			
			if (bqList.size() > 0) {
				xArr = new JSONArray();
				for (PFormQuery x : bqList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormQuery.class.getName(), xArr);
				}
			}
			
			
			if (operationList.size() > 0) {
				xArr = new JSONArray();
				for (PFormQueryOperation x : operationList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormQueryOperation.class.getName(), xArr);
				}
			}
			
			if (msgList.size() > 0) {
				xArr = new JSONArray();
				for (PFormMsgPush x : msgList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormMsgPush.class.getName(), xArr);
				}
			}
			

			if (fieldList.size() > 0) {
				xArr = new JSONArray();
				for (PFormField x : fieldList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormField.class.getName(), xArr);
				}
			}

			if (slaveList.size() > 0) {
				xArr = new JSONArray();
				for (PFormSlave x : slaveList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormSlave.class.getName(), xArr);
				}
			}

			if (actionList.size() > 0) {
				xArr = new JSONArray();
				for (PFormAction x : actionList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormAction.class.getName(), xArr);
				}
			}

			if (diyList.size() > 0) {
				xArr = new JSONArray();
				for (PFormDiy x : diyList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormDiy.class.getName(), xArr);
				}
			}

			if (rowActionList.size() > 0) {
				xArr = new JSONArray();
				for (PFormRowAction x : rowActionList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormRowAction.class.getName(), xArr);
				}
			}

			if(qList.size()>0) {
				
				xArr = new JSONArray();
				for (PFormQuery x :qList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(PFormQuery.class.getName(), xArr);
				}
			}
			
		}

		if (true) {

			ChartForm s = new ChartForm();
			s.setSys(sys);
			QueryListInfo<ChartForm> xList = service.getMgClient().getList(s,service);
			sb.append("ChartForm size="+xList.size()+"\n");
			BosConstants.debug("PChartForm 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			for (ChartForm x : xList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(ChartForm.class.getName(), xArr);
			}

		}

		if (true) {

			PMapTJConfig s = new PMapTJConfig();
			s.setSys(sys);
			QueryListInfo<PMapTJConfig> xList = service.getMgClient().getList(s,service);
			BosConstants.debug("PMapTJConfig 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			for (PMapTJConfig x : xList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(PMapTJConfig.class.getName(), xArr);
			}
		}

		if (true) {

			List<DiyInfField> inputList = new ArrayList<DiyInfField>();
			List<DiyInfOutField> outputList = new ArrayList<DiyInfOutField>();
			List<DiyInfSub> subList = new ArrayList<DiyInfSub>();
			List<DiyInfQuery> bqList = new ArrayList<DiyInfQuery>();
			List<DiyInfSynField> synList = new ArrayList<DiyInfSynField>();
			
			List<DiyInfQueryOperation> opList = new ArrayList<DiyInfQueryOperation>();
			
			List<DiyInfMsgPush> msgList = new ArrayList<DiyInfMsgPush>();
			
			
			

			DiyInf s = new DiyInf();
			s.setSys(sys);
			QueryListInfo<DiyInf> xList = service.getMgClient().getList(s,service);
			BosConstants.debug("DiyInf 导出  记录   size=" + xList.size());
			sb.append("DiyInf size="+xList.size()+"\n");
			
			JSONArray xArr = new JSONArray();
			for (DiyInf x : xList.getList()) {
				
				
				if(true) {
					
					DiyInfMsgPush ds=new DiyInfMsgPush();
					ds.setId(x.getCode());
					QueryListInfo<DiyInfMsgPush> dList=service.getList(ds, "sort");
					for (DiyInfMsgPush x1 : dList.getList()) {
						msgList.add(x1);
					}
				}
				
				
				
				if(true) {
					DiyInfField ds = new DiyInfField();
					ds.setId(x.getCode());
					QueryListInfo<DiyInfField> dList = service.getList(ds,null);
					for (DiyInfField x1 : dList.getList()) {
						inputList.add(x1);
					}
				}
				
				
				if(true) {
					DiyInfSynField ds = new DiyInfSynField();
					ds.setId(x.getCode());
					QueryListInfo<DiyInfSynField> dList = service.getList(ds,null);
					for (DiyInfSynField x1 : dList.getList()) {
						synList.add(x1);
					}
				}
				
				
				
				
				
				if(true) {
					DiyInfQuery ds = new DiyInfQuery();
					ds.setCode(x.getCode());
					QueryListInfo<DiyInfQuery> dList = service.getList(ds,null);
					for (DiyInfQuery x1 : dList.getList()) {
						bqList.add(x1);
					}
				}
				
				
				
				
				if(true) {
					DiyInfOutField ds = new DiyInfOutField();
					ds.setId(x.getCode());
					QueryListInfo<DiyInfOutField> dList = service.getList(ds, null);
					for (DiyInfOutField x1 : dList.getList()) {
						outputList.add(x1);
					}
				}
				

				if(true) {
					DiyInfSub ds = new DiyInfSub();
					ds.setCode(x.getCode());
					QueryListInfo<DiyInfSub> dList = service.getList(ds, null);
					for (DiyInfSub x1 : dList.getList()) {
						subList.add(x1);
					}
				}
				
				
				if(true) {
					
					DiyInfQueryOperation ds=new DiyInfQueryOperation();
					ds.setId(x.getCode());
					QueryListInfo<DiyInfQueryOperation> dList=service.getList(ds, null);
					for(DiyInfQueryOperation x1:dList.getList()) {
						opList.add(x1);
					}
				}
				
				xArr.add(x.getSimpleDBJson(false));
			}
			
			

			if (xArr.size() > 0) {
				json.put(DiyInf.class.getName(), xArr);
			}

			
			if (true) {
				xArr = new JSONArray();
				for (DiyInfField x : inputList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(DiyInfField.class.getName(), xArr);
				}
			}
			
			
			if (true) {
				xArr = new JSONArray();
				for (DiyInfQueryOperation x : opList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(DiyInfQueryOperation.class.getName(), xArr);
				}
			}
			
			if (true) {
				xArr = new JSONArray();
				for (DiyInfMsgPush x : msgList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(DiyInfMsgPush.class.getName(), xArr);
				}
			}
			
			
			
			if (true) {
				xArr = new JSONArray();
				for (DiyInfSynField x : synList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(DiyInfSynField.class.getName(), xArr);
				}
			}
			
			
			//List<DiyInfSynField> synList = new ArrayList<DiyInfSynField>();
			
			
			if (true) {
				xArr = new JSONArray();
				for (DiyInfQuery x : bqList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(DiyInfQuery.class.getName(), xArr);
				}
			}
			
			
			if (true) {
				xArr = new JSONArray();
				for (DiyInfOutField x : outputList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(DiyInfOutField.class.getName(), xArr);
				}
			}
			
			if (true) {
				xArr = new JSONArray();
				for (DiyInfSub x : subList) {
					xArr.add(x.getSimpleDBJson(false));
				}
				if (xArr.size() > 0) {
					json.put(DiyInfSub.class.getName(), xArr);
				}
			}

		}

		if (true) {

			DiyInfBatch s = new DiyInfBatch();
			s.setSys(sys);
			QueryListInfo<DiyInfBatch> xList = service.getMgClient().getList(s,service);
			sb.append("DiyInfBatch size="+xList.size()+"\n");
			BosConstants.debug("DiyInfBatch 导出  记录   size=" + xList.size());
			JSONArray xArr = new JSONArray();
			
			QueryListInfo<DiyInfBatchItem> itemList=new QueryListInfo<DiyInfBatchItem>();
			
			for (DiyInfBatch x : xList.getList()) {
				xArr.add(x.getSimpleDBJson(false));
				
				DiyInfBatchItem is = new DiyInfBatchItem();
				is.setId(x.getId());
				QueryListInfo<DiyInfBatchItem> ixList = service.getMgClient().getList(is,service);
				for(DiyInfBatchItem ix:ixList.getList()) {
					itemList.getList().add(ix);
				}
				
			}
			
			if (xArr.size() > 0) {
				json.put(DiyInfBatch.class.getName(), xArr);
			}
			
			
			
			if (true) {

				BosConstants.debug("DiyInfBatchItem 导出  记录   size=" + xList.size());
				JSONArray xArr1 = new JSONArray();
				for (DiyInfBatchItem x : itemList.getList()) {
					xArr1.add(x.getSimpleDBJson(false));
				}
				if (xArr1.size() > 0) {
					json.put(DiyInfBatchItem.class.getName(), xArr1);
				}
			}
			
		}

		

		if (true) {

			JSONArray xArr = new JSONArray();
			for (PClob x : clList) {
				xArr.add(x.getSimpleDBJson(false));
			}
			if (xArr.size() > 0) {
				json.put(PClob.class.getName(), xArr);
			}
		}
		
		
		
		
		

		HashMap<String, String> jsonHash = new HashMap<String, String>();

		if (json.size() > 0) {

			String jsonS = json.toString();

			if (jsonHash.containsKey(sys)) {
				String value = jsonHash.get(sys);
				if (jsonS.equals(value)) {
					return json;
				}
			}
			jsonHash.put(sys, jsonS);

			String path = System.getProperty("user.dir") + File.separator + "config" + File.separator + "export"
					+ File.separator + sys + File.separator + NetWorkTime.getCurrentDateString();
			File dir = new File(path);
			if (!dir.exists()) {
				dir.mkdirs();
			}

			BosConstants.debug("export json config path =" + path);

			File file = new File(path + File.separator + sys + "_"
					+ NetWorkTime.getCurrentDatetimeString().replaceAll(":", "") + ".json");

			FileUtils.writeStringToFile(file, json.toString(), BosConstants.LOCAL_CODE);
		}
		
		
		logger.info("saveToJson", "SYS:"+sys+"  导出:\n"+sb.toString());
		

		return json;

	}

	/**
	 * 加载配置
	 * 
	 * @param sys
	 */
	public static void initByJsonFile(String sys, File jsonFile, ServiceInf service) throws Exception {

		if (!jsonFile.exists())
			return;

		FileInputStream is = null;

		//RdClient conn = service.getRdClient();
		
		
		StringBuffer sb=new StringBuffer();

		try {

			is = new FileInputStream(jsonFile);

			InputStreamReader input = new InputStreamReader(is, "UTF-8");

			BufferedReader in = new BufferedReader(input);
			StringBuffer buffer = new StringBuffer();
			String line = " ";
			while ((line = in.readLine()) != null) {
				buffer.append(line);
			}
			input.close();
			in.close();

			String ret = buffer.toString();

			if (DataChange.isEmpty(ret)) {
				throw new Exception("在导入文件:" + sys + ".json文件 为空");
			}

			JSONObject json = JsonUtil.getInstance().string2Json(ret);
			if (json == null || json.size() == 0) {
				throw new Exception("在导入文件:" + sys + ".json文件 为空");
			}

			Vector<String> typeV = new Vector<String>();

			java.util.Iterator<String> its = json.keySet().iterator();
			while (its.hasNext()) {
				String key = its.next();

				JSONArray arr = json.getJSONArray(key);
				
				
				sb.append("导入:"+key+"  size="+arr.size());
				

				for (int i = 0; i < arr.size(); i++) {
					
					
					JSONObject x = arr.getJSONObject(i);
					
					try {
					
						
	
						if (key.equals(Table.class.getName())) {
	
							// List<BosEntity> objV=new ArrayList<BosEntity>();
	
							Table table = new Table();
							table._init(x);
							table.setEsIdxVersion(null);// es版本不要去更新
							service.getMgClient().deleteByPK(table.getPk(), Table.class,null,service);
							service.save(table, null);
	
							for (Field f : table.getFields()) {
								f.setClassName(table.getClassName());
								service.getMgClient().deleteByPK(f.getPk(), Field.class,null,service);
								service.save(f, null);
							}
	
	
							for (Index f : table.getIdxList()) {
								f.setClassName(table.getClassName());
								service.getMgClient().deleteByPK(f.getPk(), Index.class,null,service);
								service.save(f, null);
							}
	
							try {
	
								Table x1 = Table.initByMongo(table.getClassName(), service);
								BosConstants.tableHash.put(x1.getClassName(), x1);
	
							} catch (Exception e) {
								BosConstants.debug(table.getClassName() + " 加载 JSON表结构失败: " + e.getLocalizedMessage());
							}
	
							// 加入内存
						} else if (key.equals(PFormAction.class.getName())) {
							PFormAction f = new PFormAction();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PFormAction.class,null,service);
							service.save(f, null);
							
							
							
							
						} else if (key.equals(DiyInfMsgPush.class.getName())) {
							DiyInfMsgPush f = new DiyInfMsgPush();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DiyInfMsgPush.class,null,service);
							service.save(f, null);		
						} else if (key.equals(DiyInfQuery.class.getName())) {
							
							DiyInfQuery f = new DiyInfQuery();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DiyInfQuery.class,null,service);
							service.save(f, null);
							
							
						} else if (key.equals(PFormDiy.class.getName())) {
							PFormDiy f = new PFormDiy();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PFormDiy.class,null,service);
							service.save(f, null);
							
							
							
							
						} else if (key.equals(PFormQuery.class.getName())) {
							PFormQuery f = new PFormQuery();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PFormQuery.class,null,service);
							service.save(f, null);
							
							
						} else if (key.equals(PFormQueryOperation.class.getName())) {
							PFormQueryOperation f = new PFormQueryOperation();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PFormQueryOperation.class,null,service);
							service.save(f, null);
							
						} else if (key.equals(PFormMsgPush.class.getName())) {
							PFormMsgPush f = new PFormMsgPush();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PFormMsgPush.class,null,service);
							service.save(f, null);
							
						} else if (key.equals(SynTableDataConfig.class.getName())) {
							SynTableDataConfig f = new SynTableDataConfig();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), SynTableDataConfig.class,null,service);
							service.save(f, null);
							
						} else if (key.equals(SynTableDataItem.class.getName())) {
							SynTableDataItem f = new SynTableDataItem();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), SynTableDataItem.class,null,service);
							service.save(f, null);
							
						} else if (key.equals(ExportTemplate.class.getName())) {
							ExportTemplate f = new ExportTemplate();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), ExportTemplate.class,null,service);
							service.save(f, null);
							
						} else if (key.equals(PDBFun.class.getName())) {
							PDBFun f = new PDBFun();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PDBFun.class,null,service);
							service.save(f, null);
							
						} else if (key.equals(FieldCalculationFormula.class.getName())) {
							FieldCalculationFormula f = new FieldCalculationFormula();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), FieldCalculationFormula.class,null,service);
							service.save(f, null);
							
						} else if (key.equals(DiyInf.class.getName())) {
							DiyInf f = new DiyInf();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DiyInf.class,null,service);
							service.save(f, null);
							
							
						} else if (key.equals(DiyInfQueryOperation.class.getName())) {
							DiyInfQueryOperation f = new DiyInfQueryOperation();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DiyInfQueryOperation.class,null,service);
							service.save(f, null);		
							
						} else if (key.equals(DiyInfField.class.getName())) {
							DiyInfField f = new DiyInfField();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DiyInfField.class,null,service);
							service.save(f, null);	
							
						} else if (key.equals(DiyInfOutField.class.getName())) {
							DiyInfOutField f = new DiyInfOutField();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DiyInfOutField.class,null,service);
							service.save(f, null);		
							
						} else if (key.equals(DiyInfSynField.class.getName())) {
							DiyInfSynField f = new DiyInfSynField();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DiyInfSynField.class,null,service);
							service.save(f, null);			
						} else if (key.equals(DiyInfSub.class.getName())) {
							DiyInfSub f = new DiyInfSub();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DiyInfSub.class,null,service);
							service.save(f, null);		
	
						} else if (key.equals(DiyInfBatch.class.getName())) {
							DiyInfBatch f = new DiyInfBatch();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DiyInfBatch.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(DiyInfBatchItem.class.getName())) {
							DiyInfBatchItem f = new DiyInfBatchItem();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DiyInfBatchItem.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(MsgTemplate.class.getName())) {
							MsgTemplate f = new MsgTemplate();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), MsgTemplate.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(TableESConfig.class.getName())) {
							TableESConfig f = new TableESConfig();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), TableESConfig.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(TableESPropertyConfig.class.getName())) {
							TableESPropertyConfig f = new TableESPropertyConfig();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), TableESPropertyConfig.class,null,service);
							service.save(f, null);
							
							
						} else if (key.equals(TableRedicSubConfig.class.getName())) {
							TableRedicSubConfig f = new TableRedicSubConfig();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), TableRedicSubConfig.class,null,service);
							service.save(f, null);	
							
						} else if (key.equals(PDBProc.class.getName())) {
							PDBProc f = new PDBProc();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PDBProc.class,null,service);
							service.save(f, null);
						} else if (key.equals(PClob.class.getName())) {
							PClob f = new PClob();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PClob.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(DBScript.class.getName())) {
							DBScript f = new DBScript();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), DBScript.class,null,service);
							service.save(f, null);
						} else if (key.equals(PFormRowAction.class.getName())) {
							PFormRowAction f = new PFormRowAction();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PFormRowAction.class,null,service);
							service.save(f, null);
						} else if (key.equals(PRoot.class.getName())) {
							PRoot f = new PRoot();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PRoot.class,null,service);
							service.save(f, null);
						} else if (key.equals(PSysCode.class.getName())) {
							PSysCode f = new PSysCode();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PSysCode.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(PMapTJConfig.class.getName())) {
	
							PMapTJConfig f = new PMapTJConfig();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PMapTJConfig.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(Field.class.getName())) {
							Field f = new Field();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), Field.class,null,service);
							service.save(f, null);
							
						}else if (key.equals(Product.class.getName())) {
							Product f = new Product();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), Product.class,null,service);
							service.save(f, null);			
						} else if (key.equals(ProductMenuDiy.class.getName())) {
							ProductMenuDiy f = new ProductMenuDiy();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), ProductMenuDiy.class,null,service);
							service.save(f, null);			
						} else if (key.equals(ProductPage.class.getName())) {
							ProductPage f = new ProductPage();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), ProductPage.class,null,service);
							service.save(f, null);			
						}  else if (key.equals(RPageInf.class.getName())) {
							RPageInf f = new RPageInf();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), RPageInf.class,null,service);
							service.save(f, null);			
						} else if (key.equals(RPageInfBatch.class.getName())) {
							RPageInfBatch f = new RPageInfBatch();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), RPageInfBatch.class,null,service);
							service.save(f, null);			
						} else if (key.equals(WorkFlow.class.getName())) {
							WorkFlow f = new WorkFlow();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), WorkFlow.class,null,service);
							service.save(f, null);			
						} else if (key.equals(WorkFlowCC.class.getName())) {
							WorkFlowCC f = new WorkFlowCC();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), WorkFlowCC.class,null,service);
							service.save(f, null);			
						} else if (key.equals(WorkFlowNode.class.getName())) {
							WorkFlowNode f = new WorkFlowNode();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), WorkFlowNode.class,null,service);
							service.save(f, null);			
						} else if (key.equals(WhereGroup.class.getName())) {
							WhereGroup f = new WhereGroup();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), WhereGroup.class,null,service);
							service.save(f, null);			
						} else if (key.equals(Where.class.getName())) {
							Where f = new Where();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), Where.class,null,service);
							service.save(f, null);			
						} else if (key.equals(Statement.class.getName())) {
							Statement f = new Statement();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), Statement.class,null,service);
							service.save(f, null);			
						} else if (key.equals(Index.class.getName())) {
							Index f = new Index();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), Index.class,null,service);
							service.save(f, null);
						} else if (key.equals(PServiceUri.class.getName())) {
	
							PServiceUri f = new PServiceUri();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PServiceUri.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(PSysParam.class.getName())) {
	
							PSysParam f = new PSysParam();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PSysParam.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(PRole.class.getName())) {
	
							PRole f = new PRole();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PRole.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(PRRoleFun.class.getName())) {
	
							PRRoleFun f = new PRRoleFun();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PRRoleFun.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(PTreeForm.class.getName())) {
	
							PTreeForm f = new PTreeForm();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PTreeForm.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(TreeSelectForm.class.getName())) {
	
							TreeSelectForm f = new TreeSelectForm();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), TreeSelectForm.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(PMemberType.class.getName())) {
	
							PMemberType f = new PMemberType();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PMemberType.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(InputCheck.class.getName())) {
							InputCheck f = new InputCheck();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), InputCheck.class,null,service);
							service.save(f, null);
	
						} else if (key.equals(PSysCodeType.class.getName())) {
	
							PSysCodeType f = new PSysCodeType();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PSysCodeType.class,null,service);
							service.save(f, null);
	
							if (!typeV.contains(f.getType())) {
								if (f.getType() != null)
									typeV.add(f.getType());
							}
	
						} else if (key.equals(PFormField.class.getName())) {
							PFormField f = new PFormField();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PFormField.class,null,service);
							service.save(f, null);
						} else if (key.equals(PFormSlave.class.getName())) {
							PFormSlave f = new PFormSlave();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PFormSlave.class,null,service);
							service.save(f, null);
						} else if (key.equals(ChartForm.class.getName())) {
							ChartForm f = new ChartForm();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), ChartForm.class,null,service);
							service.save(f, null);
						} else if (key.equals(PForm.class.getName())) {
							PForm f = new PForm();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PForm.class,null,service);
							service.save(f, null);
						}else if (key.equals(PFormInField.class.getName())) {
							PFormInField f = new PFormInField();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PFormInField.class,null,service);
							service.save(f, null);
						}else if (key.equals(PFormOutField.class.getName())) {
							PFormOutField f = new PFormOutField();
							f.initByJson(x, false);
							service.getMgClient().deleteByPK(f.getPk(), PFormOutField.class,null,service);
							service.save(f, null);
						}
					}catch(Exception e) {
						logger.error("导入失败:"+key+"      json:"+x.toJSONString(), e);
					}

				}

			}

			for (String type : typeV) {

				PSysCode ps = new PSysCode();
				ps.setType(type);
				QueryListInfo<PSysCode> list = service.getMgClient().getList(ps, "codeOrder,code",service);
				CacheTools.setSysCode(type, list.getList());
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (is != null) {
				is.close();
			}
		}

		
		logger.info("initByJsonFile", "SYS:"+sys+"  导入:\n"+sb.toString());
		

	}

	public static void main(String[] args) {

		JSONObject json = new JSONObject();
		json.put("test", "测试");

		JSONObject data = (JSONObject) json.clone();
		data.put("test", "测试1");

		System.out.println(json.toJSONString() + "     " + data.toJSONString());

	}

}
