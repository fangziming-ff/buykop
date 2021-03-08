package com.buykop.console.thread.canal;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;

@ConfigurationProperties(prefix = "canal.host")
public class CanelConfig {

	private static Logger  logger=LoggerFactory.getLogger(CanelConfig.class);
	
	public String ip;
	public String port;
	public String username;
	public String password;
	public String instance;
	public String getSize;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	

	public String getPort() {
		return port;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getInstance() {
		return instance;
	}

	public void setInstance(String instance) {
		this.instance = instance;
	}

	public String getGetSize() {
		return getSize;
	}

	public void setGetSize(String getSize) {
		this.getSize = getSize;
	}

	public void setPort(String port) {
		this.port = port;
	}

	
}
