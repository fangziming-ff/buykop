package com.buykop.console.service;

import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.util.type.ServiceInf;

public interface MapTJService extends ServiceInf{
	
	public void buildMapTJResult(UserToken token) throws Exception;
	
}
