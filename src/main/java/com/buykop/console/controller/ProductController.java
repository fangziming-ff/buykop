package com.buykop.console.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.console.entity.product.PageTesting;
import com.buykop.console.entity.product.PageTime;
import com.buykop.console.entity.product.Product;
import com.buykop.console.entity.product.ProductMenuDiy;
import com.buykop.console.entity.product.ProductPage;
import com.buykop.console.entity.product.RPageInf;
import com.buykop.console.entity.product.RPageInfBatch;
import com.buykop.console.service.ProductService;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.DiyInfBatch;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.MQProcessing;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysCode;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.Statement;
import com.buykop.framework.scan.Table;
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


@Module(display = "产品开发管理", sys = Constants.current_sys)
@RestController
public class ProductController extends BaseController{
	
	
	private static Logger  logger=LoggerFactory.getLogger(ProductController.class);
	
	protected static final String URI="/product";
	
	
	private static final String TREE_BIZ_CATALOG="TREE_BIZ_CATALOG";
	
	@Autowired
	private ProductService service;

	
	protected PTreeForm loadTree(String parentIdValue,String productId,String bizClassName,HttpEntity json,UserToken token) throws Exception{
		
		if(DataChange.isEmpty(bizClassName)) {
			bizClassName=ProductMenuDiy.class.getName();
		}
		
		PTreeForm  obj=new PTreeForm();
 		obj.setCode(TREE_BIZ_CATALOG);
 		obj.setName("产品菜单目录树");
 		obj.setSys(Constants.current_sys);
 		obj.setClassName(ProductMenuDiy.class.getName());
 		obj.setBizClassName(bizClassName);
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
		
		ProductMenuDiy search=new ProductMenuDiy();
		search.setpId(parentIdValue);
		search.setProductId(productId);
		QueryListInfo<ProductMenuDiy> list=this.service.getMgClient().getList(search, "sort,menuName",this.service);
		
		SelectBidding bidding=list.getSelectBidding(this.service);
		json.getData().put("TREE",bidding.showJSON());
		obj.setRootId(parentIdValue);
		obj.setRootName("根目录");
		super.objToJson(obj, json);
		
		return obj;
	}
	
	
	
	
	@Security(accessType = "0", displayName = "菜单树形结构", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/tree", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject tree(@RequestBody HttpEntity json,HttpServletRequest request, @RequestHeader String token) throws Exception{ 

		json.setSys(Constants.current_sys);
		json.setUri(URI + "/tree");

		if (DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		

		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String productId=json.getSimpleData("productId", "产品", String.class, true, service);
			String rootId=json.getSimpleData("rootId", "根节点id", String.class, false, service);
			if(DataChange.isEmpty(rootId)) {
				rootId="0";
			}
			ProductMenuDiy parent=this.service.getById(rootId, ProductMenuDiy.class);
			if(parent==null) {
				rootId="0";
			}
			
			
			Product product=this.service.getById(productId, Product.class);
			if(product==null) {
				json.setUnSuccessForNoRecord(Product.class);
				return json.jsonValue();
			}
			
			ProductMenuDiy pmd=new ProductMenuDiy();
			pmd.setProductId(productId);
			pmd.setStatus(1L);
			QueryListInfo<ProductMenuDiy> list=this.service.getList(pmd, "sort");
			
			
			
			HashMap<String,ProductMenuDiy> hash=new HashMap<String,ProductMenuDiy>();
			
			List<ProductMenuDiy> rootList=new ArrayList<ProductMenuDiy>();
			
			for(ProductMenuDiy x:list.getList()) {
				hash.put(x.getMenuId(), x);
			}
			
			
			for(ProductMenuDiy x:list.getList()) {
				String pId=x.getpId();
				if(DataChange.isEmpty(pId)) {
					pId="0";
				}
				if(pId.equals(rootId)) {
					rootList.add(x);
				}else {
					ProductMenuDiy p=hash.get(pId);
					if(p!=null) {
						p.getSubList().add(x);
					}
				}
			}
			
			
			JSONObject jo=new JSONObject(true);
			JSONArray arr=new JSONArray();
			for(ProductMenuDiy x:rootList) {
				//如果子节点为空,就不要
				JSONObject jx=x.getTreeJson(ut);
				if(jx!=null) {
					arr.add(jx);
				}
			}
			
			
			jo.put("id", rootId);
			if(!rootId.equals("0")) {
				jo.put("text", parent.getMenuName());
			}else {
				jo.put("text", "根节点");
			}
			
			jo.put("children", arr);
			
			
			json.getData().put("TREE", jo);
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	//--------------------------------------------测试-------------------------
	@Menu(js = "pageTest", name = "测试任务", trunk = "开发服务,产品研发")
	@Security(accessType = "1", displayName = "测试任务列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_testing)
	@RequestMapping(value = URI+"/myTest", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject myTest(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			Product p=new Product();
			p.setTestUserId(ut.getUserId());
			p.setStatus(2);
			Vector<String> pv=this.service.getVector(p, "productId");
			
			ProductPage search=json.getSearch(ProductPage.class, null, ut,this.service);
			BaseQuery bq=new BaseQuery();
			bq.setType(1);
			bq.setFields("productId");
			bq.setValue(MyString.CombinationBy(pv, ","));
			search.getQueryList().add(bq);
			
			bq=new BaseQuery();
			bq.setType(1);
			bq.setFields("status");
			bq.setValue("3,4,5");
			search.getQueryList().add(bq);
			
			
			PageInfo page=json.getPageInfo(Product.class);
			QueryFetchInfo<ProductPage> fetch=this.service.getFetch(search, "!testTime,productId,pageCode", page.getCurrentPage(), page.getPageSize());
			for(ProductPage x:fetch.getList()) {
				PageTime time=this.service.getById(x.getTimeId(), PageTime.class);
				if(time!=null) {
					x.setDevStartTime(time.getStartTime());
					x.setDevEndTime(time.getEndTime());
				}
			}
			super.fetchToJson(fetch, json, BosConstants.getTable(ProductPage.class.getName()));
			
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "重启开发", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_testing)
	@RequestMapping(value = URI+"/reOpen", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject reOpen(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			//String id=json.getSelectedId(Constants.current_sys, URI+"/reopen", ProductPage.class.getName(), null, true, service);
	
			ProductPage obj=json.getObj(ProductPage.class, "userId", service);
			
			
			ProductPage src=this.service.getById(obj.getPageId(),ProductPage.class);
			if(src==null) {
				json.setUnSuccessForNoRecord(ProductPage.class);
				return json.jsonValue();
			}
			if(src.getStatus()!=5) {
				json.setUnSuccess(-1, "不能开重启开发");
				return json.jsonValue();
			}
			obj.setStatus(2);
			this.service.save(obj, ut);
			
			
			
			List<PageTesting> testList=json.getList(PageTesting.class, "bugLevel,remark", service);
			for(PageTesting x:testList) {
				PageTesting xs=this.service.getById(x.getTestId(),PageTesting.class);
				x.setTestUserId(ut.getUserId());
				x.setUserId(src.getUserId());
				x.setTestingBatch(src.getTestingBatch());
				x.setPageId(obj.getPageId());
				if(xs==null) {
					x.setBugTime(NetWorkTime.getCurrentDatetime());
					x.setStatus(0);
				}
				//bugTime status
				this.service.save(x, ut);
			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "开始测试", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_testing)
	@RequestMapping(value = URI+"/startTest", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject startTest(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/startTest", ProductPage.class.getName(), null, true, service);
	
			ProductPage obj=this.service.getById(id, ProductPage.class);
			
			if(obj==null) {
				json.setUnSuccessForNoRecord(ProductPage.class);
				return json.jsonValue();
			}
			// 0:未开发 1:开发中  2:返工   3:提测   4:测试中   5:完成
			if(obj.getStatus()==null) {
				obj.setStatus(0);
			}
			
			if(obj.getStatus()!=3) {
				json.setUnSuccess(-1, "页面未提测,不能开始测试");
				return json.jsonValue();
			}
			
			
			Product product=this.service.getById(obj.getProductId(), Product.class);
			if(product==null) {
				json.setUnSuccessForNoRecord(Product.class);
				return json.jsonValue();
			}
			
			if(!ut.getUserId().equals(product.getTestUserId())) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}
			
			
			obj.setTestTime(NetWorkTime.getCurrentDatetime());
			
			
			if(true) {
				PageTesting test=new PageTesting();
				test.setPageId(id);
				long count=this.service.getCount(test);
				if(count==0) {
					obj.setTestingBatch(0);
				}else {
					if(obj.getTestingBatch()==null) {
						obj.setTestingBatch(1);
					}else {
						obj.setTestingBatch(1+obj.getTestingBatch());
					}
				}
			}
			obj.setStatus(4);
			this.service.save(obj, ut);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "结束测试", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_testing)
	@RequestMapping(value = URI+"/endTest", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject endTest(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
		
	
			ProductPage obj=json.getObj(ProductPage.class, "testResult", service);
			
			ProductPage src=this.service.getById(obj.getPageId(), ProductPage.class);
			if(src==null) {
				json.setUnSuccessForNoRecord(ProductPage.class);
				return json.jsonValue();
			}
			Product product=this.service.getById(src.getProductId(), Product.class);
			if(product==null) {
				json.setUnSuccessForNoRecord(Product.class);
				return json.jsonValue();
			}
			
			if(!ut.getUserId().equals(product.getTestUserId())) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}
			
			// 0:未开发 1:开发中  2:返工   3:提测   4:测试中   5:完成
			if(obj.getStatus()==null) {
				obj.setStatus(0);
			}
			
			if(obj.getStatus()!=4) {
				json.setUnSuccess(-1, "页面未开始测试,不能结束");
				return json.jsonValue();
			}
			
			
			if(obj.getTestResult()==1) {
				obj.setStatus(5);
			}else {
				obj.setStatus(1);
			}
			
			

			List<PageTesting> testList=json.getList(PageTesting.class, "bugLevel,remark", service);
			for(PageTesting x:testList) {
				PageTesting xs=this.service.getById(x.getTestId(),PageTesting.class);
				if(x.getBugLevel()==3) {
					if(src.getBackNum()==null) {
						obj.setBackNum(1);
					}else {
						obj.setBackNum(obj.getBackNum()+1);
					}
					obj.setStatus(2);
				}
				x.setTestUserId(ut.getUserId());
				x.setUserId(src.getUserId());
				x.setTestingBatch(src.getTestingBatch());
				x.setPageId(obj.getPageId());
				if(xs==null) {
					x.setBugTime(NetWorkTime.getCurrentDatetime());
					x.setStatus(0);
				}
				//bugTime status
				this.service.save(x, ut);
			}
			
			
			if(obj.getStatus()==5) {
				PageTime time=new PageTime();
				time.setPageId(obj.getPageId());
				String sum=this.service.getSum(time, "factHour");
				obj.setFactHour(DataChange.StringToInteger(sum));
				obj.setTestingBatch(obj.getTestingBatch()+1);
			}
			
			
			this.service.save(obj, ut);
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "保存测试", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_testing)
	@RequestMapping(value = URI+"/infoTest", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject infoTest(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/infoTest", ProductPage.class.getName(), null, true, service);
			ProductPage obj=this.service.getById(id, ProductPage.class);
			
			if(obj==null) {
				json.setUnSuccessForNoRecord(ProductPage.class);
				return json.jsonValue();
			}
			
			Product product=this.service.getById(obj.getProductId(), Product.class);
			if(product==null) {
				json.setUnSuccessForNoRecord(Product.class);
				return json.jsonValue();
			}
			
			if( !ut.getUserId().equals("1") &&   !ut.getUserId().equals(product.getTestUserId())) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}
			
			super.objToJson(obj, json);
			
			
			if(true) {
				PageTesting test=new PageTesting();
				test.setPageId(id);
				test.setTestingBatch(obj.getTestingBatch());
				QueryListInfo<PageTesting> list=this.service.getList(test, "!bugTime");
				super.listToJson(list, json, test.showTable());
			}
			
			
			
			if(true) {
				//历史测试记录
				PageTesting test=new PageTesting();
				test.setPageId(id);
				PageInfo page=json.getPageInfo("PageTesting1");
				QueryFetchInfo<PageTesting> fetch=this.service.getFetch(test, "!bugTime", page.getCurrentPage(), page.getPageSize());
				super.fetchToJson(fetch, json, "PageTesting1", test.showTable());
			}
			
			
			
			if(true) {
				PageTime time=new PageTime();
				time.setPageId(id);
				QueryListInfo<PageTesting> list=this.service.getList(time, "!startTime");
				super.listToJson(list, json, time.showTable());
			}
			
			
			
			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_tech,ut.getMemberId(), service), json, ProductPage.class.getName(), "userId");
			
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "保存测试", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_testing)
	@RequestMapping(value = URI+"/saveTest", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveTest(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
		
	
			ProductPage obj=json.getObj(ProductPage.class, "", service);
			
			ProductPage src=this.service.getById(obj.getPageId(), ProductPage.class);
			if(src==null) {
				json.setUnSuccessForNoRecord(ProductPage.class);
				return json.jsonValue();
			}
			Product product=this.service.getById(src.getProductId(), Product.class);
			if(product==null) {
				json.setUnSuccessForNoRecord(Product.class);
				return json.jsonValue();
			}
			
			if( !ut.getUserId().equals("1") &&  !ut.getUserId().equals(product.getTestUserId())) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}
			
			// 0:未开发 1:开发中  2:返工   3:提测   4:测试中   5:完成
			if(obj.getStatus()==null) {
				obj.setStatus(0);
			}
			
			if(obj.getStatus()!=4) {
				json.setUnSuccess(-1, "页面未开始测试,不能结束");
				return json.jsonValue();
			}
		

			List<PageTesting> testList=json.getList(PageTesting.class, "bugLevel,remark", service);
			for(PageTesting x:testList) {
				PageTesting xs=this.service.getById(x.getTestId(),PageTesting.class);
				x.setTestUserId(ut.getUserId());
				x.setUserId(src.getUserId());
				x.setTestingBatch(src.getTestingBatch());
				x.setPageId(obj.getPageId());
				if(xs==null) {
					x.setBugTime(NetWorkTime.getCurrentDatetime());
					x.setStatus(0);
				}
				//bugTime status
				this.service.save(x, ut);
			}
			
			this.service.save(obj, ut);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "删除测试", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_testing)
	@RequestMapping(value = URI+"/delTest", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delTest(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/delTest", PageTesting.class.getName(), null, true, service);
			
			PageTesting obj=this.service.getById(id, PageTesting.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PageTesting.class);
				return json.jsonValue();
			}
			
			if( !ut.getUserId().equals("1") &&  !obj.getTestUserId().equals(ut.getUserId())) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}
			
			this.service.deleteById(id, PageTesting.class.getName(), ut);
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	//--------------------------------------------咖啡-------------------------------
	
	
	
	@Menu(js = "pageTask", name = "开发任务", trunk = "开发服务,产品研发")
	@Security(accessType = "1", displayName = "产品列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = URI+"/myTask", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject myTask(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			ProductPage search=json.getSearch(ProductPage.class, null, ut,this.service);
			search.setUserId(ut.getUserId());
			// 0:未开发 1:开发中  2:返工   3:提测   4:测试中 5:完成
			PageInfo page=json.getPageInfo(Product.class);
			QueryFetchInfo<ProductPage> fetch=this.service.getMgClient().getFetch(search, "productId,pageCode", page.getCurrentPage(), page.getPageSize(),this.service);
			for(ProductPage x:fetch.getList()) {
				PageTime time=this.service.getById(x.getTimeId(), PageTime.class);
				if(time!=null) {
					x.setDevStartTime(time.getStartTime());
					x.setDevEndTime(time.getEndTime());
				}
			}
			super.fetchToJson(fetch, json, BosConstants.getTable(ProductPage.class.getName()));
			
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "页面提测", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = URI+"/submitTest", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject submitTest(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/submitTest", ProductPage.class.getName(), null, true, service);
			
			ProductPage obj=this.service.getById(id, ProductPage.class);
			
			if(obj==null) {
				json.setUnSuccessForNoRecord(ProductPage.class);
				return json.jsonValue();
			}
			// 0:未开发  1:开发中  2:返工   3:提测   4:测试中   5:完成
			if(obj.getStatus()==null) {
				obj.setStatus(0);
			}
			
			if(obj.getStatus()!=1 &&  obj.getStatus()!=2) {
				json.setUnSuccess(-1, "页面处于非开发状态,不能提测");
				return json.jsonValue();
			}
			
			if(!ut.getUserId().equals(obj.getUserId())) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}
			
			obj.setStatus(3);
			
			
			if(true) {
				PageTime time=this.service.getById(obj.getTimeId(), PageTime.class);
				time.setEndTime(NetWorkTime.getCurrentDatetime());
				long t=(time.getEndTime().getTime()-time.getStartTime().getTime())/(1000*3600);
				time.setFactHour( DataChange.StringToInteger(String.valueOf(t)) );
				this.service.save(time, ut);
			}
			
			
			
			if(true) {
				PageTime time=new PageTime();
				time.setPageId(obj.getPageId());
				String sum=this.service.getSum(time, "factHour");
				obj.setFactHour(DataChange.StringToInteger(sum));
			}
			
			
			this.service.save(obj, ut);
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "开始任务", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = URI+"/startTask", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject startTask(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/startTask", ProductPage.class.getName(), null, true, service);
			
			ProductPage obj=this.service.getById(id, ProductPage.class);
			
			if(obj==null) {
				json.setUnSuccessForNoRecord(ProductPage.class);
				return json.jsonValue();
			}
			// 0:未开发 1:开发中  2:返工   3:提测   4:测试中   5:完成
			if(obj.getStatus()==null) {
				obj.setStatus(0);
			}
			
			
			if(obj.getStatus()==0 ||  obj.getStatus()==2) {
				PageTime time=new PageTime();
				time.setTimeId(PageTime.next());
				time.setPageId(obj.getPageId());
				time.setStartTime(NetWorkTime.getCurrentDatetime());
				this.service.save(time, ut);
				obj.setTimeId(time.getTimeId());
				if(obj.getStatus()==0) {
					obj.setStartTime(time.getStartTime());
					obj.setStartYear(NetWorkTime.getCurrentYear());
					obj.setStartMonth(NetWorkTime.getCurrentMonth());
				}
				obj.setStatus(1);
			}else {
				json.setUnSuccess(-1,"任务状态有误,不能启动开发");
				return json.jsonValue();
			}
			
			this.service.save(obj, ut);
			
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
			
	
	
	
	@Security(accessType = "1", displayName = "任务详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = URI+"/infoTask", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject infoTask(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/infoTask", ProductPage.class.getName(), null, true, service);
			
			ProductPage obj=this.service.getById(id, ProductPage.class);
			
			if(obj==null) {
				json.setUnSuccessForNoRecord(ProductPage.class);
				return json.jsonValue();
			}
		
			super.objToJson(obj, json);
			
			
			if(true) {
				PageTesting test=new PageTesting();
				test.setPageId(id);
				test.setTestingBatch(obj.getTestingBatch());
				QueryListInfo<PageTesting> tList=this.service.getList(test, "!bugTime");
				super.listToJson(tList, json, test.showTable());
			}
			
			
			
			if(true) {
				PageTesting test=new PageTesting();
				test.setPageId(id);
				PageInfo page=json.getPageInfo("PageTesting1");
				QueryFetchInfo<PageTesting> fetch=this.service.getFetch(test, "!bugTime", page.getCurrentPage(), page.getPageSize());
				super.fetchToJson(fetch, json, "PageTesting1", test.showTable());
			}
			
			
			

			if(true) {
				PageTime time=new PageTime();
				time.setPageId(id);
				QueryListInfo<PageTesting> list=this.service.getList(time, "!startTime");
				super.listToJson(list, json, time.showTable());
			}
			
			
			
			
			QueryListInfo<DiyInf> infList=new QueryListInfo<DiyInf>();
			QueryListInfo<DiyInfBatch> batchList=new QueryListInfo<DiyInfBatch>();
			
			if(true) {
				RPageInf di = new RPageInf();
				di.setPageId(id);
				QueryListInfo<RPageInf> dList=this.service.getList(di, "infCode");
				for(RPageInf x:dList.getList()) {
					DiyInf o=this.service.getById(x.getInfCode(), DiyInf.class);
					if(o==null) continue;
					infList.getList().add(o);
				}
			}
			super.listToJson(infList, json, BosConstants.getTable(DiyInf.class));
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, DiyInf.class.getName(),"sys");
			
			if(true) {
				RPageInfBatch di = new RPageInfBatch();
				di.setPageId(id);
				QueryListInfo<RPageInfBatch> dList=this.service.getList(di, "infCode");
				for(RPageInfBatch x:dList.getList()) {
					DiyInfBatch o=this.service.getById(x.getInfCode(), DiyInfBatch.class);
					if(o==null) continue;
					batchList.getList().add(o);
				}
			}
			
			super.listToJson(batchList, json, BosConstants.getTable(DiyInfBatch.class));
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, DiyInfBatch.class.getName(),"sys");
			
			
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	//---------------------------------------页面统计------------------------------------------------
	
	
	
	@Menu(js = "pageEvaluation", name = "任务评估", trunk = "开发服务,产品研发")
	@Security(accessType = "1", displayName = "页面评估列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/pageEvaluation", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject pageEvaluation(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			//Product p=new Product();
			//p.setUserId(ut.getUserId());
			//Vector<String> pv=this.service.getVector(p, "productId");
			
			
			ProductPage search=json.getSearch(ProductPage.class, null, ut,this.service);
			if(search.getStartYear()==null) {
				search.setStartYear(NetWorkTime.getCurrentYear());
			}
			if(DataChange.isEmpty(search.getStartMonth())) {
				search.setStartMonth(NetWorkTime.getCurrentMonth());
			}
			//BaseQuery bq=new BaseQuery();
			//bq.setType(1);
			//bq.setFields("productId");
			//bq.setValue(MyString.CombinationBy(pv, ","));
			//search.getQueryList().add(bq);
			super.searchToJson(search, json, ProductPage.class.getSimpleName());
			
			
			BosConstants.debug("-------userId="+search.getUserId());
			
			if(!DataChange.isEmpty(search.getUserId())) {
				
				QueryListInfo<ProductPage> list=this.service.getMgClient().getList(search, "productId,pageCode",this.service);
				
				BigDecimal x1=new BigDecimal("0");
				BigDecimal x2=new BigDecimal("0");
				
				for(ProductPage x:list.getList()) {
					
					
					
					if(x.getPlanHour()==null) {
						x.setPlanHour(0);
					}
					if(x.getBackNum()==null) {
						x.setBackNum(0);
					}
					
					if(x.getTestResult()==null) {
						x.setTestResult(0);
					}
					
					
					if(x.getFactHour()==null || x.getFactHour()==0) {
						
						
					}else {
						//x1=DataChange.add(x1, x.getPlanHour());
						//BigDecimal p1=DataChange.mul(DataChange.mul(x.getPlanHour().intValue(), x.getTestResult().intValue()), 100);
						//BigDecimal p2=DataChange.mul(x.getFactHour().intValue(), x.getBackNum()+1);
						//x.setEvaluation(DataChange.div(p1, p2).intValue());
						//x2=DataChange.add(x2, DataChange.mul(x.getPlanHour(), x.getEvaluation()));
					}
					
				}
				
				
				super.listToJson(list, json, BosConstants.getTable(ProductPage.class.getName()));
				
				//BosConstants.debug("-----"+x2+"/"+x1+"----------");
				
				if(x1.intValue()!=0) {
					//json.getData().put("evaluation",  DataChange.div(x2, x1).intValue() );
				}
				
				
				
			}else {
				
				PageInfo page=json.getPageInfo(ProductPage.class);
				QueryFetchInfo<ProductPage> fetch=this.service.getMgClient().getFetch(search, "productId,pageCode", page.getCurrentPage(), page.getPageSize(),this.service);
				for(ProductPage x:fetch.getList()) {
					//(X1*Z2)/(X2*(Y2+1))
					
					if(x.getPlanHour()==null) {
						x.setPlanHour(0);
					}
					if(x.getBackNum()==null) {
						x.setBackNum(0);
					}
					if(x.getTestResult()==null) {
						x.setTestResult(0);
					}
					
					
					if(x.getFactHour()==null || x.getFactHour()==0) {
						
						
					}else {
						//BigDecimal p1=DataChange.mul(DataChange.mul(x.getPlanHour().intValue(), x.getTestResult().intValue()), 100);
						//BigDecimal p2=DataChange.mul(x.getFactHour().intValue(), x.getBackNum()+1);
						//x.setEvaluation(DataChange.div(p1, p2).intValue());
					}
					
				}
				super.fetchToJson(fetch, json, BosConstants.getTable(ProductPage.class.getName()));
				
				
				
			}
			
			
			
			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_tech,ut.getMemberId(), service), json, ProductPage.class.getName(), "userId");
	
			
			
			
			
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "任务详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/infoEvaluation", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject infoEvaluation(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/infoEvaluation", ProductPage.class.getName(), null, true, service);
			ProductPage obj=this.service.getById(id, ProductPage.class);
			
			if(obj==null) {
				json.setUnSuccessForNoRecord(ProductPage.class);
				return json.jsonValue();
			}
			
			Product product=this.service.getById(obj.getProductId(), Product.class);
			if(product==null) {
				json.setUnSuccessForNoRecord(Product.class);
				return json.jsonValue();
			}
			
			if( !ut.getUserId().equals("1") &&  !ut.getUserId().equals(product.getUserId())) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}
			
			super.objToJson(obj, json);
			
			
			if(true) {
				PageTesting test=new PageTesting();
				test.setPageId(id);
				QueryListInfo<PageTesting> list=this.service.getList(test, "!bugTime");
				super.listToJson(list, json, test.showTable());
			}
			
			
			
			if(true) {
				PageTime time=new PageTime();
				time.setPageId(id);
				QueryListInfo<PageTesting> list=this.service.getList(time, "!startTime");
				super.listToJson(list, json, time.showTable());
			}
			
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	
				
	
	//---------------------------------------------------产品------------------------------
	
	
	@Menu(js = "product", name = "产品管理", trunk = "开发服务,产品研发")
	@Security(accessType = "1", displayName = "产品列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			Product search=json.getSearch(Product.class, null, ut,this.service);
			PageInfo page=json.getPageInfo(Product.class);
			
			
			if(!ut.getUserId().equals("1")) {
				BaseQuery bq=new BaseQuery();
				bq.setType(0);
				bq.setFields("testUserId,leaderId,userId");
				bq.setValue(ut.getUserId());
				search.getQueryList().add(bq);
			}
			
			QueryFetchInfo<Product> fetch=this.service.getMgClient().getFetch(search, "productName", page.getCurrentPage(), page.getPageSize(),this.service);
			super.fetchToJson(fetch, json, BosConstants.getTable(Product.class.getName()));
			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_product_manager,ut.getMemberId(), service), json, Product.class.getName(), "userId");
			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_tech,ut.getMemberId(), service), json, Product.class.getName(), "leaderId");
			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_testing,ut.getMemberId(), service), json, Product.class.getName(), "testUserId");
			
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, Product.class.getName(),"sys");

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "菜单列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

	

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			

			List<Product> list=json.getList(Product.class, "productId,productName,!userId",this.service);
			for(Product x:list) {
				x.setProductId(x.getProductId().toUpperCase());
				this.service.save(x,ut);
				
				BosConstants.getExpireHash().remove(Product.class, x.getProductId());
				new ClassInnerNotice().invoke(Product.class.getSimpleName(), x.getProductId());
				
			}
			
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "产品删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

	

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/delete", Product.class.getName(), null, true, service);
			
			
			ProductMenuDiy diy=new ProductMenuDiy();
			diy.setProductId(id);
			if(this.service.getCount(diy)>0) {
				json.setUnSuccess(-1, "因产品下有菜单设置,故不能删除");
				return json.jsonValue();
			}
			
			
			this.service.deleteById(id, Product.class.getName(), ut);
			
			BosConstants.getExpireHash().remove(Product.class, id);
			new ClassInnerNotice().invoke(Product.class.getSimpleName(), id);
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	//------------------------------------------页面管理------------------------------------------------------------
	
	@Security(accessType = "1", displayName = "页面列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/pageList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject pageList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			String menuId=json.getSelectedId(Constants.current_sys, URI+"/pageList", ProductMenuDiy.class.getName(), null,this.service);
			
			
			
			PTreeForm tree=this.loadTree(menuId,productId,ProductPage.class.getName(), json, ut);

			if(DataChange.isEmpty(menuId)) {
				menuId=tree.getRootId();
			}
			
			
			BosConstants.debug("productId="+productId+"     parentId="+menuId);
			
			
			ProductPage search=json.getSearch(ProductPage.class, null, ut, this.service);
			search.setMenuId(menuId);
			search.setProductId(productId);
			QueryListInfo<ProductPage> list=this.service.getList(search, "pageCode");
			super.listToJson(list, json, search.showTable());
			
			
			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_tech,ut.getMemberId(), service), json, ProductPage.class.getName(), "userId");
			
	
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("页面列表", e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	
	
	
	@Security(accessType = "1", displayName = "页面列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/pageListSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject pageListSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			String menuId=json.getSelectedId(Constants.current_sys, URI+"/pageList", ProductMenuDiy.class.getName(), null,this.service);
			
			List<ProductPage> list=json.getList(ProductPage.class, null, this.service);
			for(ProductPage x:list) {
				x.setProductId(productId);
				x.setMenuId(menuId);
				this.service.save(x, ut);
			}
			
			
			BosConstants.getExpireHash().remove(Product.class, productId);
			new ClassInnerNotice().invoke(Product.class.getSimpleName(),productId);
			
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("页面列表保存", e);
		}
		
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "页面删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/pageDelete",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject pageDelete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			String menuId=json.getSelectedId(Constants.current_sys, URI+"/pageList", ProductMenuDiy.class.getName(), null,this.service);
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/pageDelete", ProductPage.class.getName(), null, true, service);
			
			RPageInf inf=new RPageInf();
			inf.setPageId(id);
			long count=this.service.getCount(inf);
			if(count>0) {
				json.setUnSuccess(-1, "该页面含有"+count+"个接口,请先删除接口再删除页面");
				return json.jsonValue();
			}
			
			
			RPageInfBatch infBatch=new RPageInfBatch();
			infBatch.setPageId(id);
			count=this.service.getCount(infBatch);
			if(count>0) {
				json.setUnSuccess(-1, "该页面含有"+count+"个批处理接口,请先删除批处理接口再删除页面");
				return json.jsonValue();
			}
			
			this.service.deleteById(id, ProductPage.class.getName(), ut);
			
			BosConstants.getExpireHash().remove(Product.class, productId);
			new ClassInnerNotice().invoke(Product.class.getSimpleName(), productId);
			
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("页面删除", e);
		}
		
		return json.jsonValue();
	}
	
	
	
	//---------------------------------------页面的接口-----------------------
	
	
	@Security(accessType = "1", displayName = "接口列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = URI+"/infList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject infList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			String menuId=json.getSelectedId(Constants.current_sys, URI+"/pageList", ProductMenuDiy.class.getName(), null,this.service);
			
			
			
			PTreeForm tree=this.loadTree(menuId,productId,ProductPage.class.getName(), json, ut);

			if(DataChange.isEmpty(menuId)) {
				menuId=tree.getRootId();
			}
			
			
			String pageId=json.getSelectedId(Constants.current_sys,  URI+"/infList", ProductPage.class.getName(), null, service);
			Product product=this.service.getById(productId, Product.class);
			if(product==null) {
				json.setUnSuccessForNoRecord(Product.class);
				return json.jsonValue();
			}
			
			
			if( !ut.getUserId().equals("1") &&    !ut.getUserId().equals(product.getLeaderId())  &&  !ut.getUserId().equals(product.getTestUserId())  &&  !ut.getUserId().equals(product.getUserId())   ) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}
			
			ProductPage obj=this.service.getById(pageId, ProductPage.class);
			super.objToJson(obj, json);
			
			
			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_tech,ut.getMemberId(), service), json, ProductPage.class.getName(), "userId");
			
			
			QueryListInfo<DiyInf> infList=new QueryListInfo<DiyInf>();
			QueryListInfo<DiyInfBatch> batchList=new QueryListInfo<DiyInfBatch>();
			
			if(true) {
				RPageInf di = new RPageInf();
				di.setPageId(pageId);
				QueryListInfo<RPageInf> dList=this.service.getList(di, "infCode");
				for(RPageInf x:dList.getList()) {
					DiyInf o=this.service.getById(x.getInfCode(), DiyInf.class);
					if(o==null) continue;
					infList.getList().add(o);
				}
			}
			super.listToJson(infList, json, BosConstants.getTable(DiyInf.class));
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, DiyInf.class.getName(),"sys");
			
			if(true) {
				RPageInfBatch di = new RPageInfBatch();
				di.setPageId(pageId);
				QueryListInfo<RPageInfBatch> dList=this.service.getList(di, "infCode");
				for(RPageInfBatch x:dList.getList()) {
					DiyInfBatch o=this.service.getById(x.getInfCode(), DiyInfBatch.class);
					if(o==null) continue;
					batchList.getList().add(o);
				}
			}
			
			super.listToJson(batchList, json, BosConstants.getTable(DiyInfBatch.class));
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, DiyInfBatch.class.getName(),"sys");
			
			
			
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("页面的接口列表", e);
		}
		
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "接口列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/infListSave",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject infListSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			String pageId=json.getSelectedId(Constants.current_sys,  URI+"/infList", ProductPage.class.getName(), null, service);
			
			ProductPage page=json.getObj(ProductPage.class, null, service);
			page.setPageId(pageId);
			this.service.save(page, ut);
			
			
			if(true) {
				RPageInf inf=new RPageInf();
				inf.setPageId(pageId);
				this.service.delete(inf, ut);
			}
			
			if(true) {
				RPageInfBatch inf=new RPageInfBatch();
				inf.setPageId(pageId);
				
				this.service.delete(inf, ut);
			}
			
			List<DiyInf> infList=json.getList(DiyInf.class, "sys,className,code,title,infType", service);
			for(DiyInf x:infList) {
				x.setUserId(page.getUserId());
				x.setCode(x.getCode().toUpperCase());
				
				DiyInf src=service.getById(x.getCode(), DiyInf.class);
				if(src!=null) {
					if(!src.getSys().equals(x.getSys())) {
						json.setUnSuccess(-1, "接口:"+x.getCode()+"已经存在,与本次输入信息有差异");
						return json.jsonValue();
					}
					if(!src.getClassName().equals(x.getClassName())) {
						json.setUnSuccess(-1, "接口:"+x.getCode()+"已经存在,与本次输入信息有差异");
						return json.jsonValue();
					}
					if(src.getInfType().intValue()!=x.getInfType().intValue()) {
						json.setUnSuccess(-1, "接口:"+x.getCode()+"已经存在,与本次输入信息有差异");
						return json.jsonValue();
					}
				}
				RPageInf inf=new RPageInf();
				inf.setPageId(pageId);
				inf.setInfCode(x.getCode());
				inf.setProductId(productId);
				this.service.save(inf, ut);
				
				this.service.save(x, ut);
			}
	
			
			List<DiyInfBatch> batchList=json.getList(DiyInfBatch.class, "id,sys,title", service);
			for(DiyInfBatch x:batchList) {
				x.setUserId(page.getUserId());
				x.setId(x.getId().toUpperCase());
				
				
				DiyInfBatch src=service.getById(x.getId(), DiyInfBatch.class);
				if(src!=null) {
					if(!src.getSys().equals(x.getSys())) {
						json.setUnSuccess(-1, "接口批处理:"+x.getId()+"已经存在,与本次输入信息有差异");
						return json.jsonValue();
					}
				}
				
				RPageInfBatch inf=new RPageInfBatch();
				inf.setPageId(pageId);
				inf.setInfCode(x.getId());
				inf.setProductId(productId);
				this.service.save(inf, ut);
				
				this.service.save(x, ut);
			}
			
			
			
			BosConstants.getExpireHash().remove(Product.class, productId);
			new ClassInnerNotice().invoke(Product.class.getSimpleName(),productId);

			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("页面的解列保存", e);
		}
		
		return json.jsonValue();
	}
	
	//---------------------------------------------------------------------------
	
	
	
	@Security(accessType = "1", displayName = "菜单列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/menuList",method = RequestMethod.POST)
	@ResponseBody
    public JSONObject menuList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		
		try {
			
			UserToken ut = super.securityCheck(json,request);
			
			if(ut==null) {
				return json.jsonValue();
			}
			
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			
			
			String parentId=json.getSelectedId(Constants.current_sys, URI+"/menuList", ProductMenuDiy.class.getName(), null,this.service);
			
			PTreeForm tree=this.loadTree(parentId,productId,ProductMenuDiy.class.getName(), json, ut);

			if(DataChange.isEmpty(parentId)) {
				parentId=tree.getRootId();
			}
			
			
			BosConstants.debug("productId="+productId+"     parentId="+parentId);
			
			ProductMenuDiy search=json.getSearch(ProductMenuDiy.class,null,ut,this.service);
			search.setProductId(productId);
			search.setpId(parentId);
			QueryListInfo<ProductMenuDiy> list=this.service.getMgClient().getList(search, "sort,menuName",this.service);
			super.listToJson(list, json, BosConstants.getTable(ProductMenuDiy.class));
			
			SelectBidding sb = new SelectBidding();
			sb.put("0", "--根目录--");
			
			ProductMenuDiy parentx=this.getService().getById(parentId, ProductMenuDiy.class);
			if(parentx!=null) {
				ProductMenuDiy p=this.getService().getById(parentx.getpId(), ProductMenuDiy.class);
				if(p!=null) {
					sb.put(p.getMenuId(),p.getMenuName()+"[上级目录]");
				}
				sb.put(parentId, parentx.getMenuName(), true);
				super.objToJson(parentx, "parent", json);
			}
			
			
			if(true) {
				for(ProductMenuDiy x:list.getList()) {
					if(DataChange.isEmpty(x.getpId())) {
						x.setpId("0");
					}
					sb.put(x.getMenuId(), x.getMenuName());
				}
			}
			
			super.selectToJson(sb, json, ProductMenuDiy.class.getSimpleName(),"parentId");
			
			json.setSuccess();
		}catch(Exception e){
			json.setUnSuccess(e);
			logger.error("目录列表", e);
		}
		
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "菜单列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/saveMenuList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveMenuList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

	

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			
			
			long seq=0;
			List<ProductMenuDiy> list=json.getList(ProductMenuDiy.class, "menuName,routerPath,status",this.service);
			for(ProductMenuDiy x:list) {
				x.setProductId(productId);
				x.setSort(seq++);
				this.service.save(x,ut);
			}
			
			
			
			JSONArray arr=new JSONArray();
			for(BosEntity x:list) {
				if(!x.existMust("menuName")) continue;
				arr.add(x.getTreeJson(2));
			}
			json.getData().put("TREEMODIFY", arr);
			
			
			
			BosConstants.getExpireHash().remove(Product.class, productId);
			new ClassInnerNotice().invoke(Product.class.getSimpleName(),productId);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "菜单显示新增", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/menuShowAdd", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject menuShowAdd(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			String parentId=json.getSelectedId(Constants.current_sys, URI+"/menuList", ProductMenuDiy.class.getName(), null,this.service);
			
			PTreeForm tree=this.loadTree(parentId,productId,ProductMenuDiy.class.getName(), json, ut);

			if(DataChange.isEmpty(parentId)) {
				parentId=tree.getRootId();
			}
			
			
			ProductMenuDiy obj=new ProductMenuDiy();
			obj.setProductId(productId);
			obj.setpId(parentId);
			super.objToJson(obj, json);
			
			
			super.selectToJson(PRole.getJsonForSys(null, true, this.service), json, ProductMenuDiy.class, "showRoles");
			
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "菜单详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/menuInfo", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject menuInfo(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			String parentId=json.getSelectedId(Constants.current_sys, URI+"/menuList", ProductMenuDiy.class.getName(), null,this.service);
			
			
			Product product=this.service.getById(productId, Product.class);
			if(product==null) {
				json.setUnSuccessForNoRecord(Product.class);
				return json.jsonValue();
			}
			
			
			PTreeForm tree=this.loadTree(parentId,productId,ProductMenuDiy.class.getName(), json, ut);

			if(DataChange.isEmpty(parentId)) {
				parentId=tree.getRootId();
			}
			
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/menuInfo", ProductMenuDiy.class.getName(), null, true,this.getService());
			ProductMenuDiy obj=this.service.getMgClient().getById(id, ProductMenuDiy.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(ProductMenuDiy.class);
				return json.jsonValue();
			}
			
			super.objToJson(obj, json);
			
			
			super.selectToJson(PRole.getJsonForSys(product.getSys(), false, this.service), json, ProductMenuDiy.class, "showRoles");

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1", displayName = "菜单删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/menuDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject menuDelete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

	

		try {

			

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			
			String id=json.getSelectedId(Constants.current_sys, URI+"/menuDelete", ProductMenuDiy.class.getName(), null, true,this.getService());
			ProductMenuDiy obj=this.service.getMgClient().getById(id, ProductMenuDiy.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(ProductMenuDiy.class);
				return json.jsonValue();
			}
		
			
			//查看下级菜单
			ProductMenuDiy s=new ProductMenuDiy();
			s.setpId(id);
			if(this.service.getCount(s)>0) {
				json.setUnSuccess(-1, "因含有下级菜单,故不能删除");
				return json.jsonValue();
			}

			this.service.deleteById(id, ProductMenuDiy.class.getName(), ut);
			
			
			BosConstants.getExpireHash().remove(Product.class, productId);
			new ClassInnerNotice().invoke(Product.class.getSimpleName(),productId);
			
			json.getData().put("TREEREMOVE",id);
			
			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	
	
	@Security(accessType = "1", displayName = "菜单单个保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_product_manager+","+BosConstants.role_tech_manager)
	@RequestMapping(value = URI+"/menuSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject menuSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		try {

			UserToken ut = super.securityCheck(json,request);

			if (ut == null) {
				return json.jsonValue();
			}
			
			
			
			String productId=json.getSelectedId(Constants.current_sys, URI+"/info", Product.class.getName(),null, true,this.service);
			
			ProductMenuDiy obj=json.getObj(ProductMenuDiy.class, "menuName",this.service);
			
			obj.setProductId(productId);
			if(DataChange.isEmpty(obj.getpId())) {
				obj.setpId("0");
			}
			this.service.save(obj, ut);
			
			
			
			JSONArray arr=new JSONArray();
			arr.add(obj.getTreeJson(2));
			json.getData().put("TREEMODIFY", arr);
			
			
			BosConstants.getExpireHash().remove(Product.class, productId);
			new ClassInnerNotice().invoke(Product.class.getSimpleName(),productId);
			
			
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
