package com.buykop.console.controller;

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

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.CodeValueService;
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
import com.buykop.framework.scan.LableLanDisplay;
import com.buykop.framework.scan.PLanguage;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysCode;
import com.buykop.framework.scan.PSysCodeType;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.PrimaryKeyGenerator;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;


@Module(display = "数据字典", sys = Constants.current_sys)
@RestController
@RequestMapping(CodeValueController.URI)
public class CodeValueController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(CodeValueController.class);

	@Autowired
	private CodeValueService service;
	
	
	protected static final String URI="/codeValue";
	
	
	@Security(accessType = "0", displayName = "数据字典", needLogin = false, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI + "/listByType", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject listByType(@RequestBody HttpEntity json, HttpServletRequest request, @RequestHeader String token)
			throws Exception {

		// 0:列表 1:翻页 11:单个(主键) 12:单个(条件) 13:新建 14:单个复制 15:判断单个对象   21:列表保存 22:单个保存 31: 删除多个 32:单个删除

		json.setSys(Constants.current_sys);
		json.setUri(URI + "/codeValue");

		if (DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			
			String typeId = json.getSimpleData("typeId", "数据字典类型(多个用,号分隔)", String.class, true, service);
			
			
			Vector<String> ids=Field.split(typeId);
			
			for(String x:ids) {
				json.getData().put(x, CacheTools.getSysCode(x, json.getLan()));
			}

			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}

	@Menu(js = "codeValue", name = "数据字典", trunk = "基础信息,配置管理")
	@Security(accessType = "1,2", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/typeFetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject typeFetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			PSysCodeType search = json.getSearch(PSysCodeType.class, null, ut, this.service);
			search.setMemberId(ut.getMemberId());
			PageInfo page = json.getPageInfo(PSysCodeType.class);
			QueryFetchInfo<PSysCodeType> fetch = this.service.getMgClient().getFetch(search, "type",
					page.getCurrentPage(), page.getPageSize(),this.service);

			super.selectToJson(PRoot.getJsonForSelect(this.service), json,
					PSysCodeType.class.getSimpleName(), "sys");

			super.fetchToJson(fetch, json, BosConstants.getTable(PSysCodeType.class));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("类型列表", e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1,2", displayName = "数据字典分类删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/typeDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject typeDelete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/codeValue/typeDelete",
					PSysCodeType.class.getName(), null, true,this.getService());

			PSysCodeType obj = this.service.getMgClient().getById(id, PSysCodeType.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PSysCodeType.class, id);
				return json.jsonValue();
			}

			if (!ut.getMemberId().equals(obj.getMemberId())) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}
			
			

			PSysCode p = new PSysCode();
			p.setType(id);
			this.service.getMgClient().delete(p,ut,this.service);
			
			//Field field = new Field();
			//field.setCodeType(obj.getType());
			//if (this.service.getMgClient().getCount(field) > 0) {
				//json.setUnSuccess(-1, "该数据字典被使用,不能删除");
				//return json.jsonValue();
			//}
			BosConstants.debug("-------------------------");
			this.service.getMgClient().deleteByPK(id, PSysCodeType.class,ut,this.service);

			new ClassInnerNotice().invoke(PSysCode.class.getSimpleName(), id);

			BosConstants.getExpireHash().removeSysCode(id);

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("类型删除", e);
		}
		return json.jsonValue();

	}

	@Security(accessType = "1,2", displayName = "列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/saveTypeList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveTypeList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			List<PSysCodeType> list = json.getList(PSysCodeType.class, "!type,name", this.service);

			for (PSysCodeType s : list) {
				if (s.getType() == null) {
					s.setType(PrimaryKeyGenerator.next());
				}
				s.setMemberId(ut.getMemberId());
				this.service.save(s, ut);
			}

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("类型列表保存", e);
		}

		return json.jsonValue();

	}

	@Security(accessType = "1,2", displayName = "数据字典列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/codeList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject codeList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String typeId = json.getSelectedId(Constants.current_sys, "/codeValue/codeList",
					PSysCodeType.class.getName(), null, true,this.getService());

			PSysCode search = new PSysCode();
			search.setType(typeId);
			QueryListInfo<PSysCode> list = this.service.getMgClient().getList(search, "codeOrder,code",this.service);//

			super.listToJson(list, json, BosConstants.getTable(PSysCode.class));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("字典列表", e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1,2", displayName = "数据字段列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/saveCodeList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveCodeList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String typeId = json.getSelectedId(Constants.current_sys, "/codeValue/codeList",
					PSysCodeType.class.getName(), null, true,this.getService());
			
			BosConstants.debug("---------typeId="+typeId+"----------------------");
			
			
			PSysCodeType type=new PSysCodeType();
			type.setType(typeId);
			Vector<String> v=MyString.splitBy(typeId, "_");
			if(v.size()==3) {
				Table table=Table.getSysTableBySimpleName(v.get(0), v.get(1), service);
				if(table!=null) {
					Field field=new Field();
					field.setClassName(table.getClassName());
					field.setProperty(v.get(2));
					field=this.service.getById(field.getPk(), Field.class);
					if(field!=null) {
						type.setName(field.getDisplay());
						field.setCodeType(typeId);
						this.service.save(field, ut);
					}
				}
			}
			
			this.service.save(type, ut);
			
			
			PSysCode sc=new PSysCode();
			sc.setType(typeId);
			this.service.getMgClient().delete(sc,ut,this.service);
			

			
			

			List<PSysCode> list = json.getList(PSysCode.class, "code,!icoId,!stdCode,name,!outKey", this.service);
			BosConstants.debug("---------typeId="+typeId+"----------size1="+list.size()+"------------");
			long seq = 0;
			for (PSysCode s : list) {
				s.setType(typeId);
				s.setCodeOrder(seq++);
				BosConstants.debug("typeId:" + s.getType());
				this.service.save(s, ut);
			}

			PSysCode search = new PSysCode();
			search.setType(typeId);
			QueryListInfo<PSysCode> clist = this.service.getMgClient().getList(search, "codeOrder,code",this.service);//
			BosConstants.debug("---------typeId="+typeId+"----------size2="+clist.size()+"------------");
			

			CacheTools.setSysCode(typeId, clist.getList());

			BosConstants.getExpireHash().removeSysCode(typeId);

			new ClassInnerNotice().invoke(PSysCode.class.getSimpleName(), typeId);

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("字典列表保存", e);
		}

		return json.jsonValue();
	}

	@Security(accessType = "1,2", displayName = "数据字典删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/codeDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject codeDelete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {


		try {



			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/codeValue/codeDelete", PSysCode.class.getName(),
					null, true,this.getService());

			PSysCode obj = this.service.getMgClient().getById(id, PSysCode.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PSysCode.class, id);
				return json.jsonValue();
			}

			PSysCodeType type = this.service.getMgClient().getById(String.valueOf(obj.getType()), PSysCodeType.class);
			if (type == null) {
				json.setUnSuccessForNoRecord(PSysCodeType.class, obj.getType());
				return json.jsonValue();
			}

			if (!ut.getMemberId().equals(type.getMemberId())) {
				json.setUnSuccessForIllegalAccess();
				return json.jsonValue();
			}

			this.service.getMgClient().deleteByPK(id, PSysCode.class,ut,this.service);

			BosConstants.getExpireHash().removeSysCode(obj.getType());

			new ClassInnerNotice().invoke(PSysCode.class.getSimpleName(), obj.getType());

			json.setSuccess("删除成功");

			return this.codeList(json, request);
		} catch (Exception e) {
			json.setUnSuccess(e);
			logger.error("字典删除", e);
		}

		return json.jsonValue();
	}

	// --------------------------------------------多语言设置----------------------------------------------------------------
	// ---------------------------------------以下是菜单多语言设置--------------------------------------------------------
	@Security(accessType = "1", displayName = "数据字典的多语种设置", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/listForLan", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject listForLan(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		// Constants.debug("***************1\n"+json.stringValue());


		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String typeId = json.getSelectedId(Constants.current_sys, "/codeValue/listForLan",
					PSysCodeType.class.getName(), null, true,this.getService());
			PSysCodeType obj = this.service.getMgClient().getById(typeId, PSysCodeType.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PSysCodeType.class,typeId);
				return json.jsonValue();
			}
			super.objToJson(obj, json);

			// 多语言支持
			HashMap<String, QueryListInfo<PSysCode>> fhash = new HashMap<String, QueryListInfo<PSysCode>>();

			PLanguage lan = new PLanguage();
			lan.setStatus(1L);
			QueryListInfo<PLanguage> llist = this.service.getMgClient().getList(lan, "seq",this.service);
			if (llist.size() <= 0) {
				json.setUnSuccess(-1, "系统未配置多语言");
				return json.jsonValue();
			}

			SelectBidding sb = new SelectBidding();
			for (PLanguage x : llist.getList()) {
				fhash.put(x.getLan(), new QueryListInfo<PSysCode>());
				sb.put(x.getLan(), x.getRemark());
			}

			if (true) {

				PSysCode search = new PSysCode();
				search.setType(obj.getType());
				QueryListInfo<PSysCode> list = this.service.getMgClient().getList(search, "codeOrder,code",this.service);
				for (PSysCode x : list.getList()) {

					HashMap<String, String> dHash = new HashMap<String, String>();
					String labelId = PSysCode.class.getSimpleName() + "_" + x.getPk() + "_name";
					LableLanDisplay s = new LableLanDisplay();
					s.setLabelId(labelId);
					QueryListInfo<LableLanDisplay> dlist = this.service.getMgClient().getList(s, "lan",this.service);
					for (LableLanDisplay x1 : dlist.getList()) {
						dHash.put(x1.getLan() + "_name", x1.getDisplay());
					}

					for (PLanguage xl : llist.getList()) {

						QueryListInfo<PSysCode> fList = fhash.get(xl.getLan());
						PSysCode sc = new PSysCode();
						sc.setType(obj.getType());
						sc.setCode(x.getCode());
						sc.setValue(x.getName());

						if (dHash.containsKey(xl.getLan() + "_name")) {
							sc.setName(dHash.get(xl.getLan() + "_name"));
						}

						fList.getList().add(sc);
					}
				}

			}

			for (PLanguage x : llist.getList()) {
				QueryListInfo<PSysCode> fList = fhash.get(x.getLan());
				if (fList == null)
					continue;
				super.listToJson(fList, json, PSysCode.class.getSimpleName() + x.getLan(), "type,code,value,name",
						BosConstants.getTable(PSysCode.class));
			}

			json.getData().put("lanList", sb.showJSON());

			if (true) {

				String labelId = PSysCodeType.class.getSimpleName() + "_" + obj.getPk() + "_name";

				QueryListInfo<LableLanDisplay> lanDisList = new QueryListInfo<LableLanDisplay>();

				HashMap<String, LableLanDisplay> hash = new HashMap<String, LableLanDisplay>();

				if (true) {
					LableLanDisplay s = new LableLanDisplay();
					s.setLabelId(labelId);
					QueryListInfo<LableLanDisplay> dlist = this.service.getMgClient().getList(s, "lan",this.service);
					for (LableLanDisplay x : dlist.getList()) {
						hash.put(x.getLan(), x);
					}
				}

				if (true) {
					for (PLanguage x : llist.getList()) {
						LableLanDisplay dis = hash.get(x.getLan());
						if (dis == null) {
							dis = new LableLanDisplay();
							dis.setLabelId(labelId);
							dis.setClassName(PSysCodeType.class.getName());
						}
						dis.setLan(x.getLan());
						dis.setRemark(x.getRemark());
						lanDisList.getList().add(dis);
					}
				}

				super.listToJson(lanDisList, json, LableLanDisplay.class.getSimpleName(),
						BosConstants.getTable(LableLanDisplay.class));

			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据字典的多语种设置保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = "/listForLanSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject listForLanSave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		// Constants.debug("***************1\n"+json.stringValue());


		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String typeId = json.getSelectedId(Constants.current_sys, "/codeValue/listForLan",
					PSysCodeType.class.getName(), null, true,this.getService());
			PSysCodeType obj = this.service.getMgClient().getById(typeId, PSysCodeType.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PSysCodeType.class,typeId);
				return json.jsonValue();
			}

			PLanguage lan = new PLanguage();
			lan.setStatus(1L);
			QueryListInfo<PLanguage> llist = this.service.getMgClient().getList(lan, "seq",this.service);
			if (llist.size() <= 0) {
				json.setUnSuccess(-1, "系统未配置多语言");
				return json.jsonValue();
			}

			if (true) {
				// 保存子系统显示名的多语言设置
				String labelId = PSysCodeType.class.getSimpleName() + "_" + obj.getPk() + "_name";
				List<BosEntity> labelList = json.getList(LableLanDisplay.class.getSimpleName(),
						LableLanDisplay.class.getName(), "lan,!display", null, 0, this.service);
				for (BosEntity x : labelList) {
					x.putValue("labelId", labelId);
					x.putValue("className", PSysCodeType.class.getName());

					String display = x.propertyValueString("display");
					if (DataChange.isEmpty(display)) {
						this.service.getMgClient().deleteByPK(x.getPk(), LableLanDisplay.class.getName(),ut,true,this.service);
					} else {
						this.service.save(x, ut);
					}

				}
				BosConstants.getExpireHash().removeJSONObject(LableLanDisplay.class.getSimpleName() + "_" + labelId);
				new ClassInnerNotice().invoke(LableLanDisplay.class.getSimpleName(), labelId);
			}

			if (true) {

				// 保存字段的多语言设置
				for (PLanguage x : llist.getList()) {

					List<BosEntity> fList = json.getList(PSysCode.class.getSimpleName() + x.getLan(),
							PSysCode.class.getName(), "code,!name", null, 0, this.service);
					for (BosEntity f : fList) {
						f.putMustValue("type", obj.getType());

						Vector<String> v = Field.split("name");// displayName,

						for (String x1 : v) {

							String value = f.propertyValueString(x1);

							String labelId = PSysCode.class.getSimpleName() + "_" + f.getPk() + "_" + x1;
							LableLanDisplay display = new LableLanDisplay();
							display.setLabelId(labelId);
							display.setLan(x.getLan());
							if (!DataChange.isEmpty(value)) {
								display.setClassName(PSysCode.class.getName());
								display.setDisplay(value);
								this.service.save(display, ut);
							} else {
								this.service.getMgClient().deleteByPK(display.getPk(), LableLanDisplay.class,ut,this.service);
							}

							BosConstants.getExpireHash()
									.removeJSONObject(LableLanDisplay.class.getSimpleName() + "_" + labelId);
							new ClassInnerNotice().invoke(LableLanDisplay.class.getSimpleName(), labelId);
						}

					}

				}

			}

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
