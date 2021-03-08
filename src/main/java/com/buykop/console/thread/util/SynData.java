package com.buykop.console.thread.util;

import java.util.List;
import java.util.Vector;

import com.buykop.framework.entity.SynTableDataConfig;
import com.buykop.framework.entity.SynTableDataItem;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;


public class SynData implements Runnable {
	
	
	private String className;
	
	
	static final int pageSize=100;
	
	public SynData(String className) {
		this.className=className;
	}

	
	public void run() {
		// TODO Auto-generated method stub
		
		try {
			
			
			
			Table table=BosConstants.getTable(className);
		
			if(table==null) return;
			
			
			
			ServiceInf service=SpringContextUtil.getBean(BosService.class);
			
			
			try {
				
				SynTableDataConfig config=SynTableDataConfig.synTableDataConfig(this.className,service);
				
				if(config==null) {
					return;
				}
				
				
				long totalCount=service.getAllCount(table.getClassName());
				
				
				long maxpage = (totalCount / pageSize);
				if (totalCount > maxpage * pageSize) {
					maxpage++;
				}
				
				
				BosConstants.debug("--------------SynData  className="+className+"  totalCount="+totalCount+"---------------maxpage="+maxpage+"---------");
				
				for(long i=1;i<=maxpage;i++) {
					BosEntity search=new TableJson(table.getClassName());
					QueryFetchInfo<BosEntity> fetch=new QueryFetchInfo<BosEntity>();
					
					
					if(table.table()) {
						fetch=service.getRBaseDao().getFetch(search, table.getSortField(), i, pageSize,service);
					}else if(table.mongo()) {
						fetch=service.getMgClient().getFetch(search, table.getSortField(), i, pageSize,service);
					}
					
					BosConstants.debug("--------------SynData  totalCount="+totalCount+"---------------page="+i+"/"+maxpage+"------size="+fetch.size()+"---");
					
					for(BosEntity obj:fetch.getList()) {
						config.synData(obj, service, null);
					}
				}
			}catch(Exception e) {
				e.printStackTrace();
			}finally {
				service.clear();
			}
			
			//如果是非es存储,且es优先查询的,需要放弃es优先查询
			
			
		
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

}
