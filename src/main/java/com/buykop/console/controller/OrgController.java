package com.buykop.console.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.console.entity.POrg;
import com.buykop.console.entity.PUser;
import com.buykop.console.service.CatalogService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.BizFieldModifyTrack;
import com.buykop.framework.entity.FileUpload;
import com.buykop.framework.entity.PROrgRole;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.office.excel.PSheet;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PFormAction;
import com.buykop.framework.scan.PFormMember;
import com.buykop.framework.scan.PFormRowAction;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PMapTJConfig;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.Calculation;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;
import com.buykop.console.util.Constants;


@Module(display = "部门管理", sys = Constants.current_sys)
@RestController
@RequestMapping(OrgController.URI)
public class OrgController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(OrgController.class);

	protected static final String URI = "/org";

	private static final String TREE_ORG = "TREE_ORG";

	@Autowired
	private CatalogService service;

	protected PTreeForm loadTree(String parentIdValue, HttpEntity json, UserToken token, RdClient conn)
			throws Exception {

		PTreeForm obj = new PTreeForm();
		obj.setCode(TREE_ORG);
		obj.setName("部门树");
		obj.setSys(BosConstants.current_sys);
		obj.setClassName(POrg.class.getName());
		obj.setBizClassName(POrg.class.getName());
		obj.setRegType(0L);
		obj.setRootId("0");
		if (DataChange.isEmpty(obj.getRootName())) {
			obj.setRootName("根目录");
		}
		if (obj.getStatus() == null) {
			obj.setStatus(1L);
		}
		if (DataChange.isEmpty(parentIdValue)) {
			parentIdValue = "0";
		}

		POrg search = new POrg();
		search.setParentId(parentIdValue);
		search.setMemberId(token.getMemberId());
		QueryListInfo<POrg> list = this.service.getList(search, "seq,orgName",false);

		SelectBidding bidding = list.getSelectBidding(this.service);
		json.getData().put("TREE", bidding.showJSON());
		obj.setRootId(parentIdValue);
		obj.setRootName("根目录");
		super.objToJson(obj, json);

		return obj;
	}

	@Menu(js = "org", name = "部门管理", trunk = "基础信息,我的事务")
	@Security(accessType = "1,2", displayName = "目录列表", needLogin = true, isEntAdmin = false, isSysAdmin = false,roleId=BosConstants.role_depAdmin)
	@RequestMapping(value = "/list", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject list(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			// Table table = this.service.getMgClient().getTableById(className);
			// super.objToJson(table, json);

			String parentId = json.getSelectedId(Constants.current_sys, URI + "/list", POrg.class.getName(), null,this.getService());

			PTreeForm tree = this.loadTree(parentId, json, ut, this.service.getRdClient());

			if (DataChange.isEmpty(parentId)) {
				parentId = tree.getRootId();
			}

			POrg search = json.getSearch(POrg.class, null, ut, this.service);
			search.setParentId(parentId);
			search.setMemberId(ut.getMemberId());
			QueryListInfo<POrg> list = this.service.getList(search, "seq,orgName");
			super.listToJson(list, json, BosConstants.getTable(POrg.class));

			SelectBidding sb = new SelectBidding();
			sb.put("0", "--根目录--");

			POrg parentx = this.getService().getById(parentId, POrg.class);
			if (parentx != null) {
				POrg p = this.getService().getById(parentx.getParentId(), POrg.class);
				if (p != null) {
					sb.put(p.getOrgId(), p.getOrgName() + "[上级目录]");
				}
				sb.put(parentId, parentx.getOrgName(), true);
				super.objToJson(parentx, "parent", json);
			}

			if (true) {
				for (POrg x : list.getList()) {
					if (DataChange.isEmpty(x.getParentId())) {
						x.setParentId("0");
					}
					sb.put(x.getOrgId(), x.getOrgName());
				}
			}

			super.selectToJson(sb, json, POrg.class.getSimpleName(), "parentId");

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("目录列表", e);
		}

		return json.jsonValue();
	}


	@Security(accessType = "1*,2*", displayName = "删除", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/delete", POrg.class.getName(), null, true,this.getService());

			POrg org = new POrg();
			org.setParentId(id);
			org.setMemberId(ut.getMemberId());
			org.setIsValid(1);
			if (this.service.getCount(org) > 0) {
				json.setUnSuccess(-1, "因含有下级目录而不能删除");
				return json.jsonValue();
			}

			PUserMember u = new PUserMember();
			u.setOrgId(id);
			if (this.service.getCount(u) > 0) {
				json.setUnSuccess(-1, "因含有用户而不能删除");
				return json.jsonValue();
			}

			this.service.deleteById(id, POrg.class.getName(), ut, true);

			json.getData().put("TREEREMOVE", id);

			String parentId = json.getSelectedId(Constants.current_sys, URI + "/list", POrg.class.getName(), null,this.getService());
			PTreeForm tree = this.loadTree(parentId, json, ut, this.service.getRdClient());
			if (DataChange.isEmpty(parentId)) {
				parentId = tree.getRootId();
			}

			json.setSuccess("删除成功");
		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("删除目录", e);
		}

		return json.jsonValue();

	}

	@Security(accessType = "1*,2*", displayName = "列表保存", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String parentId = json.getSelectedId(Constants.current_sys, URI + "/list", POrg.class.getName(), null,this.getService());
			PTreeForm tree = this.loadTree(parentId, json, ut, this.service.getRdClient());
			if (DataChange.isEmpty(parentId)) {
				parentId = tree.getRootId();
			}

			long seq = 0;
			List<POrg> list = json.getList(POrg.class, "orgId,orgName,!parentId", this.service);
			for (POrg x : list) {

				if (DataChange.isEmpty(x.getParentId())) {
					x.setParentId("0");
				}

				if (x.getOrgId().equals(x.getParentId())) {
					json.setUnSuccess(-1, x.getOrgName() + "的上部门不能是自身");
					return json.jsonValue();
				}

				x.setMemberId(ut.getMemberId());

				x.setSeq(seq++);
				x.setIsValid(1);
				this.service.save(x, ut);
			}

			JSONArray arr = new JSONArray();
			for (BosEntity x : list) {
				if (!x.existMust("orgName"))
					continue;
				arr.add(x.getTreeJson(2));
			}
			json.getData().put("TREEMODIFY", arr);

			json.setSuccess("保存成功");
		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("列表保存", e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1*,2*", displayName = "列表保存", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/info", POrg.class.getName(), null, true,this.getService());

			String parentId = json.getSelectedId(Constants.current_sys, URI + "/list", POrg.class.getName(), null,this.getService());
			PTreeForm tree = this.loadTree(parentId, json, ut, this.service.getRdClient());
			if (DataChange.isEmpty(parentId)) {
				parentId = tree.getRootId();
			}

			POrg obj = this.service.getById(id, POrg.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(POrg.class);
				return json.jsonValue();
			}
			super.objToJson(obj, json);
			
			
			if(true) {
				PUserMember su=new PUserMember();
				su.setMemberId(ut.getMemberId());
				su.setOrgId(id);
				QueryListInfo<PUserMember> umList=this.service.getList(su, null);
				SelectBidding sb=new SelectBidding();
				for(PUserMember x:umList.getList()) {
					sb.put(x.getUserId(), CacheTools.getEntityDisplay(x.getUserId(), PUser.class.getName()));
				}
				super.selectToJson(sb, json, POrg.class.getSimpleName(), "chargeUser");
			}
			
			
			Vector<String> v=PROrgRole.getVectorForOrg(id,this.service);
			
			QueryListInfo<PRole> roleList=new QueryListInfo<PRole>();
			if(!DataChange.isEmpty(obj.getParentId()) && !parentId.equals("0")) {
				roleList=PRole.getListForOrg(obj.getParentId(),this.service);
			}else {
				//获取公司的角色
				roleList=PRole.getListForMember(ut.getMemberId(), null,this.service);
			}
			for(PRole x:roleList.getList()) {
				if(v.contains(x.getRoleId())) {
					x.setSelected();
				}
			}
			super.listToJson(roleList, json, BosConstants.getTable(PRole.class));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("列表保存", e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1*,2*", displayName = "列表保存", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			// String id = json.getSelectedId(Constants.current_sys, URI +
			// "/info",POrg.class.getName(), null,true);

			String parentId = json.getSelectedId(Constants.current_sys, URI + "/list", POrg.class.getName(), null,this.getService());
			PTreeForm tree = this.loadTree(parentId, json, ut, this.service.getRdClient());
			if (DataChange.isEmpty(parentId)) {
				parentId = tree.getRootId();
			}

			POrg obj = json.getObj(POrg.class, "orgName,!parentId", this.service);
			
			BosConstants.debug("pId="+obj.getParentId()+"   parentId="+parentId);
			
			if(DataChange.isEmpty(obj.getParentId())) {
				obj.setParentId(parentId);
			}
			
			BosConstants.debug("pId1="+obj.getParentId()+"   parentId="+parentId);
			
			if(DataChange.isEmpty(obj.getParentId())) {
				obj.setParentId("0");
			}
			
			BosConstants.debug("pId2="+obj.getParentId()+"   parentId="+parentId);
			
			if(obj.getParentId().equals(obj.getOrgId())) {
				json.setUnSuccess(-1, "上级部门与本级部门不能相同");
				return json.jsonValue();
			}
			obj.setMemberId(ut.getMemberId());
			this.service.save(obj, ut);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("列表保存", e);
		}

		return json.jsonValue();
	}

	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}

}