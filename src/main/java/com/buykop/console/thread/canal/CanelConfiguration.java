package com.buykop.console.thread.canal;

import java.net.InetSocketAddress;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;

@Configuration
public class CanelConfiguration {
	
	 //CanalConnector connector = CanalConnectors.newSingleConnector(new InetSocketAddress("106.15.62.70", 11111), "example", "canal","Com3#Net");
	private static Logger  logger=LoggerFactory.getLogger(CanelConfiguration.class);
	
	public static int size=100;
	
	
    @Value("${canal.host.ip}")
    private String ip;
 
    @Value("${canal.host.port}")
    private int port;
 
    @Value("${canal.host.instance}")
    private String instance;
    
    
    @Value("${canal.host.username}")
    private String userName;
    
    @Value("${canal.host.password}")
    private String password;
    
    
    @Value("${canal.host.getSize}")
    private int getSize=100;
    
    
    @Bean
    public CanalConnector getCanalClient() {
    	size=getSize;
        return  CanalConnectors.newSingleConnector(new InetSocketAddress(port), instance, userName,password);
    }
}
