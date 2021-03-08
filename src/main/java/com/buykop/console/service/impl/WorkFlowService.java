package com.buykop.console.service.impl;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.entity.wf.WorkFlow;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.scan.Field;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BaseService;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.TableJson;




@Service
@Component
public class WorkFlowService extends BaseService implements com.buykop.console.service.WorkFlowService{

	@Autowired
	private AmqpTemplate rabbitTemplate;

	public AmqpTemplate getRabbitTemplate() {
		return rabbitTemplate;
	}
	
	public void initForObj(BaseController controller, HttpEntity json,WorkFlow obj) throws Exception{
		
		
		SelectBidding execUser=new SelectBidding();
		PUserMember um=new PUserMember();
		um.setMemberId(json.getToken().getMemberId());
		um.setStatus(1L);
		QueryListInfo<PUserMember> uList=this.getList(um, null);
		for(PUserMember x:uList.getList()) {
			execUser.put(x.getUserId(), CacheTools.getEntityDisplay(x.getUserId(), BosConstants.userClassName));
		}
		controller.selectToJson(execUser, json, obj.showTable().getSimpleName(), "execUserId");
		
		
		SelectBidding execRole=new SelectBidding();
		QueryListInfo<PRole> rList=PRole.getListForMember(json.getToken().getMemberId(),null,this);
		for(PRole x:rList.getList()) {
			execRole.put(x.getPk(), x.getRoleName());
		}
		controller.selectToJson(execRole, json, obj.showTable().getSimpleName(), "execRoleId");
		
		
		BosEntity orgs=new TableJson(BosConstants.orgClassName);
		orgs.putValue("memberId", json.getToken().getMemberId());
		QueryListInfo<BosEntity> oList=this.getList(orgs, "seq");
		controller.selectToJson(oList.getSelectBidding(this), json, obj.showTable().getSimpleName(), "execOrgId");
		
		
		String className=obj.propertyValueString("className");
		
		SelectBidding formUser=new SelectBidding();
		if(!DataChange.isEmpty(className)) {
			Field field=new Field();
			field.setClassName(className);
			field.setCustomer(0L);
			field.setPropertyType(1L);
			field.setFkClasss(BosConstants.userClassName);
			QueryListInfo<Field> fList=this.getMgClient().getTableFieldList(field,"property");
			for(Field x:fList.getList()) {
				formUser.put(x.getProperty(), x.getDisplay());
			}
		}
		controller.selectToJson(formUser, json, obj.showTable().getSimpleName(), "formUserId");
		
		
		
		SelectBidding formOrg=new SelectBidding();
		if(!DataChange.isEmpty(className)) {
			Field field=new Field();
			field.setClassName(className);
			field.setCustomer(0L);
			field.setPropertyType(1L);
			field.setFkClasss(BosConstants.orgClassName);
			QueryListInfo<Field> fList=this.getMgClient().getTableFieldList(field,"property");
			for(Field x:fList.getList()) {
				formOrg.put(x.getProperty(), x.getDisplay());
			}
		}
		controller.selectToJson(formOrg, json, obj.showTable().getSimpleName(), "formOrgId");
		
		
		
		
		SelectBidding formMember=new SelectBidding();
		if(!DataChange.isEmpty(className)) {
			Field field=new Field();
			field.setClassName(className);
			field.setCustomer(0L);
			field.setPropertyType(1L);
			field.setFkClasss(BosConstants.memberClassName);
			QueryListInfo<Field> fList=this.getMgClient().getTableFieldList(field,"property");
			for(Field x:fList.getList()) {
				formMember.put(x.getProperty(), x.getDisplay());
			}
		}
		controller.selectToJson(formMember, json, obj.showTable().getSimpleName(), "formMemberId");
		
		
		
		SelectBidding oisUser=new SelectBidding();
		SelectBidding oisRole=new SelectBidding();
		SelectBidding oisOrg=new SelectBidding();
		if(!DataChange.isEmpty(json.getToken().getOisId())) {
			PUserMember oisusers=new PUserMember();
			oisusers.setMemberId(json.getToken().getOisId());
			oisusers.setStatus(1L);
			uList=this.getList(oisusers, null);
			for(PUserMember x:uList.getList()) {
				oisUser.put(x.getUserId(), CacheTools.getEntityDisplay(x.getUserId(), BosConstants.userClassName));
			}
			
			rList=PRole.getListForMember(json.getToken().getOisId(),null,this);
			for(PRole x:rList.getList()) {
				oisRole.put(x.getPk(), x.getRoleName());
			}
			
			BosEntity oisorgs=new TableJson(BosConstants.orgClassName);
			oisorgs.putValue("memberId", json.getToken().getOisId());
			oList=this.getList(oisorgs, "seq");
			oisOrg=oList.getSelectBidding(this);
		}
		
		controller.selectToJson(oisUser, json, obj.showTable().getSimpleName(), "oisUserId");
		controller.selectToJson(oisRole, json, obj.showTable().getSimpleName(), "oisRoleId");
		controller.selectToJson(oisOrg, json, obj.showTable().getSimpleName(), "oisOrgId");
		
		
		
		SelectBidding sysUser=new SelectBidding();
		
		PUserMember sysusers=new PUserMember();
		sysusers.setMemberId("1");
		sysusers.setStatus(1L);
		uList=this.getList(sysusers, null);
		for(PUserMember x:uList.getList()) {
			sysUser.put(x.getUserId(),CacheTools.getEntityDisplay(x.getUserId(), BosConstants.userClassName));
		}
		controller.selectToJson(sysUser, json, obj.showTable().getSimpleName(), "sysUserId");
		
		
		SelectBidding sysRole=new SelectBidding();
		rList=PRole.getListForMember("1",null,this);
		for(PRole x:rList.getList()) {
			sysRole.put(x.getPk(), x.getRoleName());
		}
		controller.selectToJson(sysRole, json, obj.showTable().getSimpleName(), "sysRoleId");
		
		
		BosEntity sysorgs=new TableJson(BosConstants.orgClassName);
		sysorgs.putValue("memberId", "1");
		oList=this.getList(sysorgs, "seq");
		controller.selectToJson(oList.getSelectBidding(this), json, obj.showTable().getSimpleName(), "sysOrgId");
		
		
	}
	
	
}
