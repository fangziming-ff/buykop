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
import com.buykop.console.service.PSysParamService;
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
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.SFTPUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;

@Module(display = "系统参数", sys = Constants.current_sys)
@RestController
@RequestMapping("/sysparam")
public class SysParamController extends BaseController {

	private static Logger  logger=LoggerFactory.getLogger(SysParamController.class);

	@Autowired
	private PSysParamService service;

	@Menu(js = "sysparam", name = "系统参数", trunk = "基础信息,配置管理")
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = true)
	@RequestMapping(value = "/list", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject list(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			PSysParam search = json.getSearch(PSysParam.class, null, ut,this.service);
			search.setParamTpye(1L);
			PageInfo page = json.getPageInfo(PSysParam.class);

			search.setMemberId(ut.getMemberId());
			QueryFetchInfo<PSysParam> list = this.service.getMgClient().getFetch(search, "group,param",
					page.getCurrentPage(), page.getPageSize(),this.service);// .getList(search,);
			for (PSysParam x : list.getList()) {

				if (DataChange.isEmpty(x.getParam())) {
					x.setParam("");
				}

				if (x.getParam().equals(BosConstants.paramMapTJClassName)) {
					super.selectToJson2(Table.getJsonForMapSelect(this.service), json, x, "value");
				}
				if (x.getCodeType() != null) {
					super.selectToJson2(CacheTools.getSysCodeSelectBidding(x.getCodeType(), json.getLan()), json, x,
							"value");
				}


				

			}
			
			super.fetchToJson(list, json, BosConstants.getTable(PSysParam.class));

			super.selectToJson(PRoot.getJsonForDev("1",this.service), json, PSysParam.class, "sys");

			json.setSuccess();
		} catch (Exception e) {
			// logger.error(e);
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/sysparam/delete", PSysParam.class.getName(), null,true,this.getService());

			PSysParam obj = this.service.getMgClient().getById(id, PSysParam.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PSysParam.class,id);
				return json.jsonValue();
			}

			if (!obj.getMemberId().equals(ut.getMemberId())) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}

			this.service.getMgClient().deleteByPK(id, PSysParam.class,ut,this.service);

			new ClassInnerNotice().invoke(PSysParam.class.getSimpleName(), id);

			BosConstants.getExpireHash().remove(PSysParam.class, id);

			json.setSuccess("删除成功");
		} catch (Exception e) {
			// logger.error(e);
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "保存列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_tech)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			List<PSysParam> list = json.getList(PSysParam.class, "param,!value,!remark,regType",this.service);

			for (PSysParam p : list) {

				p.setMemberId(ut.getMemberId());
				p.setParamTpye(1L);
				PSysParam src = this.service.getMgClient().getById(p.getPk(), PSysParam.class);
				this.service.save(p,ut);
				if (DataChange.isEmpty(p.getValue())) {
					this.service.getRdClient().remove(PSysParam.class.getName() + "_" + p.getPk());
				} else {
					this.service.getRdClient().put(PSysParam.class.getName() + "_" + p.getPk(), p.getValue());
				}

				
				if (p.getParam().equals(BosConstants.paramIndexPage) && !DataChange.isEmpty(p.getValue())) {

					int count = MyString.count(p.getValue(), "/");

					if (count <= 0) {
						BosConstants.indexPage = "/" + p.getValue();
					} else if (count == 1) {
						if (p.getValue().startsWith("/")) {
							BosConstants.indexPage = p.getValue();
						} else {
							json.setUnSuccess(-1, "首页地址必须在根目录下");
							return json.jsonValue();
						}
					} else {
						json.setUnSuccess(-1, "首页地址必须在根目录下");
						return json.jsonValue();
					}
				}

				if (DataChange.replaceNull(p.getGroup()).equals("fileServer")) {
					new ClassInnerNotice().invoke(SFTPUtil.class.getSimpleName(), "");
				}

				if (src != null
						&& !DataChange.replaceNull(src.getValue()).equals(DataChange.replaceNull(p.getValue()))) {
					new ClassInnerNotice().invoke(PSysParam.class.getSimpleName(), p.getPk());
				}

				BosConstants.getExpireHash().put(PSysParam.class, p.getPk(), p.getValue(), -1);

			}

			json.setSuccess();
		} catch (Exception e) {
			// logger.error(e);
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}

}
