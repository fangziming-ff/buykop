package com.buykop.console.controller;

import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.DataFormService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;

@Module(display = "树形结构", sys = Constants.current_sys)
@RestController
@RequestMapping(TreeFormController.URI)
public class TreeFormController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(TreeFormController.class);

	protected static final String URI = "/tree";

	@Autowired
	private DataFormService service;

	@Menu(js = "tree", name = "通用绑定模板", trunk = "开发服务,模板管理")
	@Security(accessType = "1", displayName = "业务关联树形列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			PTreeForm search = json.getSearch(PTreeForm.class, null, ut, this.service);
			PageInfo page = json.getPageInfo(PTreeForm.class);
			QueryFetchInfo<BosEntity> fetch = this.service.getMgClient().getFetch(search, "sys,className",
					page.getCurrentPage(), page.getPageSize(),this.service);
			fetch.initBiddingForSysClassName(this, json, "sys", "className", null);
			super.fetchToJson(fetch, json, BosConstants.getTable(PTreeForm.class.getName()));
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, PTreeForm.class.getName(), "sys");

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "通用绑定模板列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			List<PTreeForm> list = json.getList(PTreeForm.class, "code,name,sys,!className", this.service);
			for (PTreeForm x : list) {
				x.setCode(x.getCode().toUpperCase());
				if (!x.getCode().startsWith("TREE") && !x.getCode().startsWith("LIST")) {
					json.setUnSuccess(-1, "模板编号必须用TREE或者LIST");
					return json.jsonValue();
				}
				this.service.save(x, null);
				// this.service.getRdClient().removeKeys(PTreeForm.class.getSimpleName()+"_"+x.getPk()+"_*");
			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "通用绑定模板详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/info", PTreeForm.class.getName(), null, true,
					this.getService());
			PTreeForm obj = this.service.getMgClient().getById(id, PTreeForm.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PTreeForm.class,id);
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

			super.selectToJson(PRoot.getJsonForSelect(this.service), json, PTreeForm.class.getName(), "sys");
			if (!DataChange.isEmpty(obj.getSys())) {
				super.selectToJson(Table.getJsonForSelect(obj.getSys(), 0L, this.service), json, PTreeForm.class,
						"className");
			}

			super.selectToJson(Field.getJsonForSelect(obj.getClassName(), 1L, json.getLan(), this.service), json,
					PTreeForm.class, "keyField");

			super.selectToJson(Field.getJsonForSelect(obj.getClassName(), 1L, json.getLan(), this.service), json,
					PTreeForm.class, "parentField");

			super.objToJson(obj, json);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "通用绑定模板删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/delete", PTreeForm.class.getName(), null,
					true, this.getService());
			PTreeForm obj = this.service.getMgClient().getById(id, PTreeForm.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PTreeForm.class,id);
				return json.jsonValue();
			}

			PTreeForm.delete(id, this.service);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "通用绑定模板保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			PTreeForm obj = json.getObj(PTreeForm.class, "sys,className", this.service);

			this.service.save(obj, null);

			// this.service.getRdClient().removeKeys(PTreeForm.class.getSimpleName()+"_"+obj.getPk()+"_*");

			json.setSuccess();
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
