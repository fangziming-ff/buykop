package com.buykop.console.controller;

import java.util.Date;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.MemberTypeService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PMemberType;
import com.buykop.framework.oauth2.PRMemberRoot;
import com.buykop.framework.oauth2.PRMemberType;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;


@Module(display = "机构类型", sys = Constants.current_sys)
@RestController
@RequestMapping("/memberType")
public class MemberTypeController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(MemberTypeController.class);

	@Autowired
	private MemberTypeService service;

	@Menu(js = "memberType", name = "机构类型", trunk = "基础信息,机构及用户")
	@Security(accessType = "1", displayName = "会员类型列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_sysAdmin)
	@RequestMapping(value = "/list", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject list(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			PMemberType search = json.getSearch(PMemberType.class, null, ut, this.service);
			QueryListInfo<PMemberType> list = this.service.getMgClient().getList(search, "sys,typeName",this.service);

			super.listToJson(list, json, BosConstants.getTable(PMemberType.class.getName()));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "显示某个机构的类型列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech
			+ "," + BosConstants.role_sysAdmin)
	@RequestMapping(value = "/listForMember", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject listForMember(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/memberType/listForMember",
					BosConstants.memberClassName, null, this.getService());
			if (DataChange.isEmpty(id)) {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}

			PRMemberRoot rmr = new PRMemberRoot();
			rmr.setMemberId(id);
			Vector<String> sv = this.service.getMgClient().getVector(rmr, "sys",this.service);
			if (true) {
				PRoot search = new PRoot();
				search.setStatus(1L);
				QueryListInfo<PRoot> list = this.service.getMgClient().getList(search, "sort",this.service);
				for (PRoot x : list.getList()) {
					if (!sv.contains(x.getCode()))
						continue;
					x.setSelected();
				}
				super.listToJson(list, json, BosConstants.getTable(PRoot.class));
			}

			PRMemberType rmt = new PRMemberType();
			rmt.setMemberId(id);
			rmt.setStatus(2L);
			QueryListInfo<PRMemberType> rrfList = this.service.getMgClient().getList(rmt,this.service);
			Vector<String> fv = new Vector<String>();
			for (PRMemberType f : rrfList.getList()) {
				fv.add(f.getTypeId());
			}

			QueryListInfo<PMemberType> list = new QueryListInfo<PMemberType>();

			PMemberType search = json.getSearch(PMemberType.class, null, ut, this.service);
			QueryListInfo<PMemberType> list1 = this.service.getMgClient().getList(search, "sys,typeName",this.service);
			for (PMemberType su : list1.getList()) {

				if (!sv.contains(su.getSys())) {
					continue;
				}

				if (fv.contains(su.getPk())) {
					su.setSelected();
				}

				list.getList().add(su);
			}
			super.listToJson(list, json, BosConstants.getTable(PMemberType.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "保存机构与类型关系", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech
			+ "," + BosConstants.role_sysAdmin)
	@RequestMapping(value = "/listForMemberSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject listForMemberSave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/memberType/listForMember",
					BosConstants.memberClassName, null, this.getService());
			if (DataChange.isEmpty(id)) {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}

			PRMemberType rmt = new PRMemberType();
			rmt.setMemberId(id);
			this.service.getMgClient().delete(rmt,ut,this.service);

			PRMemberRoot rmr = new PRMemberRoot();
			rmr.setMemberId(id);
			this.service.getMgClient().delete(rmr,ut,this.service);

			// 保存信息 ,分隔
			Vector<String> ids = json.showIds();
			Vector<String> ids1 = json.showIds("ids1");

			for (String r : ids) {
				rmt = new PRMemberType();
				rmt.setMemberId(id);
				rmt.setTypeId(r);
				rmt.setStatus(2L);
				this.service.save(rmt, ut);
			}

			for (String r : ids1) {
				rmr = new PRMemberRoot();
				rmr.setMemberId(id);
				rmr.setSys(r);
				this.service.save(rmr, ut);
			}

			
			// -----------查询
			rmt = new PRMemberType();
			rmt.setMemberId(id);
			rmt.setStatus(2L);
			QueryListInfo<PRMemberType> rrfList = this.service.getMgClient().getList(rmt,this.service);
			Vector<String> fv = new Vector<String>();
			for (PRMemberType f : rrfList.getList()) {
				fv.add(f.getTypeId());
			}

			PMemberType search = json.getSearch(PMemberType.class, null, ut, this.service);
			QueryListInfo<PMemberType> fetch = this.service.getMgClient().getList(search, "typeName",this.service);
			for (PMemberType su : fetch.getList()) {
				if (fv.contains(su.getPk())) {
					su.setSelected("checked");
				}
			}
			super.listToJson(fetch, json, BosConstants.getTable(PMemberType.class));

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
