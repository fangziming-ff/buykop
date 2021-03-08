package com.buykop.console.thread;


import com.buykop.console.thread.util.SynData;
import com.buykop.console.thread.util.SynToEs;
import com.buykop.console.util.CanalClient;
import com.buykop.framework.annotation.SysThread;
import com.buykop.framework.entity.SynTableDataConfig;
import com.buykop.framework.entity.SynTableDataItem;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.scan.ThreadExecRecord;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.ServiceUtil;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;
import com.buykop.framework.util.type.TreadInf;
import com.buykop.console.util.Constants;


@SysThread(display = "表数据同步", sys = Constants.current_sys, interval = 120, execCycle = "")
public class DataSyn implements TreadInf{
	
	private static Logger  logger=LoggerFactory.getLogger(DataSyn.class);

	String execUserId;
	
	
	String synId;
	
	private static final int pageSize=100;
	
	static boolean flag=false;
	
	
	public void invoke(String execUserId) {
		this.execUserId=execUserId;
		Call remote=new Call();
		Thread t1 = new Thread(remote);
		t1.start(); 
	}
	
	
	
	public class Call implements Runnable {
			
		
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
					
					

					SynTableDataConfig ss=new SynTableDataConfig();
					ss.setStatus(1L);
					QueryListInfo<SynTableDataConfig> list=service.getList(ss, null);
				
					for(SynTableDataConfig config:list.getList()) {
						// 发起线程进行同步
						SynData call = new SynData(config.getSrcClassName());
						Thread t1 = new Thread(call);
						t1.start();
					}
					
					
					ter.setRemark("成功");
					
					
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
