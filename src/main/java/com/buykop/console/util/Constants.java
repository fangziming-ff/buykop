package com.buykop.console.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.buykop.console.util.Constants;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.JsonUtil;

public class Constants {
	
	public static final String current_sys =  "console";
	public static final String current_sys_name = "控制中心";
	
	public static final String service_name="service-console";

	public static final String LIST_ALL_OIS = "LIST_ALL_OIS";
	
	
	
	
	
	
	
	static {
		BosConstants._sysV.add(Constants.current_sys);
		BosConstants._sysV.add(BosConstants.current_sys);
	}
	
	
	
	public static void main(String[] args) {
		
		String value="[\"good1\",\"good2\",\"good3\"]";
		
		JSONArray arr= JsonUtil.string2JsonArray(value);
		
		
		System.out.println();
		
		
	}
}
