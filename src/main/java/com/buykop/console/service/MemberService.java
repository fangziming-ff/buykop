package com.buykop.console.service;

import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.util.type.ServiceInf;

public interface MemberService extends ServiceInf{
	
	public void cancel(String memberId,UserToken token) throws Exception;
}
