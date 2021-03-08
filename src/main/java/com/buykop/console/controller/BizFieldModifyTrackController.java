package com.buykop.console.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.BizFieldModifyTrackService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.BizFieldModifyTrack;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;


@Module(display = "敏感属性轨迹", sys = Constants.current_sys)
@RestController
@RequestMapping(BizFieldModifyTrackController.URI)
public class BizFieldModifyTrackController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(BizFieldModifyTrackController.class);

	public static final String URI = "/modifyTrack";

	@Autowired
	private BizFieldModifyTrackService service;

	@Menu(name = "敏感轨迹", trunk = "开发服务,日志查询", js = "modifyTrack")
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_oisAdmin)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			BizFieldModifyTrack search = json.getSearch(BizFieldModifyTrack.class, null, ut, this.service);
			PageInfo page = json.getPageInfo(BizFieldModifyTrack.class);

			QueryFetchInfo<BizFieldModifyTrack> fetch = this.service.getFetch(search, "!changeDate",
					page.getCurrentPage(), page.getPageSize());

			super.fetchToJson(fetch, json, BosConstants.getTable(BizFieldModifyTrack.class));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_oisAdmin)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/info", BizFieldModifyTrack.class.getName(),
					"", true, this.getService());
			BizFieldModifyTrack obj = this.service.getById(id, BizFieldModifyTrack.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(BizFieldModifyTrack.class,id);
				return json.jsonValue();
			}

			super.objToJson(obj, json);

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
