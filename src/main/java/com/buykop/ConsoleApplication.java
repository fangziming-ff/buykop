package com.buykop;

import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.buykop.console.entity.PUser;
import com.buykop.console.service.impl.MainService;
import com.buykop.console.util.Constants;
import com.buykop.console.util.SysInitThread;
import com.buykop.framework.annotation.SysConfig;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.ClientPool;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.SysInit;
import com.buykop.framework.util.data.MD5Encrypt;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;

@SpringBootApplication
//@EnableDiscoveryClient
//@EnableFeignClients
//@EnableCaching
//@EnableTransactionManagement
//@Configuration
@SysConfig(displayName = Constants.current_sys_name, sys = Constants.current_sys,open=true)
public class ConsoleApplication extends WebMvcConfigurationSupport {

	public static void main(String[] args) {
		
		SpringApplication.run(ConsoleApplication.class, args);
		
		ServiceInf service = SpringContextUtil.getBean(MainService.class);
		SysInit.init(Constants.current_sys, service);

		BosConstants.inited = true;
		
		
		try {
			
			BosConstants.debug("--------------------------------------------------------------------------------------");
		
			PUser user=new PUser();
			user.setUserId("1");
			user.setLoginPwd("111111");
			user.setStatus(1L);
			service.getMgClient().save(user,null, service);
			BosConstants.debug("--------------------------------------------------------------------------------------");
			
			PUserMember um=new PUserMember();
			um.setMemberId("1");
			um.setUserId("1");
			um.setStatus(1L);
			service.save(um, null);

			service.getRdClient().loginSuccess(user.getPk());
				
				

			
		}catch(Exception e){
			e.printStackTrace();
		}finally {
			service.clear();
		}
		
		
		
		SysInitThread t = new SysInitThread();
		Thread t1 = new Thread(t);
		t1.start();
		
		System.out.println("--------------start ok-----------------");
		
	}
	
	
	
	

	@Bean
	@LoadBalanced
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public HttpMessageConverters fastJsonHttpMessageConverters() {
		// 1、定义一个convert转换消息的对象
		FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
		// 2、添加fastjson的配置信息
		FastJsonConfig fastJsonConfig = new FastJsonConfig();
		fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat);
		fastJsonConfig.setFeatures(Feature.OrderedField);
		// 3、在convert中添加配置信息
		fastConverter.setFastJsonConfig(fastJsonConfig);
		// 4、将convert添加到converters中
		HttpMessageConverter<?> converter = fastConverter;
		return new HttpMessageConverters(converter);
	}

	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		// 1、定义一个convert转换消息的对象
		FastJsonHttpMessageConverter fastConverter = new FastJsonHttpMessageConverter();
		// 2、添加fastjson的配置信息
		FastJsonConfig fastJsonConfig = new FastJsonConfig();
		fastJsonConfig.setSerializerFeatures(SerializerFeature.PrettyFormat);
		fastJsonConfig.setFeatures(Feature.OrderedField);
		// 3、在convert中添加配置信息
		fastConverter.setFastJsonConfig(fastJsonConfig);
		// 4、将convert添加到converters中
		converters.add(fastConverter);
		// 5、追加默认转换器
		super.addDefaultHttpMessageConverters(converters);
	}


}
