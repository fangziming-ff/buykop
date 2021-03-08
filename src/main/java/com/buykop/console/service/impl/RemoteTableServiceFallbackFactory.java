package com.buykop.console.service.impl;

import org.springframework.stereotype.Component;

import feign.hystrix.FallbackFactory;

//@Component
public class RemoteTableServiceFallbackFactory implements FallbackFactory<com.buykop.console.service.TableService> {

	
	@Override
    public com.buykop.console.service.TableService create(Throwable throwable) {
		com.buykop.console.service.impl.TableService remoteFallback = new com.buykop.console.service.impl.TableService();
        remoteFallback.setCause(throwable);
        return remoteFallback;
    }
	
}
