package com.buykop.console.controller;

import java.util.ArrayList;
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
import com.buykop.console.entity.PMember;
import com.buykop.console.entity.POrg;
import com.buykop.console.entity.PPlaceInfo;
import com.buykop.console.entity.PUser;
import com.buykop.console.service.DataFormService;
import com.buykop.console.service.DataPerService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.cache.location.ExpiringMap;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.DataPer;
import com.buykop.framework.scan.ExportTemplate;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PClob;
import com.buykop.framework.scan.PDiyUri;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PFormAction;
import com.buykop.framework.scan.PFormField;
import com.buykop.framework.scan.PFormMember;
import com.buykop.framework.scan.PFormRowAction;
import com.buykop.framework.scan.PFormSlave;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysCode;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.Statement;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.query.BaseQuery;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;
import com.buykop.console.util.Constants;


@Module(display = "数据权限", sys = Constants.current_sys)
@RestController
@RequestMapping(DataPerController.URI)
public class DataPerController extends BaseController{
	
	
	protected final static String URI="/dp";
	
	private static Logger  logger=LoggerFactory.getLogger(DataPerController.class);
	
	
	@Autowired
	private DataPerService service;
	
	

	@Security(accessType = "1*,2*", displayName = "数据权限显示", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/dataPerShow", method = RequestMethod.POST,consumes = "application/json")
	@ResponseBody
	public JSONObject dataPerShow(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String userId=json.getSelectedId(Constants.current_sys, "/dp/dataPerShow", PUser.class.getName(), "", true,this.getService());
			
			
			if(userId.equals(ut.getMemberId()) ){
				DataPer dp=new DataPer();
				dp.setUserId(userId);
				dp.setMemberId(ut.getMemberId());
				dp.setType(2L);  
				dp.setIdValue(ut.getMemberId());
				dp.setIsValid(1);
				this.service.save(dp, ut);
			}
			
			
			PUserMember uo=new PUserMember();
			uo.setMemberId(ut.getMemberId());
			uo.setUserId(userId);
			uo=this.service.get(uo,null);
			if(uo!=null) {
				POrg org=this.service.getById(uo.getOrgId(), POrg.class);
				if(org!=null && userId.equals(org.getChargeUser())) {
					DataPer dp=new DataPer();
					dp.setUserId(userId);
					dp.setMemberId(ut.getMemberId());
					dp.setType(1L);
					dp.setIdValue(org.getOrgId());
					dp.setIsValid(1);
					this.service.save(dp, ut);
				}
			}
			
			
			Vector<String> v=new Vector<String>();
			
			PUser obj=this.service.getById(userId,PUser.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(PUser.class,userId);
				return json.jsonValue();
			}
			super.objToJson(obj, "PUser0", json);
			
			
			
			DataPer dp=new DataPer();
			dp.setUserId(userId);
			dp.setMemberId(ut.getMemberId());
			QueryListInfo<DataPer> dpList=this.service.getList(dp,null);
			for(DataPer x:dpList.getList()) {
				v.add(x.getType()+"_"+x.getIdValue());
			}
			
			
			
			if(true) {
				
				PageInfo page=json.getPageInfo(PMember.class);
				//0:用户     1:部门      2:机构     3:运营方     5:地区
				PMember sm=json.getSearch(PMember.class, null,ut,this.service);
				QueryFetchInfo<PMember> smList=new QueryFetchInfo<PMember>();
				if(ut.getMemberId().equals("1") ) {
					smList=this.service.getFetch(sm, "name", page.getCurrentPage(), page.getPageSize());
				}else {
					sm.setMemberId(ut.getMemberId());
					BaseQuery bq=new BaseQuery();
					bq.setFields("memberId,parentId,oisId");
					bq.setValue(ut.getMemberId());
					sm.getQueryList().add(bq);//查询ois=? or memberId=? or parentId=?
					smList=this.service.getMgClient().getFetch(sm,"name", page.getCurrentPage(), page.getPageSize(),this.service);
				}

				for(PMember x:smList.getList()) {
					if(v.contains("2_"+x.getMemberId())) {
						x.setSelected();
					}
				}
				super.fetchToJson(smList, json, BosConstants.getTable(PMember.class));
			}
			
			
			if(true) {
				QueryListInfo<POrg> orgList=new QueryListInfo<POrg>();
				for(DataPer x:dpList.getList()) {
					if(x.getType()!=null && x.getType().intValue()==1) {
						POrg so=this.service.getById(x.getIdValue(), POrg.class);
						if(so!=null && so.getMemberId().equals(ut.getMemberId())) {
							so.setSelected();
							orgList.getList().add(so);
						}
					}
				}
				super.listToJson(orgList, json, BosConstants.getTable(POrg.class));
			}
			
			
			
			if(true) {
				PageInfo page=json.getPageInfo(PUserMember.class);
				PUserMember search=new PUserMember();
				search.setMemberId(ut.getMemberId());
				QueryFetchInfo<PUserMember> fetch=this.service.getFetch(search, null,page.getCurrentPage(), page.getPageSize());
				for(PUserMember x:fetch.getList()) {
					if(v.contains("0_"+x.getUserId())) {
						x.setSelected();
					}
				}
				super.fetchToJson(fetch, json, BosConstants.getTable(PUserMember.class));
			}
			
			
			if(true) {
				QueryListInfo<PPlaceInfo> pList=new QueryListInfo<PPlaceInfo>();
				for(DataPer x:dpList.getList()) {
					if(x.getType()!=null && x.getType().intValue()==5) {
						PPlaceInfo so=this.service.getById(x.getIdValue(), PPlaceInfo.class);
						if(so!=null) {
							so.setSelected();
							pList.getList().add(so);
						}
					}
				}
				super.listToJson(pList, json, BosConstants.getTable(PPlaceInfo.class));
			}
			
			
			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}
	
	
	
	
	@Security(accessType = "1*,2*", displayName = "数据权限保存", needLogin = true, isEntAdmin = true, isSysAdmin = false)
	@RequestMapping(value = "/dataPerSave", method = RequestMethod.POST,consumes = "application/json")
	@ResponseBody
	public JSONObject dataPerSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			String userId=json.getSelectedId(Constants.current_sys, "/dp/dataPerShow", PUser.class.getName(), "", true,this.getService());
			
			
			
			
			// 0:用户 1:部门 2:机构 3:运营方 5:地区
			if(true) {
				
				List<PMember> mList=json.getList(PMember.class, "memberId",this.service);
				//List<POrg> oList=json.getList(POrg.class, "orgId");
				
				DataPer dp=new DataPer();
				dp.setUserId(userId);
				dp.setType(2L);
				dp.setMemberId(ut.getMemberId());
				for(PMember x:mList) {
					dp.setIdValue(x.getMemberId());
					this.service.delete(dp,ut,true);
				}
				
				
				String ids2=json.getSimpleData("ids2", "组织机构选择", String.class, false,this.getService());
				
				if(!DataChange.isEmpty(ids2)) {
					String[] arr=ids2.split(",");
					for(String x:arr) {
						dp=new DataPer();
						dp.setUserId(userId);
						dp.setMemberId(ut.getMemberId());
						dp.setType(2L);
						dp.setIdValue(x);
						dp.setIsValid(1);
						this.service.save(dp, ut);
					}
				}
			}
			
			
			
			
			String ids0=json.getSimpleData("ids0", "用户选择", String.class, false,this.getService());
			// 0:用户 1:部门 2:机构 3:运营方 5:地区
			DataPer dp=new DataPer();
			dp.setUserId(userId);
			dp.setMemberId(ut.getMemberId());
			dp.setType(0L);
			List<PUser> uList=json.getList(PUser.class, "userId",this.service);
			for(PUser x:uList) {
				dp.setIdValue(x.getUserId());
				this.service.delete(dp,ut,true);
			}
			
			if(!DataChange.isEmpty(ids0)) {
				String[] arr=ids0.split(",");
				for(String x:arr) {
					PUserMember um=this.service.getMgClient().getById(x, PUserMember.class);
					if(um!=null) {
						dp=new DataPer();
						dp.setUserId(userId);
						dp.setMemberId(ut.getMemberId());
						dp.setType(0L);
						dp.setIdValue(um.getUserId());
						dp.setIsValid(1);
						this.service.save(dp, ut);
					}
				}
			}
			
			json.setSuccess("保存成功");

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
