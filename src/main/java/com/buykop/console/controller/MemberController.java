package com.buykop.console.controller;

import java.util.Date;
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

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.entity.LoginLog;
import com.buykop.console.entity.PMember;
import com.buykop.console.entity.PPlaceInfo;
import com.buykop.console.entity.PUser;
import com.buykop.console.service.MemberService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.FileBizCatalog;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PMemberType;
import com.buykop.framework.oauth2.PRMemberRoot;
import com.buykop.framework.oauth2.PRMemberType;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.DataPer;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.util.PrimaryKeyGenerator;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.DateUtil;
import com.buykop.framework.util.data.MD5Encrypt;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.Entity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;

@Module(display = "组织机构", sys = Constants.current_sys)
@RestController
@RequestMapping(MemberController.URI)
public class MemberController extends BaseController {

	protected static final String URI = "/member";

	private static Logger log = LoggerFactory.getLogger(MemberController.class);

	@Autowired
	private MemberService service;

	private static final String TREE_PLACE = "TREE_PLACE";

	protected PTreeForm loadTreeX(String parentIdValue, HttpEntity json, UserToken token, RdClient conn)
			throws Exception {

		PTreeForm obj = new PTreeForm();
		obj.setCode(TREE_PLACE);
		obj.setName("地区目录树");
		obj.setSys(BosConstants.current_sys);
		obj.setClassName(PPlaceInfo.class.getName());
		obj.setBizClassName(PMember.class.getName());
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

		PPlaceInfo search = new PPlaceInfo();
		search.setParentId(parentIdValue);
		QueryListInfo<PPlaceInfo> list = this.service.getMgClient().getList(search, "seq,placeName",this.service);

		SelectBidding bidding = list.getSelectBidding(this.service);
		json.getData().put("TREE", bidding.showJSON());
		obj.setRootId(parentIdValue);
		obj.setRootName("根目录");
		super.objToJson(obj, json);

		return obj;
	}

	// @Menu(js = "memberProfile", name = "机构信息", trunk = "基础信息,我的事务")
	@Security(accessType = "2*,1*", displayName = "机构信息", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject profile(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			PMember obj = this.service.getById(ut.getMemberId(), PMember.class);

			super.objToJson(obj, json);

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();

	}

	@Menu(name = "组织机构", trunk = "基础信息,机构及用户", js = "member")
	@Security(accessType = "1", displayName = "组织机构列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_sysAdmin)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String parentId = json.getSelectedId(Constants.current_sys, URI + "/fetch",
					FileBizCatalog.class.getName(), null,this.getService());
			//PTreeForm tree = this.loadTree(parentId, json, ut, this.service.getRdClient());
			//if (DataChange.isEmpty(parentId)) {
				//parentId = tree.getRootId();
			//}
			//if (DataChange.isEmpty(parentId)) {
				//parentId = "0";
			//}

			PMember search = json.getSearch(PMember.class, null, ut, this.service);
			//if (!parentId.equals("0")) {
				//search.setOwnerPlaceId(parentId);
			//}

			PageInfo page = json.getPageInfo(PMember.class);
			QueryFetchInfo<PMember> fetch = this.service.getFetch(search, "seq,name,status", page.getCurrentPage(),
					page.getPageSize());
			super.fetchToJson(fetch, json, BosConstants.getTable(PMember.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}
	
	
	
	
	
	
	

	@Security(accessType = "0", displayName = "组织机构列表", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/fetchForLib", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetchForLib(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			PMember search = json.getSearch(PMember.class, null, ut, this.service);
			if (search == null) {
				search = new PMember();
			}

			PageInfo page = json.getPageInfo(PMember.class);

			QueryFetchInfo<PMember> fetch = this.service.getFetch(search, null, page.getCurrentPage(),
					page.getPageSize());
			super.fetchToJson(fetch, json, BosConstants.getTable(PMember.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_sysAdmin)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			//String parentId = json.getSelectedId(Constants.current_sys, URI + "/fetch",FileBizCatalog.class.getName(), null,this.getService());
			//PTreeForm tree = this.loadTree(parentId, json, ut, this.service.getRdClient());
			//if (DataChange.isEmpty(parentId)) {
				//parentId = tree.getRootId();
			//}
			//if (DataChange.isEmpty(parentId)) {
				//parentId = "0";
			//}

			String id = json.getSelectedId(Constants.current_sys, URI + "/info", PMember.class.getName(), null, true,this.getService());

			PMember obj = this.service.getById(id, PMember.class);

			if (obj == null) {
				json.setUnSuccessForNoRecord(PMember.class, null);
				return json.jsonValue();
			}

			PUser user = this.service.getById(id, PUser.class);
			if (user != null) {
				obj.setLoginName(user.getLoginName());
				obj.setUserName(user.getUserName());
			}

			super.objToJson(obj, json);
			
			
			
			//会员类型列表
			PRMemberType mt=new PRMemberType();
			mt.setMemberId(id);
			mt.setStatus(2L);
			Vector<String> v=this.service.getMgClient().getVector(mt, "typeId",this.service);
			
			
			PMemberType st=new PMemberType();
			QueryListInfo<PMemberType> list=this.service.getMgClient().getList(st, "typeName",this.service);
			for(PMemberType x:list.getList()) {
				if(v.contains(x.getTypeId())) x.setSelected();
			}
			super.listToJson(list, json, BosConstants.getTable(PMemberType.class));
			
			json.setSuccess();


		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "显示新增", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_sysAdmin)
	@RequestMapping(value = "/showAdd", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject showAdd(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String parentId = json.getSelectedId(Constants.current_sys, URI + "/fetch",
					FileBizCatalog.class.getName(), null,this.getService());
			//PTreeForm tree = this.loadTree(parentId, json, ut, this.service.getRdClient());
			//if (DataChange.isEmpty(parentId)) {
				//parentId = tree.getRootId();
			//}
			//if (DataChange.isEmpty(parentId)) {
				//parentId = "0";
			//}

			PMember obj = new PMember();
			obj.setMemberId(PMember.next());
			obj.initMapPlaceInfo(ut, service);
			super.objToJson(obj, json);

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "机构保存用户", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_sysAdmin)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/member/info", PMember.class.getName(), null,this.getService());
			// obj.setMemberId(id);

			PMember obj = json.getObj(PMember.class, "memberCode,name,userName,loginName,!parentId", this.service);
			if (obj == null) {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}

			if (!DataChange.isEmpty(obj.getParentId()) && obj.getParentId().equals(obj.getMemberId())) {
				json.setUnSuccess(-1, "上级主管机构不能设置当前机构");
				return json.jsonValue();
			}

			PMember src = this.service.getById(id, PMember.class);
			if (src == null) {

				PMember judge = new PMember();
				judge.setMemberCode(obj.getMemberCode());
				judge = this.service.get(judge, null);
				if (judge != null) {
					json.setUnSuccess(-1, "组织机构代码重复");
					return json.jsonValue();
				}

				judge = new PMember();
				judge.setName(obj.getName());
				judge = this.service.get(judge, null);
				if (judge != null) {
					json.setUnSuccess(-1, "机构名称重复");
					return json.jsonValue();
				}

			} else {

				// 判断是否存在上下级关系
				String idj = obj.getParentId();
				while (!DataChange.isEmpty(idj)) {
					PMember judge = this.service.getById(idj, PMember.class);
					if (judge == null || DataChange.isEmpty(judge.getParentId())) {
						idj = null;
					} else {
						if (judge.getParentId().equals(obj.getMemberId())) {
							json.setUnSuccess(-1, "当前机构与上级主管机构已存在上下级关系,请检查");
							return json.jsonValue();
						} else {
							idj = judge.getParentId();

						}
					}
				}

				PMember judge = new PMember();
				judge.setMemberId(src.getMemberId());
				judge.addPropertyOperation("memberId", 8);
				judge.setMemberCode(obj.getMemberCode());
				judge = this.service.get(judge, null);
				if (judge != null) {
					json.setUnSuccess(-1, "组织机构代码重复");
					return json.jsonValue();
				}

				judge = new PMember();
				judge.setMemberId(src.getMemberId());
				judge.addPropertyOperation("memberId", 8);
				judge.setName(obj.getName());
				judge = this.service.get(judge, null);
				if (judge != null) {
					json.setUnSuccess(-1, "机构名称重复");
					return json.jsonValue();
				}

			}

			PUser srcU = this.service.getById(id, PUser.class);
			if (srcU == null) {

				PUser judge = new PUser();
				judge.setLoginName(obj.getLoginName());
				judge = this.service.get(judge, null);
				if (judge != null) {
					json.setUnSuccess(-1, "管理员账号重复");
					return json.jsonValue();
				}

			} else {
				PUser judge = new PUser();
				judge.setUserId(id);
				judge.addPropertyOperation("userId", 8);
				judge.setLoginName(obj.getLoginName());
				judge = this.service.get(judge, null);
				if (judge != null) {
					json.setUnSuccess(-1, "管理员账号重复");
					return json.jsonValue();
				}

			}

			if (DataChange.isEmpty(obj.getParentId())) {
				obj.setParentId("0");
			}

			PUser admin = obj.getUser();

			if (srcU != null && (DataChange.isEmpty(srcU.getUserKey()) || DataChange.isEmpty(srcU.getUserSecret()))) {
				admin.setUserKey(MD5Encrypt.MD5Encode(PrimaryKeyGenerator.next() + srcU.getUserId()));
				admin.setUserSecret(MD5Encrypt.MD5Encode(PrimaryKeyGenerator.next() + srcU.getUserId()));
			}

			this.service.save(obj, ut);
			this.service.save(admin, ut);

			DataPer dp = new DataPer();
			dp.setUserId(obj.getMemberId());
			dp.setMemberId(obj.getMemberId());
			dp.setType(2L);
			dp.setIdValue(obj.getMemberId());
			dp.setIsValid(1);
			this.service.save(dp, ut);// 加入数据权限

			
			
			
			PRMemberType mt=new PRMemberType();
			mt.setMemberId(id);
			this.service.getMgClient().delete(mt,ut,this.service);
	
			Vector<String> ids=json.showIds();
			for(String x:ids) {
				mt=new PRMemberType();
				mt.setMemberId(id);
				mt.setTypeId(x);
				mt.setStatus(2L);
				this.service.save(mt, ut);
			}
			
			

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "机构保存用户列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_sysAdmin)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			List<PMember> list = json.getList(PMember.class, null, this.service);
			for (PMember x : list) {
				if (x.getIsOis() != null && x.getIsOis().intValue() == 0) {
					// this.service._getBaseDao().execute(x, "cancelOis");
					// 取消运营
				}
				this.service.save(x, ut);

				DataPer dp = new DataPer();
				dp.setUserId(x.getPk());
				dp.setMemberId(x.getPk());
				dp.setType(2L);
				dp.setIdValue(x.getPk());
				this.service.save(dp, ut);

			}

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	
	@Security(accessType = "1", displayName = "禁用", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_sysAdmin)
	@RequestMapping(value = "/disable", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject disable(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/member/disable", PMember.class.getName(), null,
					true,this.getService());

			PMember obj = new PMember();
			obj.setMemberId(id);
			obj.setStatus(0L);
			this.service.save(obj, ut);

			json.setSuccess("禁用成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "启用", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_sysAdmin)
	@RequestMapping(value = "/enable", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject enable(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/member/enable", PMember.class.getName(), null,
					true,this.getService());

			PMember obj = new PMember();
			obj.setMemberId(id);
			obj.setStatus(1L);
			this.service.save(obj, ut);

			json.setSuccess("启用成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}


	@Security(accessType = "2", displayName = "机构自助注销", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/cancelMe", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject cancelMe(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			if (ut.getUserId().equals(ut.getMemberId())) {
				this.service.cancel(ut.getMemberId(), ut);
			} else {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}

			LoginLog ls = new LoginLog();
			ls.setMemberId(ut.getMemberId());
			ls.setQueryDateProperty(LoginLog.class.getName() + Entity.keySplit + "logTime");
			ls.setQueryDateMin(DateUtil.addDay(NetWorkTime.getCurrentDate(), -7));
			ls.setQueryDateMax(DateUtil.addDay(NetWorkTime.getCurrentDate(), 1));
			QueryListInfo<LoginLog> list = this.service.getList(ls, "!logTime");
			for (LoginLog x : list.getList()) {
				if (DataChange.isEmpty(x.getToken()))
					continue;
				if (x.getToken().startsWith("TK")) {
					this.service.getRdClient().deleteUserToken(x.getToken(), null);
				} else {
					this.service.getRdClient().getJedis().del(x.getToken());
				}
			}

			json.setSuccess("自助注销成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	
	@Security(accessType = "1", displayName = "强制注销", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_sysAdmin)
	@RequestMapping(value = "/cancel", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject cancel(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/cancel", PMember.class.getName(), null,this.getService());

			if (id.equals("1")) {
				json.setUnSuccess(-1, "运营方不能被注销");
				return json.jsonValue();
			}

			this.service.cancel(id, ut);

			LoginLog ls = new LoginLog();
			ls.setMemberId(ut.getMemberId());
			ls.setQueryDateProperty(LoginLog.class.getName() + Entity.keySplit + "logTime");
			ls.setQueryDateMin(DateUtil.addDay(NetWorkTime.getCurrentDate(), -7));
			ls.setQueryDateMax(DateUtil.addDay(NetWorkTime.getCurrentDate(), 1));
			QueryListInfo<LoginLog> list = this.service.getEsClient().getList(ls, "!logTime",this.service);
			for (LoginLog x : list.getList()) {
				if (DataChange.isEmpty(x.getToken()))
					continue;
				if (x.getToken().startsWith("TK")) {
					this.service.getRdClient().deleteUserToken(x.getToken(), null);
				} else {
					this.service.getRdClient().getJedis().del(x.getToken());
				}
			}

			json.setSuccess("强制注销成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	
	
	//-----------------------------------------------开放哪些系统---------------------------------------------
	
	

			
	
	
	

	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}
}
