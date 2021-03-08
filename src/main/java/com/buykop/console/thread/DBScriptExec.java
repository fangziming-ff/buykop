package com.buykop.console.thread;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.alibaba.fastjson.JSONObject;
import com.buykop.framework.annotation.SysThread;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.scan.DBScript;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.scan.ThreadExecRecord;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.ServiceUtil;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.DateUtil;
import com.buykop.framework.util.type.BaseCall;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TreadInf;
import com.buykop.console.util.Constants;



@SysThread(display = "数据库脚本自动执行", sys = Constants.current_sys, interval = 1, execCycle = "")
public class DBScriptExec implements TreadInf{
	
	
	private static Logger  logger=LoggerFactory.getLogger(DBScriptExec.class);
	
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
					service.save(ter, null);
					
					
					StringBuffer sb=new StringBuffer();
					
		
					DBScript   search=new DBScript();
					search.setStatus(1L);
					QueryListInfo<DBScript> list=service.getMgClient().getList(search,service);
					for(DBScript x:list.getList()) {
						
						if(x.getExecTime()==null) {
							x.setExecTime(NetWorkTime.getCurrentDatetime());
						}
						
						
						if(DataChange.isEmpty(x.getScript())) continue;
						if(DataChange.isEmpty(x.getSys())) continue;
						
						if(x.getExecCycle()==null ) {
							x.setExecCycle(0L);
							service.save(x, null);
							continue;
						}
						
						
						if(!DateUtil.cycleCheck(x.getExecCycleTime())) continue;
						
						//0:手动   1:每分钟    2:每10分钟    3：每30分钟  10:每小时   20:每天    30:每周    40：每月    50:每季     60：每年
						if(x.getExecCycle().intValue()==0) {
							continue;
							
						}else if(x.getExecCycle().intValue()==1) {		
							int m1=DateUtil.getMinute(x.getExecTime());
							int m2=DateUtil.getMinute(NetWorkTime.getCurrentDatetime());
							if(m1==m2) {
								continue;
							}
						}else if(x.getExecCycle().intValue()==2) {		
							int m1=DateUtil.getMinute(x.getExecTime());
							int m2=DateUtil.getMinute(NetWorkTime.getCurrentDatetime());
							
							if(m1==m2) {
								continue;
							}else if( m2>m1 &&  m2-m1<10  ) {
								continue;
							}else if( m2<m1 &&  m2+60-m1<10  ) {
								continue;
							}	
							
							
						}else if(x.getExecCycle().intValue()==3) {		
							int m1=DateUtil.getMinute(x.getExecTime());
							int m2=DateUtil.getMinute(NetWorkTime.getCurrentDatetime());
							
							if(m1==m2) {
								continue;
							}else if( m2>m1 &&  m2-m1<30  ) {
								continue;
							}else if( m2<m1 &&  m2+60-m1<30  ) {
								continue;
							}	
							
						}else if(x.getExecCycle().intValue()==10) {
							if(DateUtil.getHour(x.getExecTime())==DateUtil.getHour(NetWorkTime.getCurrentDatetime())) {
								continue;
							}
							
						}else if(x.getExecCycle().intValue()==20) {
							if(DataChange.dateToStr(x.getExecTime()).equals(NetWorkTime.getCurrentDateString())) {
								continue;
							}
						}else if(x.getExecCycle().intValue()==30) {
							if(DateUtil.getWeek(x.getExecTime())==DateUtil.getWeek(NetWorkTime.getCurrentDatetime())) {
								continue;
							}
						}else if(x.getExecCycle().intValue()==40) {
							if(x.getExecTime()!=null){
								if(DateUtil.getMonth(x.getExecTime())==DateUtil.getMonth(NetWorkTime.getCurrentDatetime())) {
									continue;
								}
							}
							
						}else if(x.getExecCycle().intValue()==50) {
							if(DateUtil.getQuarter(x.getExecTime())==DateUtil.getQuarter(NetWorkTime.getCurrentDatetime())) {
								continue;
							}
						}else if(x.getExecCycle().intValue()==60) {
							if(DateUtil.getYear(x.getExecTime())==DateUtil.getYear(NetWorkTime.getCurrentDatetime())) {
								continue;
							}
						}else {
							continue;
						}
						
						
						x.setUserId(execUserId);
						x.setExecTime(NetWorkTime.getCurrentDatetime());
						
						
						
						
						try {
							service.getBaseDao().runScript(x.sqlScript());
							x.setRemark(BosConstants.RET_SUCCESS);
						}catch(Exception e) {
							e.printStackTrace();
							sb.append(e.getMessage());
							x.setRemark(e.getMessage());
						}
						
						x.setEndTime(NetWorkTime.getCurrentDatetime());
						
						service.save(x, null);
						
					}
					
					String msg=sb.toString();
					if(DataChange.isEmpty(msg)) {
						msg=BosConstants.RET_SUCCESS;
					}
					
					ter.setRemark(msg);
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
