package com.buykop.console.thread.util;

import java.util.List;

import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;


public class SynToEs implements Runnable {
	
	
	private String className;
	
	
	static final int pageSize=100;
	
	public SynToEs(String className) {
		this.className=className;
	}

	
	public void run() {
		// TODO Auto-generated method stub
		
		try {
			
			
			
			Table table=BosConstants.getTable(className);
		
			if(table==null) return;
			
			
			if(DataChange.getLongValueWithDefault(table.getSynEs(), 0)!=1) {
				return;
			}
			
			
			ServiceInf service=SpringContextUtil.getBean(BosService.class);
			
			boolean flag=false;
			
			
			
			
			
			try {
				
				
				//如果是非es存储,且es优先查询的,需要放弃暂时放弃es优先查询
				if(DataChange.getLongValueWithDefault(table.getQueryEs(), 0)==1) {
					flag=true;
					table.setQueryEs(0L);
					Table obj=service.getMgClient().getTableById(table.getClassName());
					obj.setQueryEs(0L);
					service.save(obj, null);
					new ClassInnerNotice().invoke(BosConstants.innerNotice_synTable, table.getClassName());
				}
			
				List<Field> fList=table.listEncryptionField();
				
			
				long totalCount=service.getAllCount(table.getClassName());
				
				
				
				
				
				long maxpage = (totalCount / pageSize);
				if (totalCount > maxpage * pageSize) {
					maxpage++;
				}
				
				
				BosConstants.debug("--------------SynToEs  className="+className+"  totalCount="+totalCount+"---------------maxpage="+maxpage+"---------");
				
				for(long i=1;i<=maxpage;i++) {
					BosEntity search=new TableJson(table.getClassName());
					QueryFetchInfo<BosEntity> fetch=new QueryFetchInfo<BosEntity>();
					
					
					if(table.table()) {
						fetch=service.getRBaseDao().getFetch(search, table.getSortField(), i, pageSize,service);
					}else if(table.mongo()) {
						fetch=service.getMgClient().getFetch(search, table.getSortField(), i, pageSize,service);
					}
					
					BosConstants.debug("--------------SynToEs  totalCount="+totalCount+"---------------page="+i+"/"+maxpage+"------size="+fetch.size()+"---");
					
					for(BosEntity obj:fetch.getList()) {
						
						for(Field x:fList) {
							obj.addEncryption(x.getProperty());
						}
						
						service.getEsClient().syn(obj, service);
					}
				}
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				//如果是非es存储,且es优先查询的,需要放弃es优先查询
				if(flag) {
					table.setQueryEs(1L);
					Table obj=service.getMgClient().getTableById(table.getClassName());
					obj.setQueryEs(1L);
					service.save(obj, null);
					new ClassInnerNotice().invoke(BosConstants.innerNotice_synTable, table.getClassName());
				}
				service.clear();
			}
			
			
			
		
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

}
