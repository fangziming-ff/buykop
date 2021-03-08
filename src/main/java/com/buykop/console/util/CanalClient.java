package com.buykop.console.util;


import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.buykop.console.thread.CanalSyn;
import com.buykop.console.thread.canal.CanelConfiguration;
import com.buykop.framework.entity.BizFieldModifyTrack;
import com.buykop.framework.entity.SynTableDataConfig;
import com.buykop.framework.entity.SynTableDataItem;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PThread;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.CheckUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.BosService;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.ListData;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.framework.util.type.TableJson;

public class CanalClient {
	
	
	
	
	
	public static int emptyActionCount=120;
	
	
	
	public String exec() {
		
		CanalConnector connector=SpringContextUtil.getBean(CanalConnector.class);
		
		 int emptyCount = 0;
		 

		
		try {
			
			connector.connect();
			 //订阅实例中所有的数据库和表
	    	connector.subscribe(".*\\..*");
	        // 回滚到未进行ack的地方
	    	connector.rollback();
	        // 获取数据 每次获取一百条改变数据
	    	
	    	 while (emptyCount < emptyActionCount) {//120*1秒还没有数据就断开
	            	
                Message message = connector.getWithoutAck(CanelConfiguration.size); // 获取指定数量的数据
                long batchId = message.getId();
                int size = message.getEntries().size();
                if (batchId == -1 || size == 0) {
                	emptyCount++;
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                } else {
                	emptyCount++;
                	BosConstants.debug("-------------------CanalClient analysis  size="+size);
                    analysis(message.getEntries());
                }
                
                
                connector.ack(batchId); // 提交确认
            }
			
			
	        return BosConstants.RET_SUCCESS;
		}catch(Exception e) {
			
			e.printStackTrace();
			
			return e.getMessage();
			
		}finally {
			connector.disconnect();
		}
       
	}
	
	
	
	private void analysis(List<CanalEntry.Entry> entries) throws Exception {
		
		
		
		HashMap<String,PRoot>  rootHash=new HashMap<String,PRoot>();
		HashMap<String,Table>  tableHash=new HashMap<String,Table>();
		
		
		ServiceInf service=SpringContextUtil.getBean(BosService.class);
		
		
		try {
	
	        for (CanalEntry.Entry entry : entries) {
	            // 只解析mysql事务的操作，其他的不解析
	            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONBEGIN) {
	                continue;
	            }
	            if (entry.getEntryType() == CanalEntry.EntryType.TRANSACTIONEND) {
	                continue;
	            }
	            // 解析binlog
	            CanalEntry.RowChange rowChange = null;
	            try {
	                rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
	            } catch (Exception e) {
	                throw new RuntimeException("解析出现异常 data:" + entry.toString(), e);
	            }
	            if (rowChange != null) {
	                // 获取操作类型
	                CanalEntry.EventType eventType = rowChange.getEventType();
	                // 获取当前操作所属的数据库
	                String dbName = entry.getHeader().getSchemaName();
	                // 获取当前操作所属的表
	                String tableName = entry.getHeader().getTableName();
	                // 事务提交时间
	                long timestamp = entry.getHeader().getExecuteTime();
	                
	                
	                
	                PRoot root=rootHash.get(dbName);
	                if(root==null) {
	                	root=service.getMgClient().getById(dbName, PRoot.class);
	                	rootHash.put(dbName, root);
	                }
	                if(root==null) {
	                	System.out.println("db:"+dbName+" is exist");
	                	continue;
	                }
	                
	                Table table=tableHash.get(root.getCode()+"_"+tableName.toUpperCase());
	                if(table==null) {
	                	table=new Table();
	                	table.setCode(tableName.toUpperCase());
	                	table.setSys(root.getCode());
	                	table=service.getMgClient().get(table,service);
	                	tableHash.put(root.getCode()+"_"+tableName.toUpperCase(), table);
	                }
	            	if(table==null) {
	            		System.out.println("tableName:"+tableName+" is exist");
	            		continue;
	            	}
	                
	                
	                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
	                	BosConstants.debug("-------------CanalClient  "+root.getCode()+"    "+table.getCode()+"     "+eventType+"      "+timestamp);
	                    dataDetails(rowData.getBeforeColumnsList(), rowData.getAfterColumnsList(), root, table,service, eventType, timestamp);
	                }
	            }
	        }
	        
		}catch(Exception e) {
			e.printStackTrace();
		}
	        
		
    }

	
	
	
	
	/**
	 * 解析具体一条Binlog消息的数据
	 * @param beforeColumns
	 * @param afterColumns
	 * @param root  当前操作所属数据库
	 * @param table  当前操作所属表
	 * @param service
	 * @param eventType  当前操作类型（新增、修改、删除）
	 * @param timestamp
	 * @throws Exception
	 */
    private void dataDetails(List<CanalEntry.Column> beforeColumns,
                                    List<CanalEntry.Column> afterColumns,
                                    PRoot root,
                                    Table table,
                                    ServiceInf service,
                                    CanalEntry.EventType eventType,
                                    long timestamp) throws Exception{
    	
    	//可以改造为多线程处理
    	
    	BosEntity obj=new TableJson(table.getClassName());
    	
        if (CanalEntry.EventType.INSERT.equals(eventType)) {
        	
        	
        	
        	JSONObject json=getInfo(afterColumns,table.getClassName(),obj,service);
        	
        	
        	/**DBLog log=new DBLog();
        	log.setDbAction(1L);
        	log.setSys(root.getCode());
        	log.setClassName(table.getClassName());
        	log.setInvokeTime(new Date(timestamp));
        	log.setInfo(json.toJSONString());
        	log.setInvokeUserId(json.getString("createUserId"));
        	log.setIdValue(json.getString("pk"));
        	log.setId(log.getSys()+"_"+table.getSimpleName()+"_"+log.getIdValue()+"_"+eventType+"_"+timestamp);
        	service.save(log,null);*/
        	
        	
        	List<Field> enList=obj.showTable().listEncryptionField();
        	for(Field x:enList) {
        		obj.addEncryption(x.getProperty());
        	}
        	
        	
        	if(true) {
        		
        		
        		//JSONObject before=getInfo(beforeColumns,table.getClassName(),null);
        		
        		Vector<String> fv=Field.split(obj.showTable().getTrackFields());
        		for(String f:fv) {
        			Field xf=obj.showTable().getDBField(f);
        			if(xf==null) continue;
        			
        			String v2=DataChange.replaceNull(json.getString(xf.getProperty()));
        			//更改留痕
        			BizFieldModifyTrack track=new BizFieldModifyTrack();
        			track.setClassName(table.getClassName());
        			track.setTrackId(BizFieldModifyTrack.next());
        			track.setIdValue(obj.getPk());
        			track.setProperty(xf.getProperty());
        			track.setPropertyValue(v2);
        			track.setChangeDate(new Date(timestamp));
        			track.setActionType(2L);
        			track.setChangeUserId(json.getString("createUserId"));
        			service.save(track, null);
        		}
        		
        		

        	}
        	
        	
        	if(DataChange.getLongValueWithDefault(table.getSynEs(), 0)==1) {
        		service.getEsClient().syn(obj, service);
        	}
        	
        	if(DataChange.getIntValueWithDefault(table.getRedisEntity(), 1)==1) {
        		service.getRdClient().putEntityDisplay(obj,service);
        	}
        	
        	
        	//是否需要同步
        	SynTableDataConfig config=service.getById(obj.getEntityClassName(), SynTableDataConfig.class);
			if(config!=null &&  DataChange.getIntValueWithDefault(config.getStatus(), 0)==1) {
				
				BosEntity com=service.getById(obj.getPk(), obj.getEntityClassName());
				if(com!=null) {
					SynTableDataItem item=new SynTableDataItem();
					item.setClassName(obj.getEntityClassName());
					QueryListInfo<SynTableDataItem> list=service.getList(item, null);
					config.setItemList(list.getList());
					config.synData(com, service, null);
				}
			}
        	
        	
        	//synToMongoForUMO(obj,service);
        	
            //System.out.println("新增数据：");
        } else if (CanalEntry.EventType.DELETE.equals(eventType)) {
            //System.out.println("删除数据：");
        	
        	JSONObject json=getInfo(beforeColumns,table.getClassName(),obj,service);
        	
        	
            /**DBLog log=new DBLog();
        	log.setDbAction(3L);
        	log.setSys(root.getCode());
        	log.setClassName(table.getClassName());
        	log.setInvokeTime(new Date(timestamp));
        	log.setInfo(json.toJSONString());
        	log.setIdValue(json.getString("pk"));
        	log.setInvokeUserId(json.getString("updateUserId"));
        	log.setId(log.getSys()+"_"+table.getSimpleName()+"_"+log.getIdValue()+"_"+eventType+"_"+timestamp);
        	service.save(log,null);*/
        	
        	BosConstants.debug("canal id="+obj.getPk()+"  pk="+json.getString("pk"));
        	
        	if(DataChange.getLongValueWithDefault(table.getSynEs(), 0)==1) {
        		service.getEsClient().deleteById(json.getString("pk"), table.getClassName());
        	}
        	
        	
        	
        	SynTableDataConfig config=service.getById(obj.getEntityClassName(), SynTableDataConfig.class);
			if(config!=null &&  DataChange.getIntValueWithDefault(config.getStatus(), 0)==1) {
				SynTableDataItem item=new SynTableDataItem();
				item.setClassName(obj.getEntityClassName());
				QueryListInfo<SynTableDataItem> list=service.getList(item, null);
				config.setItemList(list.getList());
				config.delData(obj, service, null,true);
			}
        	
        	//delToMongo(json.getString("pk"),table.getClassName() ,service);
        	
        } else {//修改
        	
        	JSONObject json=getInfo(afterColumns,table.getClassName(),obj,service);
        	
            
            /**DBLog log=new DBLog();
        	log.setDbAction(2L);
        	log.setSys(root.getCode());
        	log.setClassName(table.getClassName());
        	log.setInvokeTime(new Date(timestamp));
        	
        	log.setInfo(json.toJSONString());
        	log.setInvokeUserId(json.getString("updateUserId"));
        	log.setIdValue(json.getString("pk"));*/
        	
        	
        	//如果是如删除
        	if(table.softDelete()) {
        		Long isValid=DataChange.StringToLong(json.getString("isValid"));
        		if(isValid!=null && isValid.intValue()==0) {
        			
        			//log.setDbAction(4L);
        			//彻底删除数据
        			if(BosConstants.canalDeleteComplete) {
        				service.deleteById(json.getString("pk"), table.getClassName(), null, BosConstants.canalDeleteComplete);
        			}else {
        				try {
        					if (DataChange.getLongValueWithDefault(table.getSynEs(), 0) == 1) {
        						service.getEsClient().deleteById(json.getString("pk"), table.getClassName());
        					}
        				} catch (Exception e) {
        					e.printStackTrace();
        				}
        			}
        			
        			obj.setIsValid(0);
        		}
        	}
        	//log.setId(log.getSys()+"_"+table.getSimpleName()+"_"+log.getIdValue()+"_"+eventType+"_"+timestamp);
        	//service.save(log,null);
        	
        	
        	JSONObject before=getInfo(beforeColumns,table.getClassName(),null,service);
        	
        	//操作敏感更新字段
        	Vector<String> fv=Field.split(table.getTrackFields());
        	for(String x:fv) {
        		String v1=DataChange.replaceNull(before.getString(x));
        		String v2=DataChange.replaceNull(json.getString(x));
        		if(!v1.equals(v2)) {
        			//更改留痕
        			BizFieldModifyTrack track=new BizFieldModifyTrack();
        			track.setTrackId(BizFieldModifyTrack.next());
        			track.setClassName(table.getClassName());
        			track.setIdValue(obj.getPk());
        			track.setProperty(x);
        			track.setPropertyValue(v2);
        			track.setChangeDate(new Date(timestamp));
        			track.setActionType(2L);
        			track.setChangeUserId(json.getString("updateUserId"));
        			service.save(track, null);
        		}
        	}
        	
        	
        	List<Field> enList=obj.showTable().listEncryptionField();
        	for(Field x:enList) {
        		obj.addEncryption(x.getProperty());
        	}
        	
        	
        	if(DataChange.getLongValueWithDefault(table.getSynEs(), 0)==1) {
        		service.getEsClient().syn(obj, service);
        	}
        	
        	if(DataChange.getIntValueWithDefault(table.getRedisEntity(), 1)==1) {
        		service.getRdClient().putEntityDisplay(obj,service);
        	}
        	
        	
        	SynTableDataConfig config=service.getById(obj.getEntityClassName(), SynTableDataConfig.class);
			if(config!=null &&  DataChange.getIntValueWithDefault(config.getStatus(), 0)==1) {
				BosEntity com=service.getById(obj.getPk(), obj.getEntityClassName());
				if(com!=null ) {
					SynTableDataItem item=new SynTableDataItem();
					item.setClassName(obj.getEntityClassName());
					QueryListInfo<SynTableDataItem> list=service.getList(item, null);
					config.setItemList(list.getList());
					if(!com.showTable().softDelete()  ||  DataChange.getIntValueWithDefault(com.getIsValid(), 0)==1) {
						config.synData(com, service, null);
					}else if(com.showTable().softDelete()  &&  DataChange.getIntValueWithDefault(com.getIsValid(), 0)==0) {
						config.delData(com, service, null,false);
					}
				}
			}
        	
        	//synToMongoForUMO(obj,service);
        	
        }
        
        
        
        new ClassInnerNotice().invoke(ListData.class.getSimpleName(),table.getClassName());
       // System.out.println("操作时间：" + timestamp);
    }

    
    
    private JSONObject getInfo(List<CanalEntry.Column> columns,String className,BosEntity obj,ServiceInf service) throws Exception {
    	
    	JSONObject data=new JSONObject(true);
        for (CanalEntry.Column column : columns) {
        	Field f=obj.showTable().getDBField(CheckUtil.getJavaNameByDB(column.getName()));
        	//f.setClassName(className);
        	//f.setProperty(CheckUtil.getJavaNameByDB(column.getName()));
        	//f.setPropertyType(1L);
        	//f=service.getMgClient().getById(f.getPk(),Field.class);
        	String value=column.getValue() ;
        	
        	if(f!=null &&  !DataChange.isEmpty(value)) {
        		if(obj!=null) obj.putValue(f, value);
        		if(DataChange.getLongValueWithDefault(f.getIsKey(), 0)==1) {
        			data.put("pk", value);
        		}
        		data.put(f.getProperty(), value);
        	}
        }
    	
    	return data;
    }
	
    
    
    
    /**public void synToMongoForUMO(BosEntity obj,ServiceInf service) {
    	
    	try {
    		
    		if(obj.getEntityClassName().equals(BosConstants.userClassName)  &&  !BosConstants.userClassName.equals(PUser.class.getName())) {
    			BosConstants.debug("syn to mongo PUser:"+obj.getPk());
    			PUser entity=new PUser();
    			entity.setUserId(obj.getPk());
    			if(BosConstants.userClassName_config!=null) {
    				Iterator<String> its=BosConstants.userClassName_config.keySet().iterator();
    				while(its.hasNext()){
    					String k=its.next();
    					entity.putMustValue(k, obj.propertyValueString(BosConstants.userClassName_config.getString(k)));
    				}
    			}
    			entity.setUpdateUserId(obj.getUpdateUserId());
    			entity.setUpdateTime(obj.getUpdateTime());
    			entity.setCreateTime(obj.getCreateTime());
    			entity.setCreateUserId(obj.getCreateUserId());
    			entity.setIsValid(obj.getIsValid());
    			if(!DataChange.isEmpty(entity.getUserId())) service.save(entity, null);
    			
    		}else if(obj.getEntityClassName().equals(BosConstants.userOrgClassName) &&  !BosConstants.userOrgClassName.equals(PUserMember.class.getName())) {
    			BosConstants.debug("syn to mongo PUserMember:"+obj.getPk());
    			PUserMember entity=new PUserMember();
    			if(BosConstants.userOrgClassName_config!=null) {
    				Iterator<String> its=BosConstants.userOrgClassName_config.keySet().iterator();
    				while(its.hasNext()){
    					String k=its.next();
    					entity.putMustValue(k, obj.propertyValueString(BosConstants.userOrgClassName_config.getString(k)));
    				}
    			}
    			entity.setUpdateUserId(obj.getUpdateUserId());
    			entity.setUpdateTime(obj.getUpdateTime());
    			entity.setCreateTime(obj.getCreateTime());
    			entity.setCreateUserId(obj.getCreateUserId());
    			entity.setIsValid(obj.getIsValid());
    			if(!DataChange.isEmpty(entity.getPk())) service.save(entity, null);
    		}else if(obj.getEntityClassName().equals(BosConstants.memberClassName) &&  !BosConstants.memberClassName.equals(PMember.class.getName()) ) {
    			BosConstants.debug("syn to mongo PMember:"+obj.getPk());
    			PMember entity=new PMember();
    			entity.setMemberId(obj.getPk());
    			if(BosConstants.memberClassName_config!=null) {
    				Iterator<String> its=BosConstants.memberClassName_config.keySet().iterator();
    				while(its.hasNext()){
    					String k=its.next();
    					entity.putMustValue(k, obj.propertyValueString(BosConstants.memberClassName_config.getString(k)));
    				}
    			}
    			entity.setUpdateUserId(obj.getUpdateUserId());
    			entity.setUpdateTime(obj.getUpdateTime());
    			entity.setCreateTime(obj.getCreateTime());
    			entity.setCreateUserId(obj.getCreateUserId());
    			entity.setIsValid(obj.getIsValid());
    			if(!DataChange.isEmpty(entity.getMemberId())) service.save(entity, null);
    		}else if(obj.getEntityClassName().equals(BosConstants.placeClassName) &&  !BosConstants.placeClassName.equals(PPlaceInfo.class.getName()) ) {
    			BosConstants.debug("syn to mongo PPlaceInfo:"+obj.getPk());
    			PPlaceInfo entity=new PPlaceInfo();
    			entity.setPlaceId(obj.getPk());
    			if(BosConstants.placeClassName_config!=null) {
    				Iterator<String> its=BosConstants.placeClassName_config.keySet().iterator();
    				while(its.hasNext()){
    					String k=its.next();
    					entity.putMustValue(k, obj.propertyValueString(BosConstants.placeClassName_config.getString(k)));
    				}
    			}
    			if(!DataChange.isEmpty(entity.getParentId())) {
    				entity.setParentId("0");
    			}
    			entity.setUpdateUserId(obj.getUpdateUserId());
    			entity.setUpdateTime(obj.getUpdateTime());
    			entity.setCreateTime(obj.getCreateTime());
    			entity.setCreateUserId(obj.getCreateUserId());
    			entity.setIsValid(obj.getIsValid());
    			if(!DataChange.isEmpty(entity.getPlaceId())) service.save(entity, null);
    		}else if(obj.getEntityClassName().equals(BosConstants.orgClassName) &&  !BosConstants.orgClassName.equals(POrg.class.getName()) ) {
    			BosConstants.debug("syn to mongo POrg:"+obj.getPk());
    			POrg entity=new POrg();
    			entity.setOrgId(obj.getPk());
    			if(BosConstants.orgClassName_config!=null) {
    				Iterator<String> its=BosConstants.orgClassName_config.keySet().iterator();
    				while(its.hasNext()){
    					String k=its.next();
    					entity.putMustValue(k, obj.propertyValueString(BosConstants.orgClassName_config.getString(k)));
    				}
    			}
    			entity.setUpdateUserId(obj.getUpdateUserId());
    			entity.setUpdateTime(obj.getUpdateTime());
    			entity.setCreateTime(obj.getCreateTime());
    			entity.setCreateUserId(obj.getCreateUserId());
    			entity.setIsValid(obj.getIsValid());
    			if(!DataChange.isEmpty(entity.getOrgId())) service.save(entity, null);
    		}
    		

    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    }
    
    
    private void delToMongo(String id,String className,ServiceInf service) {
    	
    	try {
    		
    		if(className.equals(BosConstants.userClassName)  &&  !BosConstants.userClassName.equals(PUser.class.getName())) {
    			service.deleteById(id, PUser.class.getName(), null, true);
    		}else if(className.equals(BosConstants.userOrgClassName) &&  !BosConstants.userOrgClassName.equals(PUserMember.class.getName())) {
    			service.deleteById(id, PUserMember.class.getName(), null, true);
    		}else if(className.equals(BosConstants.memberClassName) &&  !BosConstants.memberClassName.equals(PMember.class.getName()) ) {
    			service.deleteById(id, PMember.class.getName(), null, true);
    		}else if(className.equals(BosConstants.placeClassName) &&  !BosConstants.placeClassName.equals(PPlaceInfo.class.getName()) ) {
    			service.deleteById(id, PPlaceInfo.class.getName(), null, true);
    		}else if(className.equals(BosConstants.orgClassName) &&  !BosConstants.orgClassName.equals(POrg.class.getName()) ) {
    			service.deleteById(id, POrg.class.getName(), null, true);
    		}
    		

    	}catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    }*/
    
    
    public static void main(String[] args) {
    	
    }

}
