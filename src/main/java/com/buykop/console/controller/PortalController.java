package com.buykop.console.controller;

import java.util.HashMap;
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
import com.buykop.console.entity.PPlaceInfo;
import com.buykop.console.service.PortalService;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.HotKeyWord;
import com.buykop.framework.entity.HotKeyWordForMember;
import com.buykop.framework.entity.HotKeyWordRankingList;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.PSysCodeType;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;

@Module(display = "门户", sys = Constants.current_sys)
@RestController
@RequestMapping(PortalController.URI)
public class PortalController extends BaseController {

	public static final String URI = "/portal";

	@Autowired
	private PortalService service;

	@Security(accessType = "0", displayName = "多语言列表", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/index", method = RequestMethod.GET)
	@ResponseBody
	public JSONObject index(HttpServletRequest request,@RequestHeader String token) throws Exception {

		HttpEntity json = new HttpEntity();
		json.setSys(Constants.current_sys);
		json.setUri(URI+"/index");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String key = "BUYKOP_PORTAL_INDEX";

			JSONObject jo = CacheTools.getJson(key);
			if (jo != null) {
				return jo;
			}

			// 找到10个最热的关键字
			HotKeyWord hw = new HotKeyWord();
			hw.setClassName("buykop.Product");
			QueryListInfo<HotKeyWord> hwList = this.service.getList(hw, "!searchNum", 10);
			json.getData().put("HotKeyWord", hwList.getJSONArray(hw.showTable().listDBFields(false), false));

			// 找到推荐的2个产品
			HotKeyWordRankingList rank = new HotKeyWordRankingList();
			rank.setClassName("buykop.Product");
			QueryListInfo<HotKeyWordRankingList> rankList = this.service.getList(rank, "seq", 10);
			JSONArray topList = new JSONArray();
			for (HotKeyWordRankingList x : rankList.getList()) {
				if (DataChange.isEmpty(x.getIdValue()))
					continue;
				BosEntity product = this.service.getById(x.getIdValue(), "buykop.Currency");
				if (product == null)
					continue;
				topList.add(product.getJson("id,productName,title"));
			}

			json.getData().put("Product", topList);

			// 币种查询
			BosEntity cs = new TableJson("buykop.Currency");
			QueryListInfo<BosEntity> csList = this.service.getList(cs, "seq");
			json.getData().put("Currency", csList.getJSONArray(cs.showTable().listDBFields(false), false));

			// 加载产品的搜索条件(销售区域salesPlaceId、分类typeId,币种currencyType)
			HashMap<String, PPlaceInfo> pHash = new HashMap<String, PPlaceInfo>();
			QueryListInfo<PPlaceInfo> p0List = new QueryListInfo<PPlaceInfo>();
			PPlaceInfo ps = new PPlaceInfo();
			ps.setLevelType(3L);
			ps.addPropertyOperation("levelType", 7);
			QueryListInfo<PPlaceInfo> pList = this.service.getList(ps, "parentId,seq");
			for (PPlaceInfo x : pList.getList()) {
				pHash.put(x.getPlaceId(), x);
				if (DataChange.isEmpty(x.getParentId())) {
					x.setParentId("0");
				}
				if (x.getParentId().equals("0")) {
					p0List.getList().add(x);
				}
			}

			for (PPlaceInfo x : pList.getList()) {
				if (x.getParentId().equals("0"))
					continue;
				PPlaceInfo p = pHash.get(x.getParentId());
				if (p == null)
					continue;
				p.getSubList().add(x);
			}
			json.getData().put("PlaceInfo", p0List.getJSONArray(ps.showTable().listDBFields(false), true));

			json.setSuccess();

			CacheTools.putJson(key, json.jsonValue(), 3600);

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "0", displayName = "多语言列表", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/clearCache", method = RequestMethod.GET)
	@ResponseBody
	public JSONObject clearCache(HttpServletRequest request,@RequestHeader String token) throws Exception {

		HttpEntity json = new HttpEntity();
		json.setSys(Constants.current_sys);
		json.setUri(URI+"/clearCache");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}

		try {

			String key = "BUYKOP_PORTAL_INDEX";

			CacheTools.removeJson(key);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "0", displayName = "列举关键字+推荐产品", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/listKeyWord", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject listKeyWord(@RequestBody HttpEntity json, HttpServletRequest request,@RequestHeader String token) throws Exception {

		
		
		json.setSys(Constants.current_sys);
		json.setUri(URI+"/listKeyWord");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		
		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String searchKeyWord = json.getSimpleData("searchKeyWord", "关键字查询", String.class, true, this.getService());

			if (true) {
				// 查找关键字
				HotKeyWord kw = new HotKeyWord();
				kw.setClassName("buykop.Product");
				kw.setSearchKeyWord(searchKeyWord);
				QueryListInfo<HotKeyWordRankingList> hotList = this.service.getList(kw, "!searchNum", 10);
				System.out.println("hotList size=" + hotList.size());
				System.out.println("hotList size1=" + this.service.getMgClient().getList(kw, "!searchNum", 10,this.service).size());
				System.out.println("hotList size2=" + this.service.getFetch(kw, "!searchNum", 1L, 10).size());
				json.getData().put("HotKeyWord", hotList.getJSONArray("id,hotKey,searchNum", false));
			}

			HotKeyWord kw = new HotKeyWord();
			kw.setClassName("buykop.Product");
			kw.setHotKey(searchKeyWord);
			kw = this.service.get(kw, "!searchNum");
			if (kw != null) {
				HotKeyWordRankingList rank = new HotKeyWordRankingList();
				rank.setClassName("buykop.Product");
				rank.setKwId(kw.getId());
				QueryListInfo<HotKeyWordRankingList> rankList = this.service.getList(rank, "seq", 10);
				JSONArray topList = new JSONArray();
				for (HotKeyWordRankingList x : rankList.getList()) {
					if (DataChange.isEmpty(x.getIdValue()))
						continue;
					BosEntity product = this.service.getById(x.getIdValue(), "buykop.Product");
					if (product == null)
						continue;
					topList.add(product.getJson("id,productName,title"));
				}
				json.getData().put("Product", topList);
			} else {
				json.getData().put("Product", new JSONArray());
			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "0", displayName = "全文检索", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/query", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject query(@RequestBody HttpEntity json, HttpServletRequest request,@RequestHeader String token) throws Exception {

		
		json.setSys(Constants.current_sys);
		json.setUri(URI+"/query");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		
		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String searchKeyWord = json.getSimpleData("searchKeyWord", "关键字查询", String.class, true, this.getService());

			PageInfo page = json.getPageInfo("buykop.Product");
			BosEntity search = new TableJson("buykop.Product");
			search.setSearchKeyWord(searchKeyWord);
			search.putMustValue("status", "1");
			QueryFetchInfo<BosEntity> fetch = this.service.getFetch(search,
					"!" + search.showTable().getHotSearchField(), page.getCurrentPage(), page.getPageSize());
			super.fetchToJson(fetch, json, search.showTable().getSimpleName(), search.showTable().listDBFields(false),
					search.showTable());

			// 加入推荐
			BosEntity search1 = new TableJson("buykop.Product");
			// search1.initByFormual(recommendInit, ut, json);
			search1.putMustValue("status", "1");
			QueryListInfo<BosEntity> recommendList = this.service.getEsClient().getList(search1,
					"!" + search.showTable().getHotSearchField(), 10,this.service);
			super.listToJson(recommendList, json, search1.showTable().getSimpleName() + "Recommend",
					search1.showTable().listDBFields(false), search1.showTable());

			// 加入热搜
			BosEntity search2 = new TableJson("buykop.Product");
			// search1.initByFormual(hotInit, ut, json);
			search2.putMustValue("status", "1");
			QueryListInfo<BosEntity> hotList = this.service.getEsClient().getList(search2,
					"!" + search.showTable().getHotSearchField(), 10,this.service);
			super.listToJson(hotList, json, search2.showTable().getSimpleName() + "Hot",
					search2.showTable().listDBFields(false), search2.showTable());

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();

	}

	@Security(accessType = "0", displayName = "全文检索(商户)", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/queryForShop", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject queryForShop(@RequestBody HttpEntity json, HttpServletRequest request,@RequestHeader String token) throws Exception {

		
		json.setSys(Constants.current_sys);
		json.setUri(URI+"/queryForShop");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		
		try {
			

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String searchKeyWord = json.getSimpleData("searchKeyWord", "关键字查询", String.class, false, this.getService());
			String businessId = json.getSimpleData("businessId", "商家", String.class, true, this.getService());

			
			
			
			if(!DataChange.isEmpty(searchKeyWord)) {
				
				
				Vector<String> v=MyString.splitBy(searchKeyWord, " ");
				for(String x:v) {
					HotKeyWordForMember hotKey = new HotKeyWordForMember();
					hotKey.setHotKey(x.trim());
					hotKey.setClassName("buykop.Product");
					hotKey.setMemberId(businessId);
					if(DataChange.isEmpty(hotKey.getHotKey())) continue;
					
					hotKey = service.get(hotKey,null);
					if (hotKey == null) {
						hotKey = new HotKeyWordForMember();
						hotKey.setId(HotKeyWord.next());
						hotKey.setClassName("buykop.Product");
						hotKey.setHotKey(x.trim());
						hotKey.setSys("buykop");
					}
					if (hotKey.getSearchNum() == null) {
						hotKey.setSearchNum(0L);
					}
					hotKey.setMemberId(businessId);
					hotKey.setSearchNum(hotKey.getSearchNum() + 1);
					service.save(hotKey, null);
				}
			}
			
			
			
			
			
			
			PageInfo page = json.getPageInfo("buykop.Product");
			BosEntity search = new TableJson("buykop.Product");
			search.putMustValue("businessId", businessId);
			search.putMustValue("status", "1");
			if (!DataChange.isEmpty(searchKeyWord)) {
				search.setSearchKeyWord(searchKeyWord);
			}
			QueryFetchInfo<BosEntity> fetch = this.service.getEsClient().getFetch(search,
					"!" + search.showTable().getHotSearchField(), page.getCurrentPage(), page.getPageSize(),this.service);
			super.fetchToJson(fetch, json, search.showTable().getSimpleName(), search.showTable().listDBFields(false),
					search.showTable());

			// 加入推荐
			BosEntity search1 = new TableJson("buykop.Product");
			search1.setSearchKeyWord(searchKeyWord);
			search1.putMustValue("businessRecommendation", "1");
			search1.putMustValue("businessId", businessId);
			search1.putMustValue("status", "1");
			QueryListInfo<BosEntity> recommendList = this.service.getEsClient().getList(search1,
					"!" + search.showTable().getHotSearchField(), 10,this.service);
			super.listToJson(recommendList, json, search1.showTable().getSimpleName() + "Recommend",
					search1.showTable().listDBFields(false), search1.showTable());

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();

	}

	@Security(accessType = "0", displayName = "商家门户首页", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/indexForShop",method = RequestMethod.GET)
	@ResponseBody
	public JSONObject indexForShop(String businessId,HttpServletRequest request,@RequestHeader String token) throws Exception{ 
		
		
		
		HttpEntity json=new HttpEntity();
		json.setSys(Constants.current_sys);
		json.setUri(URI+"/indexForShop");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		

		try {
			
			json.setSys(Constants.current_sys);
			json.setUri(URI+"/indexForShop");
			
			if(DataChange.isEmpty(businessId)) {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}
			
			
			if(DataChange.isEmpty(token)) {
				token=UserToken.next();
			}
			json.setTokenKey(token);
			
			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String key=businessId+RdClient.splitChar+"SHOP_INDEX";
			
			JSONObject jo=CacheTools.getJson(key);
			
			if(jo==null){
				
				
				HotKeyWordForMember  hw=new HotKeyWordForMember();
				hw.setClassName("buykop.Product");
				hw.setMemberId(businessId);
				QueryListInfo<HotKeyWordForMember> hwList=this.service.getMgClient().getList(hw, "!searchNum", 5,this.service);
				super.listToJsonForChart(hwList, json, HotKeyWordForMember.class.getSimpleName(), "hotKey,searchNum", hw.showTable(),true);
				
				
				
				
				Vector<String> v1=new Vector<String>();
				if(true) {
					BosEntity search = new TableJson("buykop.Product");
					search.putMustValue("businessRecommendation", "1");
					search.putMustValue("businessId", businessId);
					search.putMustValue("status", "1");
					QueryListInfo<BosEntity> list=this.service.getList(search, "!"+search.showTable().getHotSearchField(), 1);
					for(BosEntity x:list.getList()) {
						v1.add(x.getPk());
					}
					super.listToJsonForChart(list, json, search.showTable().getSimpleName()+"Recommendation", search.showTable().listDBFields(false), search.showTable(),true);
				}
				
				
				
				if(true) {
					QueryListInfo<BosEntity> list=new QueryListInfo<BosEntity>();
					BosEntity search = new TableJson("buykop.Product");
					search.putMustValue("businessId", businessId);
					search.putMustValue("status", "1");
					QueryListInfo<BosEntity> list1=this.service.getList(search, "!"+search.showTable().getClickNumField(), 2);
					for(BosEntity x:list1.getList()) {
						if(!v1.contains(x.getPk())) {
							list.getList().add(x);
							break;
						}
					}
					super.listToJsonForChart(list, json, search.showTable().getSimpleName()+"Hot", search.showTable().listDBFields(false), search.showTable(),true);
				}
				
				
				if(true) {
					BosEntity search = new TableJson("buykop.ProductType");
					search.putMustValue("businessId", businessId);
					search.putMustValue("status", "1");
					QueryListInfo<BosEntity> list=this.service.getList(search, "title", 6);
					super.listToJsonForChart(list, json, search.showTable().getSimpleName(), search.showTable().listDBFields(false), search.showTable(),true);
				}
				
				
				
				json.setSuccess();
				
				
				jo=json.jsonValue();
				
				if(BosConstants.runTimeMode()) {
					CacheTools.putJson(key, jo,300);
				}else {
					CacheTools.putJson(key, jo,60);
				}
				
			}
			
			
			
			return jo;
			
			
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

			
	}

	@Override
	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}

}
