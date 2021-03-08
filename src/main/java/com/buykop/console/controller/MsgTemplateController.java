package com.buykop.console.controller;

import java.util.Date;
import java.util.HashMap;
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
import com.buykop.console.service.MsgService;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.SynTableDataConfig;
import com.buykop.framework.scan.MsgTemplate;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.MQProcessing;
import com.buykop.framework.scan.MsgTemplate;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysCode;
import com.buykop.framework.scan.Statement;
import com.buykop.framework.scan.Table;
import com.buykop.framework.scan.ChartForm;
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

@Module(display = "通用接口配置", sys = Constants.current_sys)
@RestController
public class MsgTemplateController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(MsgTemplateController.class);

	protected static final String URI = "/msg/template";

	@Autowired
	private MsgService service;

	@Menu(js = "msgTemplate", name = "消息模板", trunk = "开发服务,模板管理")
	@Security(accessType = "1", displayName = "消息模板列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = URI + "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			MsgTemplate search = json.getSearch(MsgTemplate.class, null, ut, this.service);
			PageInfo page = json.getPageInfo(MsgTemplate.class);
			QueryFetchInfo<MsgTemplate> fetch = this.service.getMgClient().getFetch(search, "className,templateCode,templateName",
					page.getCurrentPage(), page.getPageSize(),this.service);
			fetch.initBiddingForSysClassName(this, json, "sys", "className", null);
			super.fetchToJson(fetch, json, BosConstants.getTable(MsgTemplate.class.getName()));
			super.selectToJson(PRoot.getJsonForSelect(this.service), json, MsgTemplate.class.getName(),
					"sys");


			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "消息模板列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = URI + "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			List<MsgTemplate> list = json.getList(MsgTemplate.class, "templateCode,templateName,sys,className,status,pushScope", this.service);
			for (MsgTemplate x : list) {
				x.setTemplateCode(x.getTemplateCode().toUpperCase());
				this.service.save(x, ut);
				
				BosConstants.getExpireHash().remove(MsgTemplate.class, x.getTemplateId());
				new ClassInnerNotice().invoke(MsgTemplate.class.getSimpleName(), x.getTemplateId());
				
			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	
	
	

	@Security(accessType = "1", displayName = "消息模板详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = URI + "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/info", MsgTemplate.class.getName(), null, true,
					this.getService());
			MsgTemplate obj = this.service.getMgClient().getById(id, MsgTemplate.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(MsgTemplate.class);
				return json.jsonValue();
			}



			Table table = BosConstants.getTable(obj.getClassName());
			if (table == null) {
				json.setUnSuccessForNoRecord(Table.class);
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
			
			
			
			SelectBidding sb=new SelectBidding();
			
			//加入属性的选择项目 pushScope;// 0:机构 1:部门 2:用户 3:指定属性
			if(obj.getPushScope()!=null) {
				if(obj.getPushScope().intValue()==0) {
					sb=Field.getJsonForSelectWithFK(obj.getClassName(), BosConstants.memberClassName, service);
				}else if(obj.getPushScope().intValue()==1) {
					sb=Field.getJsonForSelectWithFK(obj.getClassName(), BosConstants.orgClassName, service);
				}else if(obj.getPushScope().intValue()==2) {
					sb=Field.getJsonForSelectWithFK(obj.getClassName(), BosConstants.userClassName, service);
				}else if(obj.getPushScope().intValue()==3) {
					for (Field t : table.getFields()) {
						if(!t.judgeDB()) continue;
						if(t.getIsKey()!=null && t.getIsKey().intValue()==1) continue;
						if(!DataChange.isEmpty(t.getFkClasss())) continue;
						if(t.getCustomer()==null) continue; //0:开发人员定义 1:平台字段
						if(t.getCustomer().intValue()==1) continue;
						sb.put(t.getProperty(), t.getProperty() + "[" + t.getDisplay() + "]");
					}
				}
			}
			
			super.selectToJson(sb, json, MsgTemplate.class, "property");

			super.objToJson(obj, json);
			

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "消息模板删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = URI + "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI + "/delete", MsgTemplate.class.getName(), null, true,
					this.getService());
			MsgTemplate obj = this.service.getMgClient().getById(id, MsgTemplate.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(MsgTemplate.class);
				return json.jsonValue();
			}

			this.service.deleteById(id, MsgTemplate.class.getName(), ut);
			
			BosConstants.getExpireHash().remove(MsgTemplate.class, id);
			new ClassInnerNotice().invoke(MsgTemplate.class.getSimpleName(), id);

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "消息模板单个保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech)
	@RequestMapping(value = URI + "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			MsgTemplate obj = json.getObj(MsgTemplate.class, "templateCode,templateName,sys,className", this.service);
			this.service.save(obj, ut);
			
			
			BosConstants.getExpireHash().remove(MsgTemplate.class, obj.getTemplateId());
			new ClassInnerNotice().invoke(MsgTemplate.class.getSimpleName(), obj.getTemplateId());

			
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
