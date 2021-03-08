package com.buykop.console.mq;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.buykop.framework.mq.RabbitConfig;

/**
 * 延迟删除数据的延迟队列
 * 
 * @author Administrator
 *
 */
@Configuration
public class DelayDeleteConfig {
	
	
	
	@Value("${spring.rabbitmq.delay-delete-minute}")
	private  int delayDeleteMinute;
	
	
	

	
	
	// 声明延时Exchange
	@Bean("delayExchange")
	public DirectExchange delayExchange() {
		return new DirectExchange(RabbitConfig.DELAY_EXCHANGE_NAME);
	}

	// 声明死信Exchange
	@Bean("deadLetterExchange")
	public DirectExchange deadLetterExchange() {
		return new DirectExchange(RabbitConfig.DEAD_LETTER_EXCHANGE);
	}

	// 声明延时队列 延时24小时
	// 并绑定到对应的死信交换机
	@Bean("delayQueue")
	public Queue delayQueue() {
		Map<String, Object> args = new HashMap<>(2);
		// x-dead-letter-exchange 这里声明当前队列绑定的死信交换机
		args.put("x-dead-letter-exchange", RabbitConfig.DEAD_LETTER_EXCHANGE);
		// x-dead-letter-routing-key 这里声明当前队列的死信路由key
		args.put("x-dead-letter-routing-key", RabbitConfig.DEAD_LETTER_QUEUE_ROUTING_KEY);
		// x-message-ttl 声明队列的TTL
		//if(this.delayDeleteMinute==0) {
			args.put("x-message-ttl", 24*3600*1000);
		//}else {
			//args.put("x-message-ttl", this.delayDeleteMinute*60*1000);
		//}
		
		return QueueBuilder.durable(RabbitConfig.DELAY_QUEUE_NAME).withArguments(args).build();
	}

	// 声明死信队列 用于接收延时处理的消息
	@Bean("deadLetterQueue")
	public Queue deadLetterQueueA() {
		return new Queue(RabbitConfig.DEAD_LETTER_QUEUE_NAME);
	}

	// 声明延时队列A绑定关系
	@Bean
	public Binding delayBindingA(@Qualifier("delayQueue") Queue queue,
			@Qualifier("delayExchange") DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(RabbitConfig.DELAY_QUEUE_ROUTING_KEY);
	}

	// 声明死信队列A绑定关系
	@Bean
	public Binding deadLetterBindingA(@Qualifier("deadLetterQueue") Queue queue,
			@Qualifier("deadLetterExchange") DirectExchange exchange) {
		return BindingBuilder.bind(queue).to(exchange).with(RabbitConfig.DEAD_LETTER_QUEUE_ROUTING_KEY);
	}

	// 生产者---->延迟交换机------>延迟队列-------->死信交换机------->死信队列------->消费者

}
