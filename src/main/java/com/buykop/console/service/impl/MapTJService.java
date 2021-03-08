package com.buykop.console.service.impl;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.framework.cache.location.ExpiringMap;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.UrlUtil;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.PMapTJConfig;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.Calculation;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseService;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.TableJson;

@Service
@Component
public class MapTJService extends BaseService implements com.buykop.console.service.MapTJService{
	
	@Autowired
	private AmqpTemplate rabbitTemplate;

	public AmqpTemplate getRabbitTemplate() {
		return rabbitTemplate;
	}
	
	
	/**
	 * 
	 */
	public void buildMapTJResult(UserToken token) throws Exception{
		
		RdClient conn=this.getRdClient();
		
		HashMap<String,PMapTJConfig> paramHash=new HashMap<String,PMapTJConfig>();
		
		
		//String tjClassName=DataChange.replaceNull(PSysParam.paramValue("1", Constants.paramMapTJClassName));
		
		

		Long tjMethod=PSysParam.paramLongValue("1", BosConstants.paramMapTJMethod);
		if(tjMethod==null) {//0:平均值   1:最新值
			tjMethod=1L;
		}

		StringBuffer log=new StringBuffer();
		
		PMapTJConfig config=new PMapTJConfig();
		config.setStatus(1L);
		QueryListInfo<PMapTJConfig> cList=this.getMgClient().getList(config, "seq",this);
		
		//QueryListInfo<PMapTJConfig> cList=new QueryListInfo<PMapTJConfig>();
		
		JSONArray configArr=new JSONArray();
		for(PMapTJConfig c:cList.getList()) {
			
			JSONObject cf=new JSONObject(true);
			
			if(DataChange.isEmpty(c.getClassName())) continue;
			
			if(DataChange.isEmpty(c.getId())) continue;
			
			if(!DataChange.isEmpty(c.getDataPerType()))  continue;
			
			Table table=BosConstants.getTable(c.getClassName());
			if(table==null) continue;
			
			PRoot root=this.getMgClient().getById(table.getSys(),PRoot.class);
			if(root==null || root.getStatus()==null || root.getStatus().intValue()!=1) {
				continue;
			}
			
			paramHash.put(c.getId(),c);
			
			
			cf.put("id", c.getId());
			cf.put("text", c.getName());
			cf.put("sys", root.getCode());
			cf.put("class", table.getSimpleName());
			configArr.add(cf);
		}
		
		
		
		if(paramHash.size()<=0) {
			//MainController.executeMapTJResult=false;
			return;
		}
		
		HashMap<String,BosEntity> placeHash=new HashMap<String,BosEntity>();
		
		String fileName=MyString.replace(NetWorkTime.getCurrentDatetimeString()+".log", ":", ""); 
		
		BosEntity place=new TableJson(BosConstants.placeClassName);
		QueryListInfo<BosEntity> pList=this.getList(place, null);
		for(BosEntity x:pList.getList()) {
			placeHash.put(x.getPk(), x);
		}
		
		//log.append("地区数量="+pList.size()+"\n\r");
		//UrlTools.logFile(log,fileName);
		//json.put("paramList", configArr);
	
		//UserToken token=new UserToken();
		
		//JSONObject sftpConfig=PFileUpload.getSFTPConfig();
		
		//SFTPUtil sftp = new SFTPUtil(sftpConfig.getString("fileServerLogin"), sftpConfig.getString("fileServerPwd"), sftpConfig.getString(Constants.paramFileServerIp), DataChange.StringToInteger(sftpConfig.getString("fileServerPort")));  

		
		
		
		HashMap<String,JSONObject> joHash=new HashMap<String,JSONObject>();
		
		JSONObject root=CacheTools.getJson("BOS_MAPTJ_ROOT");
		
		
		int count=0;
		
		if(true) {//单个合计
			//BOS_MAPTJ_ROOT

			
			if(root==null) {
				root=new JSONObject(true);
			}
			
			
			Iterator<String> its=paramHash.keySet().iterator();
			while(its.hasNext()) {
				
				PMapTJConfig c=paramHash.get(its.next());

				if(!DataChange.isEmpty(c.getCalculateFormula())) continue;
				
				try {
					//if(hour<=4 ||  hour>=22) {
						//c.initMapTJResult("ROOT",jo,placeHash, token, this,true);
					//}else {
						c.initMapTJResult("ROOT",root,placeHash, token,token.getLan(),this);
					//}
					count++;
				}catch(Exception e) {
					e.printStackTrace();
					log.append(e.getMessage()+"\n\r");
				}
				
			}
			
			log.append("写入参数数量="+count+"\n\r");
			UrlUtil.logFile(log,fileName);
			
			
			//if(group) this.uploadJson(sftp,jo, "/mapTJ/"+NetWorkTime.getCurrentYear()+"/"+NetWorkTime.getCurrentMonth()+"/"+NetWorkTime.getCurrentDateString(), "root.json");

			CacheTools.putJson("BOS_MAPTJ_ROOT", root, 48*3600);

			BosConstants.debug("save BOS_MAPTJ_ROOT");
			
			//json.put("root", jo);
			
			log.append("完成 加入 root \n\r");
			UrlUtil.logFile(log,fileName);
			
		}
			
			
			
			
			
			
			
		if(true) {
			
			BosConstants.debug("**************************all*****no map******************************");
			
			Iterator<String> its=paramHash.keySet().iterator();
			
			while(its.hasNext()) {
				
				PMapTJConfig c=paramHash.get(its.next());
				
				if(!DataChange.isEmpty(c.getCalculateFormula())) continue;
				
				
				try {
					c.initMapTJResult(joHash, token, this,conn);
				}catch(Exception e) {
					log.append(e.getMessage()+"\n\r");
				}
				
			}
			
			
		}
			
		
		
		if(true) {
			
			joHash.put("ROOT", root);
		
			Iterator<String> itsx=joHash.keySet().iterator();
			
			
			BosConstants.debug("save BOS_MAPTJ joHash size="+joHash.size());
			
			while(itsx.hasNext()) {
				
				String id=itsx.next();
				
				
				JSONObject jo=joHash.get(id);
				
				
				Iterator<String> its=paramHash.keySet().iterator();
				while(its.hasNext()) {
					
					PMapTJConfig c=paramHash.get(its.next());

	
					if(DataChange.isEmpty(c.getCalculateFormula())) continue;
					
					//开始计算
					try {
						
						Long accuracy=c.getTjAccuracy();
						if(accuracy==null) {
							accuracy=0L;
						}
						
						String result=DataChange.replaceNull(new MyString().replaceLongByJson(c.getCalculateFormula(), jo,"0"));
						
						result=Calculation.getExpressionValue(result, accuracy.intValue());
						
						if(DataChange.isEmpty(result)) {
							result="0";
						}
						
						jo.put(c.getId(), result);
						
					}catch(Exception e) {
						log.append(e.getMessage()+"\n\r");
					}
					
					
				}
				
				CacheTools.putJson("BOS_MAPTJ_"+id, jo, 48*3600);
				
				BosConstants.debug("save BOS_MAPTJ_"+id);
				
				UrlUtil.saveJson(jo, "/mapTJ/"+NetWorkTime.getCurrentYear()+"/"+NetWorkTime.getCurrentMonth()+"/"+NetWorkTime.getCurrentDateString(), id+".json");
				
			}	
		}
		
		
		
		
		placeHash.clear();
		joHash.clear();
		
		
		//Vector<String> v=this.getRdClient().getKeys("BOS_MAPTJ_Q_*");
		//for(String x:v) {
			//this.getRdClient().remove(x);
		//}
		
		BosConstants.getExpireHash().removeMatch("BOS_MAPTJ_Q_");
		new ClassInnerNotice().invoke(ExpiringMap.class.getSimpleName(), "BOS_MAPTJ_Q_");
		

	}
	
	
	
	
	
	
	
	
	public static void main(String[] args) {
		
		System.out.println(50%200);
		
		
	}
}
