package com.buykop.console.thread;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import com.alibaba.fastjson.JSONObject;
import com.buykop.framework.annotation.SysThread;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.UrlUtil;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.mongodb.MgClient;
import com.buykop.framework.scan.PController;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.scan.ThreadExecRecord;
import com.buykop.framework.scan.ServerConfig;
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


@SysThread(display = "检查节点", sys = Constants.current_sys, interval = 10, execCycle = "")
public class ConsoleCheckController implements TreadInf {

	
	private static Logger  logger=LoggerFactory.getLogger(ConsoleCheckController.class);
	
	String execUserId;

	static boolean flag = false;

	public void invoke(String execUserId) {
		this.execUserId = execUserId;
		Call remote = new Call();
		Thread t1 = new Thread(remote);
		t1.start();
	}

	public class Call implements Runnable {

		public Call() {

		}

		public void run() {
			// TODO Auto-generated method stub

			if (flag) {
				return;
			}
			
			
			ServiceInf service=SpringContextUtil.getBean(BosService.class);

			String className = this.getClass().getName().substring(0, this.getClass().getName().length() - 5);

			try {

				PThread t = service.getMgClient().getById(className, PThread.class);
				if (t == null) {
					flag = false;
					return;
				}

				if (DataChange.isEmpty(t.getServerId())) {
					flag = false;
					return;
				}

				if (!t.getServerId().equals(ServiceUtil.currentConfig.getServerId())) {
					flag = false;
					return;
				}

				
				
				
				
				ThreadExecRecord ter = new ThreadExecRecord();
				ter.setRecordId(ThreadExecRecord.next());
				ter.setClassName(className);
				ter.setExecTime(NetWorkTime.getCurrentDatetime());
				ter.setUserId(execUserId);

				HashMap<String, QueryListInfo<ServerConfig>> serverHash = new HashMap<String, QueryListInfo<ServerConfig>>();

				try {

					flag = true;
					ter.setRemark("正在执行中");
					service.save(ter, null);

					if (true) {

						QueryListInfo<ServerConfig> sList = service.getMgClient().getAll(ServerConfig.class);
						for (ServerConfig s : sList.getList()) {

							if (DataChange.isEmpty(s.getIp())) {
								continue;
							}

							if (DataChange.isEmpty(s.getSys())) {
								continue;
							}

							if (s.getPort() == null) {
								continue;
							}

							QueryListInfo<ServerConfig> scList = serverHash.get(s.getSys());
							if (scList == null) {
								scList = new QueryListInfo<ServerConfig>();
								serverHash.put(s.getSys(), scList);
							}

							String url = "http://" + s.getIp() + ":" + s.getPort() + "";
							try {
								String ip = UrlUtil.doGet(url);
								if (!BosConstants.runTimeMode()) {
									BosConstants.debug("监控服务节点: url=" + url + "   local ip=" + ip);
								}

								s.setMsg(null);
								scList.getList().add(s);

							} catch (Exception e) {
								e.printStackTrace();
								s.setMsg(e.getMessage());
								if (DataChange.isEmpty(s.getMsg())) {
									s.setMsg(e.getLocalizedMessage());
								}
								if (DataChange.isEmpty(s.getMsg())) {
									s.setMsg("检查失败");
								}

								// Pool.getInstance().getConn().deleteByPK(s.getPk(), ServerConfig.class);
							}

							try {
								s.addToMust("msg");
								s.initServerId();
								s.setAccessTime(NetWorkTime.getCurrentDatetime());
								service.save(s, null);
							} catch (Exception e) {
								e.printStackTrace();
							}

						}

						sList.clear();

					}
					

					ter.setRemark(BosConstants.RET_SUCCESS);
				} catch (Exception e) {
					e.printStackTrace();
					ter.setRemark(e.getMessage());
				} finally {

					flag = false;

					try {
						ter.setFinishTime(NetWorkTime.getCurrentDatetime());
						ter.setExecSecond((ter.getFinishTime().getTime() - ter.getExecTime().getTime()) / 1000);
						service.save(ter, null);

						PThread tx = service.getMgClient().getById(ter.getClassName(), PThread.class);
						if (tx != null) {
							tx.setExecTime(NetWorkTime.getCurrentDatetime());
							tx.setExecSecond(ter.getExecSecond());
							service.save(tx, null);
						}

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				
				
				
				
				
				
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				service.clear();
			}

			

		}

	}

}
