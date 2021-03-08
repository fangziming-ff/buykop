package com.buykop.console.thread;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.buykop.framework.annotation.SysThread;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.PMemberType;
import com.buykop.framework.oauth2.PRMemberType;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.scan.ChartForm;
import com.buykop.framework.scan.PController;
import com.buykop.framework.scan.PDBFun;
import com.buykop.framework.scan.PDBProc;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.PFormAction;
import com.buykop.framework.scan.PFormField;
import com.buykop.framework.scan.PFormRowAction;
import com.buykop.framework.scan.PFormSlave;
import com.buykop.framework.scan.PMapTJConfig;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PServiceParam;
import com.buykop.framework.scan.PServiceParamField;
import com.buykop.framework.scan.PServiceUri;
import com.buykop.framework.scan.PSysCodeType;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.scan.ThreadExecRecord;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.TreeSelectForm;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.ServiceUtil;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseCall;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TreadInf;
import com.buykop.console.util.Constants;


@SysThread(display = "自动清理子系统的配置", sys = Constants.current_sys, interval = 24*60, execCycle = "")
public class ConsoleConfig implements TreadInf{
	
	
	private static Logger  logger=LoggerFactory.getLogger(ConsoleConfig.class);
	
	
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
					service.save(ter, null);
					

						PRoot search=new PRoot();
						Vector<String> sysV=service.getMgClient().getVector(search, "code",service);
						
						
						if(true) {
							
							QueryListInfo<PForm> list=service.getMgClient().getAll(PForm.class);
							for(PForm x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								
								
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PForm.class,null,service);
									
									
									PFormField fs=new PFormField();
									fs.setFormId(x.getFormId());
									service.getMgClient().delete(fs,null,service);
		
									
									PFormSlave ss=new PFormSlave();
									ss.setFormId(x.getFormId());
									service.getMgClient().delete(ss,null,service);
		
									
									
									PFormAction pa=new PFormAction();
									pa.setFormId(x.getFormId());
									service.getMgClient().delete(pa,null,service);
									
									
									PFormRowAction ra=new PFormRowAction();
									ra.setFormId(x.getFormId());
									service.getMgClient().delete(ra,null,service);
									
									
									
								}
							}
						}
						
						
						

						
						if(true) {
							QueryListInfo<PThread> list=service.getMgClient().getAll(PThread.class);
							for(PThread x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PThread.class,null,service);
								}
							}
						}
						
						
						
						
						
						
						
						if(true) {
							QueryListInfo<PMapTJConfig> list=service.getMgClient().getAll(PMapTJConfig.class);
							for(PMapTJConfig x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PMapTJConfig.class,null,service);
								}
							}
						}
						
						
						if(true) {
							QueryListInfo<ChartForm> list=service.getMgClient().getAll(ChartForm.class);
							for(ChartForm x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), ChartForm.class,null,service);
									continue;
								}
							}
						}
						
						
						if(true) {
							QueryListInfo<PTreeForm> list=service.getMgClient().getAll(PTreeForm.class);
							for(PTreeForm x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PTreeForm.class,null,service);
									continue;
								}
							}
						}
						
						
						
						
						
						
						if(true) {
							QueryListInfo<TreeSelectForm> list=service.getMgClient().getAll(TreeSelectForm.class);
							for(TreeSelectForm x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), TreeSelectForm.class,null,service);
									continue;
								}
							}
						}
						
						
						
						
						
						if(true) {
							QueryListInfo<PController> list=service.getMgClient().getAll(PController.class);
							for(PController x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PController.class,null,service);
									continue;
								}
							}
						}
						
						
						if(true) {
							QueryListInfo<PDBFun> list=service.getMgClient().getAll(PDBFun.class);
							for(PDBFun x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PDBFun.class,null,service);
									continue;
								}
							}
						}
						
						
						if(true) {
							QueryListInfo<PDBProc> list=service.getMgClient().getAll(PDBProc.class);
							for(PDBProc x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PDBProc.class,null,service);
									continue;
								}
							}
						}
						
						
						

						
						
						if(true) {
							QueryListInfo<PMemberType> list=service.getMgClient().getAll(PMemberType.class);
							for(PMemberType x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PMemberType.class,null,service);
									continue;
								}
							}
						}
						
					
						
						if(true) {
							QueryListInfo<PRMemberType> list=service.getMgClient().getAll(PRMemberType.class);
							for(PRMemberType x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PRMemberType.class,null,service);
									continue;
								}
							}
						}
						
						if(true) {
							QueryListInfo<PRole> list=service.getMgClient().getAll(PRole.class);
							for(PRole x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PRole.class,null,service);
									continue;
								}
							}
						}
						
						
						
						if(true) {
							QueryListInfo<PServiceParam> list=service.getMgClient().getAll(PServiceParam.class);
							for(PServiceParam x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PServiceParam.class,null,service);
									continue;
								}
							}
						}
						
						
						
						if(true) {
							QueryListInfo<PServiceParamField> list=service.getMgClient().getAll(PServiceParamField.class);
							for(PServiceParamField x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PServiceParamField.class,null,service);
									continue;
								}
							}
						}
						
						
						
						if(true) {
							QueryListInfo<PServiceUri> list=service.getMgClient().getAll(PServiceUri.class);
							for(PServiceUri x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PServiceUri.class,null,service);
									continue;
								}
							}
						}
						
						
						
						
						if(true) {
							QueryListInfo<PSysCodeType> list=service.getMgClient().getAll(PSysCodeType.class);
							for(PSysCodeType x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PSysCodeType.class,null,service);
									continue;
								}
							}
						}
						
						
						if(true) {
							QueryListInfo<PSysParam> list=service.getMgClient().getAll(PSysParam.class);
							for(PSysParam x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), PSysParam.class,null,service);
									continue;
								}
							}
						}
						
						
						if(true) {
							QueryListInfo<Table> list=service.getMgClient().getAll(Table.class);
							for(Table x:list.getList()) {
								if(DataChange.isEmpty(x.getSys())) continue;
								if(!sysV.contains(x.getSys())) {
									service.getMgClient().deleteByPK(x.getPk(), Table.class,null,service);
									continue;
								}
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
