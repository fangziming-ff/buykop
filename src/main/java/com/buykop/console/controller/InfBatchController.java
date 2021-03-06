package com.buykop.console.controller;

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
import com.buykop.console.entity.product.RPageInfBatch;
import com.buykop.console.service.InfService;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.DiyInfBatch;
import com.buykop.framework.entity.DiyInfBatchItem;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PRoot;
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

@Module(display = "???????????????????????????", sys = Constants.current_sys)
@RestController
public class InfBatchController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(InfBatchController.class);

	protected static final String URI = "/inf/batch/config";

	@Autowired
	private InfService service;

	@Menu(js = "infBatch", name = "?????????????????????", trunk = "????????????,????????????")
	@Security(accessType = "1", displayName = "??????????????????", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = URI + "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			DiyInfBatch search = json.getSearch(DiyInfBatch.class, null, ut, this.service);
			PageInfo page = json.getPageInfo(DiyInfBatch.class);
			QueryFetchInfo<BosEntity> fetch = this.service.getMgClient().getFetch(search, "id", page.getCurrentPage(),
					page.getPageSize(),this.service);
			super.fetchToJson(fetch, json, BosConstants.getTable(DiyInfBatch.class.getName()));
			super.selectToJson(PRoot.getJsonForSelect( this.service), json,
					DiyInfBatch.class.getName(), "sys");
			
			
			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_tech,ut.getMemberId(), service), json, DiyInfBatch.class.getName(), "userId");

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "?????????????????????????????????", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech+","+BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			List<DiyInfBatch> list = json.getList(DiyInfBatch.class, "id,sys,title,cache,status", this.service);
			for (DiyInfBatch x : list) {
				x.setId(x.getId().toUpperCase());
				this.service.save(x, ut);
				
				CacheTools.removeDiyInfBatch(x.getId());
				new ClassInnerNotice().invoke(DiyInfBatch.class.getSimpleName(), x.getId());
			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "???????????????????????????", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech+","+BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/getObj", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject getObj(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/getObj", DiyInfBatch.class.getName(), null,
					true, this.getService());
			DiyInfBatch obj = this.service.getMgClient().getById(id, DiyInfBatch.class);
			if (obj != null) {
				super.objToJson(obj, json);
			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	

	@Security(accessType = "1", displayName = "???????????????????????????", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech+","+BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/info", DiyInfBatch.class.getName(), null,
					true, this.getService());
			DiyInfBatch obj = this.service.getMgClient().getById(id, DiyInfBatch.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(DiyInfBatch.class,id);
				return json.jsonValue();
			}
			super.objToJson(obj, json);
			super.selectToJson(PUserMember.getUserListForRole(BosConstants.role_tech,ut.getMemberId(), service), json, DiyInfBatch.class.getName(), "userId");
			

			SelectBidding sb = new SelectBidding();
			DiyInf s = new DiyInf();
			s.setStatus(1L);
			QueryListInfo<DiyInf> sList = this.service.getList(s, "sys,className,code");
			for (DiyInf x : sList.getList()) {
				
				Table tx=BosConstants.getTable(x.getClassName());
				if(tx==null) continue;
				sb.put(x.getCode(),
						x.getTitle()
								+ "[" + tx.getDisplayName() + "  " + CacheTools
										.getSysCodeDisplay("86", DataChange.objToString(x.getInfType()), json.getLan())
								+ " ]  "+x.getJsonKey());
			}
			super.selectToJson(sb, json, DiyInfBatchItem.class.getSimpleName(), "code");
			

			DiyInfBatchItem item = new DiyInfBatchItem();
			item.setId(id);
			QueryListInfo<DiyInfBatchItem> list = this.service.getList(item, "seq,jsonKey,!formula,!terminateFormula");
			for (DiyInfBatchItem x : list.getList()) {
				if (DataChange.isEmpty(x.getCode()))
					continue;

				DiyInf inf = this.service.getById(x.getCode(), DiyInf.class);
				if (inf != null) {
					x.setClassName(inf.getClassName());
					if(DataChange.isEmpty(x.getJsonKey())) {
						x.setJsonKey(inf.getJsonKey());
					}
					if(DataChange.isEmpty(x.getInit())) {
						x.setInit(inf.getInit());
					}
					x.setTitle(inf.getTitle());
				}

			}
			super.listToJson(list, json, item.showTable());
			
			
			
			boolean allow=false;
			if(ut.getUserId().equals(obj.getUserId())) {
				allow=true;
			}
			
			if(ut.getUserId().equals("1")) {
				allow=true;
			}

			//???????????????????????? userId=1 or ???????????????leader ??????
			if(!allow) {
				Vector<String> prv=new Vector<String>();
				RPageInfBatch pi=new RPageInfBatch();
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

	@Security(accessType = "1", displayName = "???????????????????????????", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech+","+BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/delete", DiyInfBatch.class.getName(), null,
					true, this.getService());
			DiyInfBatch obj = this.service.getMgClient().getById(id, DiyInfBatch.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(DiyInfBatch.class,id);
				return json.jsonValue();
			}

			this.service.deleteById(id, DiyInfBatch.class.getName(), ut);

			DiyInfBatchItem item = new DiyInfBatchItem();
			item.setId(id);
			this.service.delete(item, ut);
			
			
			RPageInfBatch rpib=new RPageInfBatch();
			rpib.setInfCode(id);
			this.service.delete(rpib, ut,true);
			
			
			CacheTools.removeDiyInfBatch(id);
			
			
			new ClassInnerNotice().invoke(DiyInfBatch.class.getSimpleName(), id);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	
	
	@Security(accessType = "1", displayName = "???????????????????????????", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech+","+BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/deleteItem", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deleteItem(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/deleteItem", DiyInfBatchItem.class.getName(), null,
					true, this.getService());
			DiyInfBatchItem obj = this.service.getMgClient().getById(id, DiyInfBatchItem.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(DiyInfBatchItem.class,id);
				return json.jsonValue();
			}

			this.service.deleteById(id, DiyInfBatchItem.class.getName(), ut);

			CacheTools.removeDiyInfBatch(id);
			
			new ClassInnerNotice().invoke(DiyInfBatch.class.getSimpleName(), obj.getCode());

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	
	
	@Security(accessType = "1", displayName = "?????????????????????????????????", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech+","+BosConstants.role_product_manager)
	@RequestMapping(value = URI + "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			DiyInfBatch obj = json.getObj(DiyInfBatch.class, "id,cache", this.service);

			

			DiyInfBatch src = this.service.getMgClient().getById(obj.getId(), DiyInfBatch.class);

			String id = json.getSelectedId(Constants.current_sys, URI + "/info", DiyInfBatch.class.getName(), null,
					true, this.getService());
			List<DiyInfBatchItem> list = json.getList(DiyInfBatchItem.class, "code,jsonKey,!formula,!terminateFormula", service);

			Vector<String> jsonV = new Vector<String>();

			
			
			for (DiyInfBatchItem x : list) {
				x.setId(id);
				DiyInf inf = this.service.getById(x.getCode(), DiyInf.class);
				if (inf == null) {
					json.setUnSuccess(-1,
							LabelDisplay.get("???????????????:", json.getLan()) + obj.getId()
									+ LabelDisplay.get(" ????????????:", json.getLan()) + x.getCode()
									+ LabelDisplay.get("?????????", json.getLan()),
							true);
					return json.jsonValue();
				}

				if (jsonV.contains(x.getJsonKey())) {
					json.setUnSuccess(-1,
							LabelDisplay.get("???????????????:", json.getLan()) + obj.getId()
									+ LabelDisplay.get(" ????????????:", json.getLan()) + x.getCode()
									+ LabelDisplay.get("JsonKey??????", json.getLan()),
							true);
					return json.jsonValue();
				}
				
				jsonV.add(x.getJsonKey());

				// 0:?????? 1:?????? 2:???????????? 11:??????(??????) 12:??????(??????) 13:?????? 14:????????????  15:??????????????????????????????  16:?????????????????????  17:??????????????????????????????  18:??????????????????????????? 19:??????(???:?????????) 
				//21:???????????? 22:????????????  23:????????????(??????) 28:?????????????????????  29:???????????? 31:???????????? 32:????????????
				if (obj.getCache().intValue() == 1) {
					if (!inf.judgeSearchInf() ) {
						json.setUnSuccess(-1,
								LabelDisplay.get("???????????????:", json.getLan()) + obj.getId()
										+ LabelDisplay.get("??????redis??????,???????????????:", json.getLan()) + x.getCode()
										+ LabelDisplay.get("??????????????????", json.getLan()),
								true);
						return json.jsonValue();
					}
				}

			}
			
			
			StringBuffer sb=new StringBuffer();

			long i = 0;
			for (DiyInfBatchItem x : list) {
				x.setId(id);
				x.setSeq(i++);
				x.setSys(src.getSys());
				DiyInf inf = this.service.getById(x.getCode(), DiyInf.class);
				if (inf == null) {
					this.service.delete(x, ut, true);
					continue;
				}
				sb.append(x.getCode()+"   ");
				this.service.save(x, ut);
			}
			
			obj.setItems(sb.toString());
			this.service.save(obj, ut);
			
			CacheTools.removeDiyInfBatch(obj.getPk());
			
			
			new ClassInnerNotice().invoke(DiyInfBatch.class.getSimpleName(), obj.getPk());

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}

	@Override
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}

}
