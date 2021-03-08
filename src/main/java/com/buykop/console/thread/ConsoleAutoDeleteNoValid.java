package com.buykop.console.thread;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.buykop.framework.annotation.SysThread;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.scan.ThreadExecRecord;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.ClientPool;
import com.buykop.framework.util.ServiceUtil;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseCall;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;
import com.buykop.framework.util.type.TreadInf;
import com.buykop.console.util.Constants;

@SysThread(display = "删除软删除标识的数据", sys = Constants.current_sys, interval = 7*24*60, execCycle = "")
public class ConsoleAutoDeleteNoValid implements TreadInf{
	
	private static Logger  logger=LoggerFactory.getLogger(ConsoleAutoDeleteNoValid.class);
	
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
					service.save(ter,null);

		
		
					Table st=new Table();
					st.setCache(0L);
					QueryListInfo<Table> list=service.getMgClient().getList(st,"className",service);
					
					for(Table x:list.getList()) {
						
						if(!x.judgePK()) continue;
						
						BosEntity search=new TableJson(x.getClassName());
						search.setIsValid(0);
						search.setUpdateTime(NetWorkTime.getCurrentDate());
						search.addPropertyOperation("updateTime", 6);
						QueryListInfo<BosEntity> mList=service.getBaseDao().getList(search,null,service);
						for(BosEntity m:mList.getList()) {
							if(m.getIsValid()==null) continue;
							if(m.getIsValid().intValue()!=0) continue;
							service.deleteById(m.getPk(), x.getClassName(), null, true);
						}
						
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
