package com.buykop.console.thread.canal;

import java.util.List;

import com.alibaba.otter.canal.protocol.CanalEntry;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.type.ServiceInf;

public class ActionTask implements Runnable {
	
	
	private static Logger  logger=LoggerFactory.getLogger(ActionTask.class);

	private List<CanalEntry.Column> beforeColumns;
	private List<CanalEntry.Column> afterColumns;
	private PRoot root;
	private Table table;
	private ServiceInf service;
	private CanalEntry.EventType eventType;
	private long timestamp;

	public List<CanalEntry.Column> getBeforeColumns() {
		return beforeColumns;
	}

	public void setBeforeColumns(List<CanalEntry.Column> beforeColumns) {
		this.beforeColumns = beforeColumns;
	}

	public List<CanalEntry.Column> getAfterColumns() {
		return afterColumns;
	}

	public void setAfterColumns(List<CanalEntry.Column> afterColumns) {
		this.afterColumns = afterColumns;
	}

	public PRoot getRoot() {
		return root;
	}

	public void setRoot(PRoot root) {
		this.root = root;
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public ServiceInf getService() {
		return service;
	}

	public void setService(ServiceInf service) {
		this.service = service;
	}

	public CanalEntry.EventType getEventType() {
		return eventType;
	}

	public void setEventType(CanalEntry.EventType eventType) {
		this.eventType = eventType;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public void run() {
		
		
		

	}
	


}
