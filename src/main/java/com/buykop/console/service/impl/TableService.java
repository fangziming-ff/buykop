package com.buykop.console.service.impl;

import java.util.List;
import java.util.Vector;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.buykop.framework.elasticsearch.EsClient;
import com.buykop.framework.hbase.HBaseClient;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.mysql.BaseDao;
import com.buykop.framework.mysql.Import;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.Index;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.ClientPool;
import com.buykop.framework.util.SpringContextUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseService;


@Service
@Component
public class TableService extends BaseService implements com.buykop.console.service.TableService{
	
	
	private Throwable cause;
	
	
	@Autowired
	private AmqpTemplate rabbitTemplate;

	public AmqpTemplate getRabbitTemplate() {
		return rabbitTemplate;
	}
	/**
	 * 同步生效,  持久化并注入redis
	 * @param className
	 * @throws Exception
	 */
	public void syn(String className,String memberId,String userId) throws Exception{
		
		if(DataChange.isEmpty(className)) throw new Exception("入参:className为空");
		
		Table obj=Table.initByMongo(className,this);
		if(obj==null) throw new Exception("表对象为空");
		
		BosConstants.debug("------"+className+"--code="+obj.getCode()+"----------存储方式:"+obj.getCache()+"---------");
		
		
		//BaseDao dao=ClientPool.getConnection();
		
	
		if(obj.getCache().longValue()==0) {
			Import importDB=new Import(this.getBaseDao());
			importDB.syn(obj);
		}
		
		
		
		obj.setRefreshTime(NetWorkTime.getCurrentDatetime());
		BosConstants.tableHash.put(obj.getClassName(),obj);
		
		
		if(obj.getCache().longValue()==0) {
			Table.createBaseSQLFunction(className, false,userId,this);
		}
		
		if(obj.getCache().intValue()==5 ||  DataChange.getLongValueWithDefault(obj.getSynEs(), 0)==1) {//es需要建立索引
			if(!this.getEsClient().indexExists(obj)) {
				this.getEsClient().indexSyn(obj,this);
			}
		}
		
		
		if(obj.getCache().intValue()==8) {//es需要建立索引
			this.getHBaseClient().synTable(className);
		}
	}
	
	
	
	
	
	/**
	 * 删除某个表
	 * @param className
	 * @throws Exception
	 */
	public void delete(String className) throws Exception{
		
		//如果数据库mongo已经持久化,则必须删除所有数据后才能删除表结构
		if(DataChange.isEmpty(className)) throw new Exception("入参:className为空");
		Table obj=this.getMgClient().getTableById(className);
		
		if(obj!=null) {
			
			if(obj.getCache().longValue()==2) {
				if(this.getMgClient().exist(className)) {
					long num=this.getMgClient().getCount(obj,this);
					if(num>0) {
						throw new Exception("数据表已创建,请先删除所有数据");
					}
					this.getMgClient().removeTable(obj,this);
				}
				 
				
				
				
			}else if(obj.getCache().longValue()==0) {
				
				Import importDB=new Import(this.getBaseDao());
				if(importDB.exist(obj.getSys(), obj.getCode())) {
					long num=this.getBaseDao().getAllCount(obj);
					if(num>0) {
						throw new Exception("数据表已创建,请先删除所有数据");
					}
					importDB.dropTable(obj);
				}
				
			}
		}
		
		
		this.getMgClient().deleteByPK(className, Table.class,null,this);
		BosConstants.removeTable(className);
		
		
		
		Field field=new Field();
		field.setClassName(className);
		this.getMgClient().delete(field,null,this);
		
		Index index=new Index();
		index.setClassName(className);
		this.getMgClient().delete(index,null,this);
		
		
	}
	
	
	public Throwable getCause() {
        return cause;
    }

    public void setCause(Throwable cause) {
        this.cause = cause;
    }
	
	
	

}
