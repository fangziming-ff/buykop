package com.buykop.console.service;

import com.buykop.framework.entity.wf.WorkFlow;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.ServiceInf;

public interface WorkFlowService extends ServiceInf{
	
	public void initForObj(BaseController controller, HttpEntity json,WorkFlow obj) throws Exception;
	
}
