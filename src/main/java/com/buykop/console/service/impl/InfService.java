package com.buykop.console.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.framework.entity.BizViewTrack;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.DiyInfBatch;
import com.buykop.framework.entity.DiyInfBatchItem;
import com.buykop.framework.entity.DiyInfField;
import com.buykop.framework.entity.DiyInfOutField;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.MQProcessing;
import com.buykop.framework.scan.Statement;
import com.buykop.framework.scan.Table;
import com.buykop.framework.thread.SynToEs2;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.Calculation;
import com.buykop.framework.util.JSUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.query.BaseQuery;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BaseService;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.TableJson;

@Service
@Component
public class InfService extends BaseService implements com.buykop.console.service.InfService {

	private static Logger logger = LoggerFactory.getLogger(InfService.class);

	@Autowired
	private AmqpTemplate rabbitTemplate;

	public AmqpTemplate getRabbitTemplate() {
		return rabbitTemplate;
	}
}
