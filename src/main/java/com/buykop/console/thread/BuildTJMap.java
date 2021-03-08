package com.buykop.console.thread;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.buykop.console.service.impl.MapTJService;
import com.buykop.framework.annotation.SysThread;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.scan.ThreadExecRecord;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.ClientPool;
import com.buykop.framework.util.ServiceUtil;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseCall;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TreadInf;
import com.buykop.console.util.Constants;


@SysThread(display = "重建指标信息", sys = Constants.current_sys, interval = 60, execCycle = "")
public class BuildTJMap implements TreadInf{
	
	private static Logger  logger=LoggerFactory.getLogger(BuildTJMap.class);
	
	static int pageSize=1000;
	
	String execUserId;
	
	static boolean flag=false;
	
	
	public void invoke(String execUserId) {
		this.execUserId=execUserId;
		Call remote=new Call();
		Thread t1 = new Thread(remote);
		t1.start(); 
	}
	


	public class Call extends BaseCall implements Runnable {
			
		
		public Call() {
			
		}
		
	
		public void run() {
			// TODO Auto-generated method stub
			
			if(flag) {
				return;
			}
			
			ServiceInf service=SpringContextUtil.getBean(BosService.class);

			String className=this.getClass().getName().substring(0, this.getClass().getName().length()-5);
			
			try {
				
				PThread t=service.getMgClient().getById(className,PThread.class);
				if(t==null) {
					flag=false;
					return;
				}
				
				if(DataChange.isEmpty(t.getServerId())) {
					flag=false;
					return;
				}
				
				if(!t.getServerId().equals(ServiceUtil.currentConfig.getServerId())) {
					flag=false;
					return;
				}
				
				
				
				
				
				
				ThreadExecRecord ter=new ThreadExecRecord();
				ter.setRecordId(ThreadExecRecord.next());
				ter.setClassName(className);
				ter.setExecTime(NetWorkTime.getCurrentDatetime());
				ter.setUserId(execUserId);
				
				
				
				try {
					
					
					flag=true;
					ter.setRemark("正在执行中");
					service.save(ter, null);

						
					MapTJService mapTJService=SpringContextUtil.getBean(MapTJService.class);
					
					try {
						
						//mapTJService.buildMapTJResult(conn);
					}catch(Exception e) {
						e.printStackTrace();
					}
						
					
					
					
					this.upload(className);
					
					
					
					
					ter.setRemark(BosConstants.RET_SUCCESS);
				}catch(Exception e) {
					e.printStackTrace();
					ter.setRemark(e.getMessage());
				}finally {
					
					flag=false;
					
					try {
						ter.setFinishTime(NetWorkTime.getCurrentDatetime());
						ter.setExecSecond((ter.getFinishTime().getTime()-ter.getExecTime().getTime())/1000);
						service.save(ter, null);
						
						PThread tx=service.getMgClient().getById(ter.getClassName(),PThread.class);
						if(tx!=null) {
							tx.setExecTime(NetWorkTime.getCurrentDatetime());
							tx.setExecSecond(ter.getExecSecond());
							service.save(tx, null);
						}
						
					}catch(Exception e) {
						e.printStackTrace();
					}
					
				}
				
				
				
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				service.clear();
			}
			
			
			
			
		}
		
	}
		
	
}
