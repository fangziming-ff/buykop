package com.buykop.console.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.buykop.console.util.Constants;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.util.type.ServiceInf;



//@FeignClient(contextId = "dataTableService", value = Constants.service_name, fallbackFactory = RemoteTableServiceFallbackFactory.class)
public interface TableService extends ServiceInf{

	
	/**
	 * 同步生效,  持久化并注入redis
	 * @param className
	 * @throws Exception
	 */
	//@PostMapping(value="/dataTableService/syn")
	public void syn(@RequestParam String className,@RequestParam String memberId,@RequestParam String userId) throws Exception;
	
	
	/**
	 * 删除某个表
	 * @param className
	 * @throws Exception
	 */
	public void delete(@RequestParam String className) throws Exception;
}
