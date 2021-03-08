package com.buykop.console.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import com.alibaba.fastjson.JSONObject;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.entity.wf.WorkFlow;
import com.buykop.framework.entity.wf.WorkFlowCC;
import com.buykop.framework.entity.wf.WorkFlowCase;
import com.buykop.framework.entity.wf.WorkFlowCaseTrack;
import com.buykop.framework.entity.wf.WorkFlowNode;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.buykop.console.service.WorkFlowService;
import com.buykop.console.util.ConfigUtil;
import com.buykop.console.util.Constants;


@Module(display = "流程", sys = Constants.current_sys)
@RestController
public class WorkFlowController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(WorkFlowController.class);
	
	
	protected static final String URI="/workFlow";

	@Autowired
	private WorkFlowService service;

	
	@Menu(js = "approveFlow", name = "流程设置", trunk = "开发服务,模板管理")
	@Security(accessType = "1", displayName = "流程列表", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			WorkFlow search = json.getSearch(WorkFlow.class, "",ut,this.service);
			search.setMemberId(ut.getMemberId());
			PageInfo page = json.getPageInfo(WorkFlow.class);
			QueryFetchInfo<WorkFlow> fetch =this.service.getFetch(search,"name", page.getCurrentPage(),page.getPageSize());
			for(WorkFlow x:fetch.getList()) {
				super.selectToJson2(Table.getJsonForSelect(x.propertyValueString("sys"), 0L,this.service), json, x, "className");
			}
			
			super.selectToJson(PRoot.getJsonForDev(ut.getMemberId(),this.service), json,BosConstants.getTable(WorkFlow.class).getSimpleName(), "sys");
			

			super.fetchToJson(fetch, json, BosConstants.getTable(WorkFlow.class));



			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "流程显示新增", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/showAdd", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject showAdd(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			// 新建流程
			WorkFlow obj = new WorkFlow();
			obj.setFlowId(BosEntity.next());
			obj.setMemberId(ut.getMemberId());
			obj.setStartType(1L);
			this.objToJson(obj, json);

			super.selectToJson(PRoot.getJsonForSelect(this.service), json, obj.showTable().getSimpleName(), "sys");
			//super.selectToJson(PRole.getJsonForMember(ut.getMemberId(),null), json, WorkFlow.class.getSimpleName(), "roleId");
			
			
			// 新建流程节点
			WorkFlowNode afn = new WorkFlowNode();
			afn.setFlowId(obj.getPk());
			QueryListInfo<BosEntity> wnList = this.service.getList(afn, "seq");
			super.listToJson(wnList, json,BosConstants.getTable(WorkFlowNode.class));
			super.selectToJson(new SelectBidding(), json, WorkFlowNode.class.getSimpleName(), "typeId");

			// 新建审批抄送设置
			WorkFlowCC acc = new WorkFlowCC();
			acc.setFlowId(obj.getPk());
			QueryListInfo<BosEntity> uList1 = this.service.getList(acc, "");
			super.listToJson(uList1, json, BosConstants.getTable(WorkFlowCC.class));

			BosEntity su = new TableJson(BosConstants.userClassName);
			su.putValue("memberId", ut.getMemberId());
			super.selectToJson(this.service.getList(su, "userName").getSelectBidding(this.service), json,acc.showTable().getSimpleName(), "userId");

			
			if(true) {//execUserId  execRoleId   execOrgId  formUserId  formOrgId
				
				this.service.initForObj(this, json, obj);
				
			}
			
			
			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	
	
	
	@Security(accessType = "1", displayName = "流程显示新增", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/workFlow/info", WorkFlow.class.getName(), "", true,this.service);

			// 根据Id查找流程
			WorkFlow obj = this.service.getById(id, WorkFlow.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(WorkFlow.class,id);
				return json.jsonValue();
			}
			
			String className=obj.getClassName();
			String sys=obj.getSys();
			
			
			if(DataChange.isEmpty(className)) {
				json.setUnSuccess(-1, "流程未定义业务类对象");
				return json.jsonValue();
			}
			
			
			super.selectToJson(ConfigUtil.getIcoJson(Table.class.getName(), className,this.service), json, obj.showTable().getSimpleName(), "icoId");
			
			
			if(DataChange.isEmpty(sys)) {
				Table table=this.service.getMgClient().getTableById(className);
				obj.putValue("sys", table.getSys());
				sys=table.getSys();
			}
			
			
			PRoot root=this.service.getMgClient().getById(sys, PRoot.class);
			if(root==null) {
				json.setUnSuccessForNoRecord(PRoot.class,sys);
				return json.jsonValue();
			}
			
			
			SelectBidding sysJson=PRoot.getJsonForSelect(this.service);
			super.selectToJson(sysJson, json, obj.showTable().getSimpleName(), "sys");
			

			super.selectToJson(Table.getPkDBJsonForSelect(sys,this.service), json, obj.showTable().getSimpleName(),"className");
			super.selectToJson(PForm.getJsonForSelect(ut.getMemberId(),className,0L,this.service), json,obj.showTable().getSimpleName(), "formId");

			super.selectToJson(PRole.getJsonForSys(sys,true,this.service), json, obj.showTable().getSimpleName(), "roleId");
			
			
			if(!DataChange.isEmpty(className)) {
				super.selectToJson(ConfigUtil.getIcoJson(Table.class.getName(),className,this.service), json, obj.showTable().getSimpleName(), "icoId");
			}
			
			
			
			if(true) {//execUserId  execRoleId   execOrgId  formUserId
				this.service.initForObj(this, json, obj);
			}
			
			
			
			

			this.objToJson(obj, json);

			/// 根据ID查找流程节点
			WorkFlowNode afn = new WorkFlowNode();
			afn.setFlowId(obj.getFlowId());
			QueryListInfo<BosEntity> nList =this.service.getList(afn, "seq");
			super.listToJson(nList, json, BosConstants.getTable(WorkFlowNode.class));
			super.selectToJson(new SelectBidding(), json, WorkFlowNode.class.getSimpleName(), "typeId");
			super.selectToJson(nList.getSelectBidding(this.service), json, afn.showTable().getSimpleName(), "rejectBackNodeId");
			

			// 根据ID查找抄送人
			WorkFlowCC acc = new WorkFlowCC();
			acc.setFlowId(obj.getFlowId());
			QueryListInfo<BosEntity> cList = this.service.getList(acc, "");
			super.listToJson(cList, json, BosConstants.getTable(WorkFlowCC.class));

			BosEntity su = new TableJson(BosConstants.userClassName);
			su.putValue("memberId", ut.getMemberId());
			
			//super.selectToJson(PUserMember.getUserListForRole(Constants.ROLE_WORKFLOW_CC,ut.getMemberId(), service), json,acc.showTable().getSimpleName(), "userId");

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
		}

		return json.jsonValue();
	}

	
	
	
	
	@Security(accessType = "1", displayName = "保存流程", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		

		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			
			List<WorkFlow> list=json.getList(WorkFlow.class, "name,sys,className",this.service);
			for(WorkFlow x:list) {
				x.putValue("memberId", ut.getMemberId());
				this.service.save(x, ut);
			}
			

			json.setSuccess("保存成功");
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	
	
	@Security(accessType = "1", displayName = "保存流程", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/workFlow/info",WorkFlowCase.class.getName(), null,true,this.service);
			BosConstants.debug("流程ID：" + id);

			

			// 如果存在还在流转的实例,则不能修改
			WorkFlowCase acase = new WorkFlowCase(); //new WorkFlowCase();
			acase.setFlowId(id);
			acase.setStatus(1L);
			if (this.service.getCount(acase) > 0) {
				json.setUnSuccess(-1, "该流程存在流转中的实例,不能修改");
				return json.jsonValue();
			}

			// 保存审批流程  
			WorkFlow obj = json.getObj(WorkFlow.class, "name,sys,className,nextType,noPerson,isOpen,!remark,!roleId,!startScript,!rejectScript,!passScript",this.service);
			obj.setFlowId(id);
			obj.setStatus(1L);
			obj.setMemberId(ut.getMemberId());

			
			this.service.save(obj,ut);

			
			Vector<String>  v=new Vector<String>();
			//// 0-指定人员 1：指定部门 2：指定角色   11:所在部门   13:上级主管    21：所在部门(递归)   23：上级主管(递归)   30:单据内人员(指定了表单的属性)
			List<WorkFlowNode> nList = json.getList(WorkFlowNode.class, "nodeName,approveSetType,!typeId,!rejectBackNodeId,!remark",this.service);
			for (WorkFlowNode wfn : nList) {
				v.add(wfn.getPk());
			}
			
			
			Long isOpen=obj.getIsOpen();
			
			
			for (WorkFlowNode wfn : nList) {
				
				Long approveSetType=wfn.getApproveSetType();
						
				
				if(isOpen.intValue()==1 &&  (approveSetType.intValue()==0  ||  approveSetType.intValue()==1  ||  approveSetType.intValue()==2)    ) {
					json.setUnSuccess(-1, LabelDisplay.get("由于该工作流开放,流程节点不能设置为 ", json.getLan()) +CacheTools.getSysCodeDisplay("91", "0",json.getLan())+" 或  "+CacheTools.getSysCodeDisplay("91", "1",json.getLan()) +"  或 "+CacheTools.getSysCodeDisplay("91", "2",json.getLan()),true);
					return json.jsonValue();
				}
				wfn.setFlowId(obj.getPk());
				if(approveSetType.intValue()==0  ||  approveSetType.intValue()==1 ||  approveSetType.intValue()==2  ||  approveSetType.intValue()==30) {
					if(DataChange.isEmpty(wfn.propertyValueString("typeId"))) {
						json.setUnSuccess(-1, "请设置执行对象");
						return json.jsonValue();
					}
				}
				
				//
				
				if(!DataChange.isEmpty(wfn.getRejectBackNodeId())) {
					int cur=v.indexOf(wfn.getPk());
					int bak=v.indexOf(wfn.getRejectBackNodeId());
					if(bak>=cur) {
						json.setUnSuccess(-1,LabelDisplay.get("节点:", json.getLan()) +wfn.propertyValueString("nodeName")+LabelDisplay.get(" 拒绝后返回节点应早于当前节点", json.getLan()) ,true);
						return json.jsonValue();
					}
				}
				wfn.setClassName(obj.getClassName());
				wfn.setMemberId(obj.getMemberId());
				this.service.save(wfn,ut);
			}

			
			List<WorkFlowCC> cList = json.getList(WorkFlowCC.class, "userId",this.service);
			for (WorkFlowCC wcc : cList) {
				wcc.setFlowId(obj.getPk());
				this.service.save(wcc,ut);
			}


			json.setSuccess("保存成功");
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();
	}
	
	

	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 
		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI+"/delete",WorkFlow.class.getName(), null,true,this.service);

			WorkFlowCase acase = new WorkFlowCase(); //new WorkFlowCase();
			acase.setFlowId(id);
			acase.setStatus(1L);
			if (this.service.getCount(acase) > 0) {
				json.setUnSuccess(-1, "该流程存在流转中的实例,不能修改");
				return json.jsonValue();
			}

			this.service.deleteById(id, WorkFlow.class.getName(),ut,true);


			json.setSuccess("删除成功");
			
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}

	
	
	@Security(accessType = "1", displayName = "启用", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/enable", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject enable(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI+"/enable",WorkFlow.class.getName(), null,true,this.service);

			WorkFlow obj=this.service.getById(id,WorkFlow.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(WorkFlow.class,id);
				return json.jsonValue();
			}
			
			//WorkFlow.check(id);//调用远程服务
			obj.setStatus(1L);
			this.service.save(obj,ut);

			
			json.setSuccess("操作成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}
	
	
	
	
	@Security(accessType = "1", displayName = "禁用", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/disable", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject disable(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI+"/disable",WorkFlow.class.getName(), null,true,this.service);

			WorkFlow obj=this.service.getById(id,WorkFlow.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(WorkFlow.class,id);
				return json.jsonValue();
			}
			obj.setStatus(0L);
			this.service.save(obj,ut);
			
			
			json.setSuccess("操作成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}
	
	@Security(accessType = "1", displayName = "删除节点", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/nodeDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject nodeDelete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI+"/nodeDelete",WorkFlowNode.class.getName(), null,true,this.service);
			
			
			WorkFlowNode node=this.service.getById(id,WorkFlowNode.class);
			if(node==null) {
				json.setUnSuccessForNoRecord(WorkFlowNode.class,id);
				return json.jsonValue();
			}
			
			
			WorkFlow flow=this.service.getById(node.getFlowId(), WorkFlow.class);
			if(flow==null) {
				json.setUnSuccessForNoRecord(WorkFlow.class,node.getFlowId());
				return json.jsonValue();
			}
			
			
			
			WorkFlowCaseTrack track=new WorkFlowCaseTrack();
			track.setNodeId(id);
			if (this.service.getCount(track) > 0) {
				json.setUnSuccess(-1, "该流程已启动实例,不能删除");
				return json.jsonValue();
			}
			flow.setStatus(0L);
			this.service.save(flow,ut);
			
			
			this.service.deleteById(id,WorkFlowNode.class.getName(),ut,true);

			json.setSuccess("删除成功");
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}

	
	
	@Security(accessType = "1", displayName = "流程节点详情", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/nodeInfo", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject nodeInfo(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 


		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, URI+"/nodeInfo",WorkFlowNode.class.getName(), null,true,this.service);

			// 根据Id查找流程
			WorkFlowNode obj=this.service.getById(id,WorkFlowNode.class);
			if(obj==null) {
				json.setUnSuccessForNoRecord(WorkFlowNode.class,id);
				return json.jsonValue();
			}
			

			super.selectToJson(PRoot.getJsonForSelect(this.service), json,WorkFlowNode.class.getSimpleName(), "sys");

			String className=obj.getClassName();
			if (!DataChange.isEmpty(className)) {

				Table table = this.service.getMgClient().getTableById(className);
				if (table != null) {
					obj.setSys(table.getSys());
				}
				super.selectToJson(Table.getDBJsonForSelect(table.getSys(),this.service), json, obj.showTable().getSimpleName(),"className");
				super.selectToJson(PForm.getJsonForSelect(ut.getMemberId(),className,0L,this.service), json, obj.showTable().getSimpleName(), "formId");
			}

			this.objToJson(obj, json);

			
			json.setSuccess();
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}

	
	@Security(accessType = "1", displayName = "流程节点保存", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/nodeSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject nodeSave(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}

			//String id = json.getSelectedId(Constants.current_sys, "/workFlow/nodeInfo", BosConstants.workflowNodeClassName,"", true);
			String id = json.getSelectedId(Constants.current_sys, URI+"/nodeInfo",WorkFlowNode.class.getName(), null,true,this.service);

			// 根据Id查找流程
			WorkFlowNode obj = json.getObj(WorkFlowNode.class, "sys,className,!passScript,!rejectScript",this.service);
			if (obj == null) {
				json.setUnSuccessForNoRecord(WorkFlowNode.class,null);
				return json.jsonValue();
			}


			this.service.save(obj,ut);
			

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}
	
	
	
	@Security(accessType = "1", displayName = "删除抄送", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/ccDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject ccDelete(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id = json.getSelectedId(Constants.current_sys, URI+"/ccDelete",WorkFlowCC.class.getName(), null,true,this.service);

			
			WorkFlowCC cc=this.service.getById(id,WorkFlowCC.class);
			if(cc==null) {
				json.setUnSuccessForNoRecord(WorkFlowCC.class,id);
				return json.jsonValue();
			}
			
			
			WorkFlow flow=this.service.getById(cc.getFlowId(),WorkFlow.class);
			if(flow==null) {
				json.setUnSuccessForNoRecord(WorkFlow.class,cc.getFlowId());
				return json.jsonValue();
			}
			flow.setStatus(0L);
			this.service.save(flow,ut);

			this.service.deleteById(id,WorkFlowCC.class.getName(),ut,true);

			json.setSuccess("删除成功");
			
		} catch (Exception e) {
			json.setUnSuccess(e);
		}
		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "节点列表", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = URI+"/nodeList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject nodeList(@RequestBody HttpEntity json,HttpServletRequest request) throws Exception{ 

		
		try {

			UserToken ut = super.securityCheck(json,request);
			if (ut == null) {
				return json.jsonValue();
			}
			
			
			String id = json.getSelectedId(Constants.current_sys, URI+"/nodeList",WorkFlow.class.getName(), null,true,this.service);

			
			
			WorkFlow flow=this.service.getById(id, WorkFlow.class);
			if(flow==null) {
				json.setUnSuccessForNoRecord(WorkFlow.class,id);
				return json.jsonValue();
			}
			super.objToJson(flow, json);
			
			

			// 根据ID查找流程节点
			WorkFlowNode wfn = new WorkFlowNode();
			wfn.setFlowId(id);
			QueryListInfo<WorkFlowNode> uList = this.service.getList(wfn, "seq");
			super.listToJson(uList, json,BosConstants.getTable(WorkFlowNode.class));


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
