package com.buykop.console.util;

import com.buykop.console.service.impl.MainService;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.ClientPool;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;

public class SysInitThread implements Runnable {

	public void run() {
		
		MainService service = SpringContextUtil.getBean(MainService.class);
		

		try {

			
			service.init();
			
			
			// 自动注册的表建立es索引
			Table table = new Table();
			table.setSys(Constants.current_sys);
			table.setRegType(0L);
			QueryListInfo<Table> list = service.getMgClient().getList(table, "className",service);
			for (Table x : list.getList()) {
				
				if (DataChange.getLongValueWithDefault(x.getSynEs(), 0) == 1) {
					if (!service.getEsClient().indexExists(x)) {
						service.getEsClient().indexSyn(x, service);
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			service.clear();
		}
	}

}
