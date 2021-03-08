package com.buykop.console.service.impl;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.buykop.framework.annotation.util.DataCheck;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseService;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;



@Service
@Component
public class UserService  extends BaseService implements com.buykop.console.service.UserService{

	
	@Autowired
	private AmqpTemplate rabbitTemplate;

	public AmqpTemplate getRabbitTemplate() {
		return rabbitTemplate;
	}
	
	
	
	
	
}
