package com.buykop.console.service;

import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.util.type.ServiceInf;

public interface TokenService extends ServiceInf{
	
	
	public UserToken login(String loginName,String password,int isMobile,String ip,String lan) throws Exception;

	public UserToken adminLogin(String loginName,String password,int isMobile,String ip,String lan) throws Exception;
	
	
	public UserToken memberLogin(String memberId,String loginName,String password,int isMobile,String ip,String lan) throws Exception;
	
}
