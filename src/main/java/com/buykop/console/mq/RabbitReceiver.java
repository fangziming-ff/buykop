package com.buykop.console.mq;

import java.util.HashMap;
import java.util.Iterator;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.buykop.console.controller.BizFieldModifyTrackController;
import com.buykop.console.controller.DBLogController;
import com.buykop.console.controller.InvokeLogController;
import com.buykop.console.controller.LoginLogController;
import com.buykop.console.controller.SysLogController;
import com.buykop.console.entity.DBLog;
import com.buykop.console.entity.InvokeLog;
import com.buykop.console.entity.LoginLog;
import com.buykop.console.entity.SysLog;
import com.buykop.console.entity.UserBehaviorAnalysis;
import com.buykop.console.service.CatalogService;
import com.buykop.framework.annotation.MQ;
import com.buykop.framework.annotation.MQConfig;
import com.buykop.framework.entity.BizRemarkTrack;
import com.buykop.framework.entity.BizViewTrack;
import com.buykop.framework.entity.DelayDelete;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.mq.RabbitConfig;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.Table;
import com.buykop.framework.thread.SynToEs2;
import com.buykop.framework.thread.UserTokenRefresh;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;

@Component
@MQ
public class RabbitReceiver {
	
	@Autowired
	private BosService service;
	
	
	@RabbitListener(queues = "userTokenRefresh")
	public void userTokenRefresh(Message message, Channel channel) throws Exception {
    	
		
		if(!BosConstants.inited) return;
		
    	String input=new String(message.getBody(),BosConstants.LOCAL_CODE);
    	if(DataChange.isEmpty(input)) {
    		System.out.println("userTokenRefresh input is null");
    		return;
    	}
    	
    	try {
	    	BosConstants.debug("userTokenRefresh input="+input);
	    	JSONObject json=JSONObject.parseObject(input);
	    	UserToken token=new UserToken();
	    	token.initJson(json);
	    			
	    	BosConstants.debug("UserToken:" + token.getTokenKey());
	    	if(!DataChange.isEmpty(token.getTokenKey())) {
	    		UserTokenRefresh.tokenHash.put(token.getTokenKey(), token.getUserId());
	    	}

	        channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
	        
    	}catch(Exception e) {
    		e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                //logger.error("消息已重复处理失败,拒绝再次接收...");
                //第二个参数是是否放回queue中，requeue如果只有一个消费者的话，true将导致无限循坏
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {
                //logger.warn("消息即将再次返回队列处理...");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // requeue为是否重新回到队列
            }
            
    	}finally {
    		this.service.clear();
    	}
    }
	
	
	
	
	@RabbitListener(queues = "bizRemark")
	@MQConfig(cl = BizRemarkTrack.class, display = "业务标注")
    public void bizRemark(Message message, Channel channel) throws Exception {
    	
		if(!BosConstants.inited) return;
    	
    	String input=new String(message.getBody(),BosConstants.LOCAL_CODE);
    	if(DataChange.isEmpty(input)) {
    		System.out.println("bizRemark input is null");
    		return;
    	}
    	try {
	    	BosConstants.debug("bizRemark input="+input);
	    	JSONObject json=JSONObject.parseObject(input);
	    	BizRemarkTrack log=JSONObject.toJavaObject(json, BizRemarkTrack.class);
	    	 BosConstants.debug("bizRemark:" + log.getClassName()+"   "+log.getIdValue());
	        if(DataChange.isEmpty(log.getMarkId())) {
	        	log.setMarkId(BizRemarkTrack.next());
	        }
	        
	        
	        if(log.getMarkDate()==null) {
	        	log.setMarkDate(NetWorkTime.getCurrentDatetime());
	        }
	        service.save(log, null);
	        channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
	        
    	}catch(Exception e) {
    		e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                //logger.error("消息已重复处理失败,拒绝再次接收...");
                //第二个参数是是否放回queue中，requeue如果只有一个消费者的话，true将导致无限循坏
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {
                //logger.warn("消息即将再次返回队列处理...");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // requeue为是否重新回到队列
            }
            
    	}finally {
    		this.service.clear();
    	}
    }
	
	
	
	@RabbitListener(queues = "sysLog")
	@MQConfig(cl = SysLog.class, display = "系统日志")
    public void sysLog(Message message, Channel channel) throws Exception {
    	
		if(!BosConstants.inited) return;
		
    	String input=new String(message.getBody(),BosConstants.LOCAL_CODE);
    	if(DataChange.isEmpty(input)) {
    		System.out.println("dbLog input is null");
    		return;
    	}
    	try {
	    	BosConstants.debug("dbLog input="+input);
	    	JSONObject json=JSONObject.parseObject(input);
	    	SysLog log=JSONObject.toJavaObject(json, SysLog.class);

	        BosConstants.debug("sysLog:" + log.getClassName()+"   "+log.getDbAction());
	        if(DataChange.isEmpty(log.getId())) {
	        	log.setId(SysLog.next());
	        }
	        if(log.getLogTime()==null) {
	        	log.setLogTime(NetWorkTime.getCurrentDatetime());
	        }
	        service.save(log, null);
	        channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
	        
    	}catch(Exception e) {
    		e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                //logger.error("消息已重复处理失败,拒绝再次接收...");
                //第二个参数是是否放回queue中，requeue如果只有一个消费者的话，true将导致无限循坏
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {
                //logger.warn("消息即将再次返回队列处理...");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // requeue为是否重新回到队列
            }
            
    	}finally {
    		this.service.clear();
    	}
    }
    


    
    @RabbitListener(queues = "dbLog")
    public void dbLog(Message message, Channel channel) throws Exception {
    	
    	if(!BosConstants.inited) return;
    	
    	String input=new String(message.getBody(),BosConstants.LOCAL_CODE);
    	if(DataChange.isEmpty(input)) {
    		System.out.println("dbLog input is null");
    		return;
    	}
    	try {
	    	BosConstants.debug("dbLog input="+input);
	    	JSONObject json=JSONObject.parseObject(input);
	    	DBLog log=JSONObject.toJavaObject(json, DBLog.class);
	    	 BosConstants.debug("dbLog:" + log.getClassName()+"   "+log.getDbAction());
	        service.save(log, null);
	        
	        channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
	        
    	}catch(Exception e) {
    		e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                //logger.error("消息已重复处理失败,拒绝再次接收...");
                //第二个参数是是否放回queue中，requeue如果只有一个消费者的话，true将导致无限循坏
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {
                //logger.warn("消息即将再次返回队列处理...");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // requeue为是否重新回到队列
            }
            
    	}finally {
    		this.service.clear();
    	}
    }
    
    
    
    
  
    @RabbitListener(queues = "loginLog")
    @MQConfig(cl = LoginLog.class, display = "登录日志")
    public void loginLog(Message message, Channel channel) throws Exception {
    	
    	if(!BosConstants.inited) return;
    	
    	String input=new String(message.getBody(),BosConstants.LOCAL_CODE);
    	if(DataChange.isEmpty(input)) {
    		System.out.println("loginLog input is null");
    		return;
    	}
    	
    	
    	try {
    		JSONObject json=JSONObject.parseObject(input,Feature.OrderedField);
        	LoginLog log=JSONObject.toJavaObject(json, LoginLog.class);

        	if(log.getLogTime()==null) log.setLogTime(NetWorkTime.getCurrentDatetime());
            // 只包含发送的消息
        	if(DataChange.isEmpty(log.getId())) log.setId(LoginLog.next());
    		service.save(log,null);
    		BosConstants.debug("confirm loginLog input="+input);
    		service.getRdClient().loginSuccess(log.getUserId());
    		channel.basicAck(message.getMessageProperties().getDeliveryTag(),true);
    	}catch(Exception e) {
    		e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                //logger.error("消息已重复处理失败,拒绝再次接收...");
                //第二个参数是是否放回queue中，requeue如果只有一个消费者的话，true将导致无限循坏
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {
                //logger.warn("消息即将再次返回队列处理...");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // requeue为是否重新回到队列
            }
            
    	}finally {
    		this.service.clear();
    	}
    }
    
    
    
    @RabbitListener(queues = "bizView")
    @MQConfig(cl = BizViewTrack.class, display = "对象查看轨迹")
    public void bizView(Message message, Channel channel) throws Exception {
    	
    	
    	if(!BosConstants.inited) return;
    	
    	String input=new String(message.getBody(),BosConstants.LOCAL_CODE);
    	if(DataChange.isEmpty(input)) {
    		System.out.println("loginLog input is null");
    		return;
    	}
    	
    	//{"userId":"用户id","className":"类名","idValue":"业务id","ipAddress":"ip地址","token":"xxxx"}
    	
    	
    	try {
    		
    		
    		JSONObject json=JSONObject.parseObject(input,Feature.OrderedField);
    		BizViewTrack log=JSONObject.toJavaObject(json, BizViewTrack.class);
    		if(DataChange.isEmpty(input))
        	if(log.getViewTime()==null) log.setViewTime(NetWorkTime.getCurrentDatetime());
    		
    		log.setViewDate(DataChange.dateToStr(log.getViewTime()));
            // 只包含发送的消息
        	if(DataChange.isEmpty(log.getTrackId())) log.setTrackId(BizViewTrack.next());
        	
        	Table table=BosConstants.getTable(log.getClassName());
    		if(table==null) {
    			channel.basicAck(message.getMessageProperties().getDeliveryTag(),true);
    			return;
    		}
    		
    		log.setSys(table.getSys());
    		
    		BosConstants.debug("confirm BizViewTrack input="+input);
    		service.save(log, null);
    		


			SynToEs2 t = new SynToEs2(log.getIdValue(),log.getClassName());
			Thread t1 = new Thread(t);
			t1.start();
			
    		
    		
    		channel.basicAck(message.getMessageProperties().getDeliveryTag(),true);
    	}catch(Exception e) {
    		e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                //logger.error("消息已重复处理失败,拒绝再次接收...");
                //第二个参数是是否放回queue中，requeue如果只有一个消费者的话，true将导致无限循坏
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {
                //logger.warn("消息即将再次返回队列处理...");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // requeue为是否重新回到队列
            }
            
    	}finally {
    		this.service.clear();
    	}
    }
    
    
    
    @RabbitListener(queues = "behavior")
    @MQConfig(cl = UserBehaviorAnalysis.class, display = "用户行为分析")
    public void behavior(Message message, Channel channel) throws Exception {
    	
    	if(!BosConstants.inited) return;
    	
    	
    	String input=new String(message.getBody(),BosConstants.LOCAL_CODE);
    	if(DataChange.isEmpty(input)) {
    		System.out.println("behavior input is null");
    	}
    	BosConstants.debug("behavior input="+input);
    	
    	try {
    		JSONObject json=JSONObject.parseObject(input);
    		UserBehaviorAnalysis log=JSONObject.toJavaObject(json, UserBehaviorAnalysis.class);

    		service.save(log, null);
    		channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
    	}catch(Exception e) {
    		e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                //logger.error("消息已重复处理失败,拒绝再次接收...");
                //第二个参数是是否放回queue中，requeue如果只有一个消费者的话，true将导致无限循坏
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {
                //logger.warn("消息即将再次返回队列处理...");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // requeue为是否重新回到队列
            }
            
    	}finally {
    		this.service.clear();
    	}
    }
    
    
    
    @RabbitListener(queues = "invokeLog")
    public void invokeLog(Message message, Channel channel) throws Exception {
    	
    	if(!BosConstants.inited) return;
    	
    	String input=new String(message.getBody(),BosConstants.LOCAL_CODE);
    	if(DataChange.isEmpty(input)) {
    		System.out.println("invokeLog input is null");
    	}
    	BosConstants.debug("invokeLog input="+input);
    	
    	try {
    		
    		
	    	JSONObject json=JSONObject.parseObject(input);
	    	InvokeLog log=JSONObject.toJavaObject(json, InvokeLog.class);
	    	
	    	if(!DataChange.isEmpty(log.getClassName())) {
	    		
	    		if(InvokeLogController.class.getName().equals(log.getClassName())) {
		    		
		    		
		    	}else if(SysLogController.class.getName().equals(log.getClassName())) {
		    		
		    		
		    	}else if(DBLogController.class.getName().equals(log.getClassName())) {
		    		
		    		
		    	}else if(LoginLogController.class.getName().equals(log.getClassName())) {
		    		
		    		
		    	}else if(BizFieldModifyTrackController.class.getName().equals(log.getClassName())) {
		    		
		    		

		    		
		    	}else {
		    		
		    		
		    		if(log.getInvokeTime()==null) {
		    			log.setInvokeTime(NetWorkTime.getCurrentDatetime());
		    		}
		    		
		    		service.save(log, null);
		    	}
	    		
	    	}
	    	
	    	
	    	
	    	
	    	
	    	channel.basicAck(message.getMessageProperties().getDeliveryTag(), true);
	    	
    	}catch(Exception e) {
    		e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                //logger.error("消息已重复处理失败,拒绝再次接收...");
                //第二个参数是是否放回queue中，requeue如果只有一个消费者的话，true将导致无限循坏
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {
                //logger.warn("消息即将再次返回队列处理...");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // requeue为是否重新回到队列
            }
            
    	}finally {
    		this.service.clear();
    	}
    }
    
    
    
    
    
    
    @RabbitListener(queues = RabbitConfig.DEAD_LETTER_QUEUE_NAME)
    @MQConfig(cl = DelayDelete.class, display = "延迟24小时删除")
    public void delayDelete(Message message, Channel channel) throws Exception {
    	
    	
    	if(!BosConstants.inited) return;
    	
    	
    	String input=new String(message.getBody(),BosConstants.LOCAL_CODE);
    	if(DataChange.isEmpty(input)) {
    		System.out.println("delayDelete input is null");
    	}
    	BosConstants.debug("delayDelete input="+input);
    	
    	
    	try {
	    	JSONObject json=JSONObject.parseObject(input);
	    	
	    	String id=DataChange.replaceNull(json.getString("id"));
	    	String className=DataChange.replaceNull(json.getString("className"));
	    	String init=DataChange.replaceNull(json.getString("init"));
	    	
	    	if(!DataChange.isEmpty(id) && !DataChange.isEmpty(className)) {
	    		
	    		
	    		DelayDelete log=JSONObject.toJavaObject(json, DelayDelete.class);
	    		log.setDdId(DelayDelete.next());

	    		
	    		Table table=BosConstants.getTable(className);
	    		if(table!=null) {
	    			
	    			service.save(log, null);
	    			
	    			BosEntity obj=new TableJson(className);
	    			obj.setPk(id);
	    			obj.initByFormual(init, null,null);
	    			obj=service.get(obj, null);
	    			
	    			if(obj!=null && obj.getPk().equalsIgnoreCase(id)) {
	    				service.deleteById(id, className, null, true);
	    			}
	    			
	    		}
	    	}
	    	
	    	channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
	    	
    	}catch(Exception e) {
    		e.printStackTrace();
            if (message.getMessageProperties().getRedelivered()) {
                //logger.error("消息已重复处理失败,拒绝再次接收...");
                //第二个参数是是否放回queue中，requeue如果只有一个消费者的话，true将导致无限循坏
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false); // 拒绝消息
            } else {
                //logger.warn("消息即将再次返回队列处理...");
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true); // requeue为是否重新回到队列
            }
            
    	}finally {
    		this.service.clear();
    	}
    	
        
    }
    
    
}
