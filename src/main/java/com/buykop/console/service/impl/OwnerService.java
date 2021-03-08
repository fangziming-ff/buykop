package com.buykop.console.service.impl;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.util.type.BaseService;


@Service
@Component
public class OwnerService  extends BaseService implements com.buykop.console.service.OwnerService{

	
	@Autowired
	private AmqpTemplate rabbitTemplate;

	public AmqpTemplate getRabbitTemplate() {
		return rabbitTemplate;
	}
	

}
