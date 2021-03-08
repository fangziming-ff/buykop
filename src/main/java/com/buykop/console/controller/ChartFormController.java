package com.buykop.console.controller;

import java.util.Date;
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
import com.buykop.console.service.ChartFormService;
import com.buykop.console.service.RootService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.cache.location.ExpiringMap;
import com.buykop.framework.chart.Data;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.ChartForm;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysCode;
import com.buykop.framework.scan.Statement;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.ListData;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;


@Module(display = "图形统计", sys = Constants.current_sys)
@RestController
@RequestMapping(ChartFormController.URI)
public class ChartFormController extends BaseController {

	protected static final String URI = "/chartform";
	
	private static Logger  logger=LoggerFactory.getLogger(ChartFormController.class);
	
	
	@Autowired
	private ChartFormService  service;
	

	@Menu(js = "chartForm", name = "图形统计", trunk = "开发服务,模板管理")
	@Security(accessType = "1", displayName = "流程列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			ChartForm search = json.getSearch(ChartForm.class, "", ut,this.service);
			search.setMemberId(ut.getMemberId());
			PageInfo page = json.getPageInfo(ChartForm.class);
			QueryFetchInfo<ChartForm> fetch = this.service.getMgClient().getFetch(search, "sys,sort,className",
					page.getCurrentPage(), page.getPageSize(),this.service);
			BosConstants.debug(fetch);

			fetch.initBiddingForSysClassName(this, json, "sys", "className", null);

			super.fetchToJson(fetch, json, BosConstants.getTable(ChartForm.class));

			super.selectToJson(PRoot.getJsonForDev(ut.getMemberId(),this.service), json, ChartForm.class.getSimpleName(), "sys");

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "主表单更换类名", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/formClassChange", method = RequestMethod.POST )
	@ResponseBody
	public JSONObject formClassChange(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			ChartForm obj = json.getObj(ChartForm.class, "formId,className",this.service);

			Table table = BosConstants.getTable(obj.getClassName());

			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class, obj.getClassName());
				return json.jsonValue();
			}

			ChartForm src = this.service.getMgClient().getById(obj.getFormId(), ChartForm.class);
			if (src == null) {
				json.setUnSuccessForNoRecord(ChartForm.class, obj.getFormId());
				return json.jsonValue();
			}

			src.setClassName(obj.getClassName());

			this.service.save(src,ut);

			json.setSelectedId(Constants.current_sys, URI + "/info", obj.getFormId());

			return this.info(json, request);

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

	}

	@Security(accessType = "1", displayName = "图形统计设置详情", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/info", ChartForm.class.getName(), "", true,this.getService());

			ChartForm obj = this.service.getMgClient().getById(id, ChartForm.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(ChartForm.class, id);
				return json.jsonValue();
			}

			if (DataChange.isEmpty(obj.getSys())) {
				json.setUnSuccess(-1, "请设置所属系统");
				return json.jsonValue();
			}

			if (DataChange.isEmpty(obj.getClassName())) {
				json.setUnSuccess(-1, "请设置绑定业务类");
				return json.jsonValue();
			}

			json.putUserInfo(ut);

			super.selectToJson(PRole.getJsonForSys(obj.getSys(), true, this.service), json, ChartForm.class.getSimpleName(),
					"viewRole");

			super.selectToJson(PRoot.getJsonForSelect(this.service), json, ChartForm.class.getSimpleName(), "sys");

			super.selectToJson(Table.getPkDBChartJsonForSelect(obj.getSys(),this.service), json, ChartForm.class.getSimpleName(),
					"className");
			
			
			if(!DataChange.isEmpty(obj.getClassName())) {
				super.selectToJson(Statement.getJsonForSelect(obj.getClassName(),this.service), json, ChartForm.class.getSimpleName(),"mapId");
			}

			super.selectToJson(PSysCode.getForSelect("126",this.service), json, ChartForm.class.getSimpleName(), "dataPerType");

			
			
			if (!DataChange.isEmpty(obj.getClassName())) {

				super.selectToJson(Field.getDimensionaFieldJsonForSelect(obj.getClassName(), null,this.service), json,ChartForm.class.getSimpleName(), "dimensionaField");

				// ------------------
				super.selectToJson(Field.getDimensionaFieldJsonForSelect(obj.getClassName(),null,this.service), json, ChartForm.class.getSimpleName(),"tjField");
				
				
				
				//
				if(DataChange.getLongValueWithDefault(obj.getFormType(), 0)==0) {
					super.selectToJson(Field.getDisFieldJsonForSelect(obj.getClassName(), Date.class.getName(),this.service), json,ChartForm.class.getSimpleName(), "xField");
				}else {
					super.selectToJson(Field.getDisFieldJsonForSelect(obj.getClassName(), null,this.service), json,ChartForm.class.getSimpleName(), "xField");
				}
				
				
				
				obj.setJsonKey(BosConstants.getTable(obj.getClassName()).getSimpleName());
				
			}
			
			
			super.objToJson(obj, json);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "清理缓存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/clear", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject clear(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/clear", ChartForm.class.getName(), "", true,this.getService());

			ChartForm obj = this.service.getMgClient().getById(id, ChartForm.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(ChartForm.class, id);
				return json.jsonValue();
			}

			BosConstants.getExpireHash().removeDataListMap(ChartForm.cacheKey(id));

			new ClassInnerNotice().invoke(ListData.class.getSimpleName(), ChartForm.cacheKey(id));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "图形统计设置列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			List<ChartForm> list = json.getList(ChartForm.class,
					"formId,formName,sys,formType,className,status",this.service);

			for (ChartForm x : list) {

				ChartForm src = this.service.getMgClient().getById(x.getFormId(), ChartForm.class);
				if (src != null) {
					if (!src.getSys().equals(x.getSys())) {
						x.setClassName(null);
					}
				}else {
					x.setFormId(x.getFormId().toUpperCase());
				}
				x.setMemberId(ut.getMemberId());
				this.service.save(x, ut);
				BosConstants.debug(x);
			}

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "图形统计设置保存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			ChartForm obj = json.getObj(ChartForm.class, "formName,formType,sys,className,tjField,!tjType",this.service);

			// formType,tjField,tjType

			if (obj.getFormType().intValue() == 100) {// 自定义图形类型
				obj.setTjType(null);

				if (DataChange.isEmpty(obj.getTjField())) {
					json.setUnSuccess(-1, "请选择统计字段");
					return json.jsonValue();
				}

			} else {// 日报 月报 周报
				if (obj.getTjType() == null) {
					json.setUnSuccess(-1, "请选择统计类型");// 记录 合计 平均等
					return json.jsonValue();
				} else if (obj.getTjType().intValue() > 0) {

					if (DataChange.isEmpty(obj.getTjField())) {
						json.setUnSuccess(-1, "请选择统计字段");
						return json.jsonValue();
					}
				}

			}

			if (obj.getFormType().intValue() == 100 || (obj.getTjType() != null && obj.getTjType().intValue() > 0)) {
				if (DataChange.isEmpty(obj.getTjField())) {
					json.setUnSuccess(-1, "请选择统计字段");
					return json.jsonValue();
				}
			} else {

			}

			ChartForm src = this.service.getMgClient().getById(obj.getFormId(), ChartForm.class);
			if (src == null) {
				json.setUnSuccessForNoRecord(ChartForm.class, obj.getFormId());
				return json.jsonValue();
			}

			Table table = BosConstants.getTable(obj.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class, obj.getClassName());
				return json.jsonValue();
			}

			Vector<String> dpV = MyString.splitBy(obj.getDataPerType(), ",");
			if (dpV.contains("5")) {
				if (DataChange.getLongValueWithDefault(table.getIsMap(), 0) == 0) {
					json.setUnSuccess(-1,
							table.getDisplayName() + LabelDisplay.get("不支持地图操作,无法按照所属地区查询", json.getLan()), true);
					return json.jsonValue();
				}

			}

			obj.setMemberId(ut.getMemberId());
			this.service.save(obj,ut);

			BosConstants.getExpireHash().removeDataListMap(ChartForm.cacheKey(obj.getFormId()));

			new ClassInnerNotice().invoke(ListData.class.getSimpleName(), ChartForm.cacheKey(obj.getFormId()));

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "图形统计设置删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/delete", method = RequestMethod.POST )
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/delete", ChartForm.class.getName(), null,true,this.getService());

			this.service.getMgClient().deleteByPK(id, ChartForm.class,ut,false,this.service);

			BosConstants.getExpireHash().removeDataListMap(ChartForm.cacheKey(id));

			new ClassInnerNotice().invoke(ListData.class.getSimpleName(), ChartForm.cacheKey(id));

			json.setSuccess("删除成功");
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "清理图形菜单缓存", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/clearMenu", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject clearMenu(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			//Vector<String> v = this.service.getRdClient().getKeys("*" + RdClient.splitChar + "BOSDYNCHART");
			//for (String x : v) {
				//this.service.getRdClient().remove(x);
			//}

			BosConstants.getExpireHash().removeMatch(RdClient.splitChar + "BOSDYNCHART");
			new ClassInnerNotice().invoke(ExpiringMap.class.getSimpleName(), RdClient.splitChar + "BOSDYNCHART");

			json.setSuccess("清理成功");

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
