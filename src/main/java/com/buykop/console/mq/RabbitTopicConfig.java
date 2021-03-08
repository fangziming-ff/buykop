package com.buykop.console.mq;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;

@Configuration
public class RabbitTopicConfig {

	@Bean
	public Queue bizRemark() {
		return new Queue("bizRemark", true);
	}
	
	
	@Bean
	public Queue behavior() {
		return new Queue("behavior", true);
	}

	@Bean
	public Queue dbLog() {
		return new Queue("dbLog", true);
	}

	@Bean
	public Queue loginLog() {
		return new Queue("loginLog", true);
	}

	@Bean
	public Queue invokeLog() {
		return new Queue("invokeLog", true);
	}

	@Bean
	public Queue bizView() {
		return new Queue("bizView", true);
	}

	@Bean
	public Queue sysLog() {
		return new Queue("sysLog", true);
	}
	
	
	
	@Bean
	public Queue userTokenRefresh() {
		return new Queue("userTokenRefresh", true);
	}

}
