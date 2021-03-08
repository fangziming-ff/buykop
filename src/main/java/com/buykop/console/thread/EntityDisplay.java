package com.buykop.console.thread;

import com.buykop.framework.annotation.SysThread;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.scan.ThreadExecRecord;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.ServiceUtil;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseCall;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TreadInf;
import com.buykop.console.util.Constants;


@SysThread(display = "更新实体类的显示信息", sys = Constants.current_sys, interval = 24*60, execCycle = "")
public class EntityDisplay implements TreadInf{
	
	
	private static Logger  logger=LoggerFactory.getLogger(EntityDisplay.class);
	
	
	static int pageSize=1000;
	
	String execUserId;
	
	
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
			
			
			//int hour=DateUtil.getHour(NetWorkTime.getCurrentDatetime());
			
			//if(hour<=4) {
				
			//}else {
				//return;
			//}
			
			UserToken ut=new UserToken();
			
			
			
			if(flag) {
				return;
			}
			
			String className=this.getClass().getName().substring(0, this.getClass().getName().length()-5);
			
			
			BosService service=SpringContextUtil.getBean(BosService.class);
			
			
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
					service.save(ter, ut);
					
					//int hour=DateUtil.getHour(NetWorkTime.getCurrentDatetime());
		
						
					if(true) {
						
						
						Table search=new Table();
						search.setCache(0L);
						search.setRedisEntity(1);
						QueryListInfo<Table> list=service.getMgClient().getList(search,service);
						
						BosConstants.debug("待处理的表单任务:"+list.size());
						
						
						
						for(Table tt:list.getList()) {
							
							if(tt==null) continue;
							
							
							if(tt.judgePK()) {
								tt.setIsMaster(1L);
							}else {
								tt.setIsMaster(0L);
							}
							
							service.save(tt, ut);
							
							if(DataChange.isEmpty(tt.getDisFieldFormula()) && DataChange.isEmpty(tt.getDisField()) ) continue;
							
							try {
								service.refreshCacheData(tt,service);
							}catch(Exception e) {
								e.printStackTrace();
							}
						}
						
						list.clear();
					}
					
					
					
					ter.setRemark(BosConstants.RET_SUCCESS);
				}catch(Exception e) {
					e.printStackTrace();
					ter.setRemark(e.getMessage());
				}finally {
					
					flag=false;
					
					try {
						ter.setFinishTime(NetWorkTime.getCurrentDatetime());
						ter.setExecSecond((ter.getFinishTime().getTime()-ter.getExecTime().getTime())/1000);
						service.save(ter, ut);
						
						PThread tx=service.getMgClient().getById(ter.getClassName(),PThread.class);
						if(tx!=null) {
							tx.setExecTime(NetWorkTime.getCurrentDatetime());
							tx.setExecSecond(ter.getExecSecond());
							service.save(tx, ut);
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
