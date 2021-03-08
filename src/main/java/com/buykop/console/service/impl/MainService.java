package com.buykop.console.service.impl;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.entity.PMember;
import com.buykop.console.entity.POrg;
import com.buykop.console.entity.PPlaceInfo;
import com.buykop.console.entity.PUser;
import com.buykop.console.entity.product.ProductMenuDiy;
import com.buykop.console.util.Constants;
import com.buykop.framework.annotation.util.DataCheck;
import com.buykop.framework.entity.DiyInf;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.mysql.Import;
import com.buykop.framework.mysql.TableStructure;
import com.buykop.framework.oauth2.PMemberType;
import com.buykop.framework.oauth2.PRMemberType;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.DataPer;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PForm;
import com.buykop.framework.scan.InputCheck;
import com.buykop.framework.scan.LableLanDisplay;
import com.buykop.framework.scan.PLanguage;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.PTreeForm;
import com.buykop.framework.scan.TreeSelectForm;
import com.buykop.framework.scan.Statement;
import com.buykop.framework.scan.Table;
import com.buykop.framework.scan.Timezone;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.type.BaseService;
import com.buykop.framework.util.type.QueryListInfo;

@Service
@Component
public class MainService extends BaseService implements com.buykop.console.service.MainService {

	
	@Autowired
	private AmqpTemplate rabbitTemplate;

	public AmqpTemplate getRabbitTemplate() {
		return rabbitTemplate;
	}
	
	
	public void init() throws Exception {
		
		
		super.sysInit(Constants.current_sys);
		
		
		
		UserToken ut=new UserToken();
		
		
		PMemberType type = this.getMgClient().getById(BosConstants.memberType, PMemberType.class);
		if (type == null) {
			type = new PMemberType();
			type.setTypeId(BosConstants.memberType);
			type.setFeeType(0L);
		}
		type.setTypeName("机构会员");
		type.setSys(Constants.current_sys);
		if (type.getStatus() == null) {
			type.setStatus(1L);
		}
		this.save(type,ut);

		PRMemberType rmt = new PRMemberType();
		rmt.setMemberId("1");
		rmt.setTypeId(BosConstants.memberType);
		rmt.setStatus(2L);
		this.save(rmt, ut);

		type = this.getMgClient().getById(BosConstants.memberType_admin, PMemberType.class);
		if (type == null) {
			type = new PMemberType();
			type.setTypeId(BosConstants.memberType_admin);
			type.setFeeType(0L);
		}
		type.setTypeName("平台运营方");
		type.setSys(Constants.current_sys);
		if (type.getStatus() == null) {
			type.setStatus(1L);
		}
		this.save(type, ut);

		rmt = new PRMemberType();
		rmt.setMemberId("1");
		rmt.setTypeId(BosConstants.memberType_admin);
		rmt.setStatus(2L);
		this.save(rmt, ut);

		PRoot root = this.getMgClient().getById(BosConstants.current_sys, PRoot.class);
		root.setVersion(BosConstants.sysVersionHash.get(BosConstants.current_sys.toUpperCase()));
		root.setStatus(1L);
		this.save(root, ut);

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_ALL_SYS, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_ALL_SYS);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("所有系统列表");
			}
			obj.setOrderBy("displayName");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(PRoot.class.getName());
			obj.setInitScript("");
			obj.addToMust("initScript");
			obj.setRegType(0L);
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_MANUAL_SYS, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_MANUAL_SYS);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("人工注册系统列表");
			}
			obj.setOrderBy("displayName");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(PRoot.class.getName());
			obj.setInitScript("regType=${1}");
			obj.setRegType(0L);
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}

		if (true) {

			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_FORM_DB_BY_CLASSNAME, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_FORM_DB_BY_CLASSNAME);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("某个类对象的基础数据维护表单");
			}
			// obj.setMapId("getListForMember");
			obj.setOrderBy("formName");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(PForm.class.getName());
			obj.setInitScript("formType=${1} status=${1}");
			obj.setRegType(0L);
			obj.setParentField("className");
			obj.setValueFormula("${formName}[${memberId}]");
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}
		
		
		
		
		
		
		
		

		

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_CLASS_BY_SYS, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_CLASS_BY_SYS);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("系统的所有类对象列表");
			}

			obj.setInitScript("");
			obj.addToMust("initScript");
			obj.setRegType(0L);
			obj.setOrderBy("className");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(Table.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			obj.setParentField("sys");
			obj.setValueFormula("${displayName}[${className} ${cache}]");
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_DB_PROPERTY_BY_CLASS, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_DB_PROPERTY_BY_CLASS);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("类对象存储属性列表");
			}
			obj.addToMust("initScript");
			obj.setInitScript("propertyType=${1}");
			obj.setRegType(0L);
			obj.setOrderBy("property");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(Field.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			obj.setParentField("className");
			obj.setKeyField("${property}");
			obj.setValueFormula("${display}[${property} ${propertyType}]");
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_PROPERTY_BY_CLASS, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_PROPERTY_BY_CLASS);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("类对象存储属性列表");
			}
			// obj.setInitScript("propertyType=${1}");
			obj.addToMust("initScript");
			obj.setRegType(0L);
			obj.setOrderBy("property");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(Field.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			obj.setParentField("className");
			obj.setKeyField("${property}");
			obj.setValueFormula("${display}[${property} ${propertyType}]");
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_ALL_TIMEZONE, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_ALL_TIMEZONE);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("所有时区");
			}

			obj.setInitScript("");
			obj.addToMust("");
			// obj.setRegType(0L);
			obj.setOrderBy("seq");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(Timezone.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			// obj.setParentField("");
			// obj.setValueFormula("");
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_CHARTCLASS_BY_SYS, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_CHARTCLASS_BY_SYS);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("系统的图形类对象列表");
			}

			obj.setInitScript("isChart=${1} cache=${0}");
			obj.addToMust("initScript");
			obj.setRegType(0L);
			obj.setOrderBy("className");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(Table.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			obj.setParentField("sys");
			obj.setValueFormula("${displayName}[${className}]");
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_MAPID_BY_CLASS, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_MAPID_BY_CLASS);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("类对象的SQLMAP列表");
			}

			obj.setKeyField("id");
			// obj.setInitScript("");
			obj.addToMust("initScript");
			obj.setRegType(0L);
			obj.setOrderBy("id");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(Statement.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			obj.setParentField("className");
			obj.setValueFormula("${note}[${id}]");
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_ROLE_BY_SYS, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_ROLE_BY_SYS);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("系统的角色列表");
			}

			// obj.setKeyField("id");
			// obj.setInitScript("");
			obj.addToMust("initScript");
			obj.setRegType(0L);
			obj.setOrderBy("roleName");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(PRole.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			obj.setParentField("sys");
			// obj.setValueFormula("${note}[${id}]");
			this.save(obj, ut);
		}
		
		
		
		
		

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_TREEFORM_BY_CLASS, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_TREEFORM_BY_CLASS);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("类对象的通用绑定列表");
			}

			obj.setLazyLoad(0L);
			// obj.setKeyField("code");
			obj.addToMust("initScript");
			obj.setInitScript("status=${1}");
			obj.setRegType(0L);
			obj.setOrderBy("name");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(PTreeForm.class.getName());
			obj.setStatus(1L);
			obj.setParentField("className");
			this.save(obj, ut);
		}

		PRole role = new PRole();
		role.setRoleId(BosConstants.role_memberAdmin);
		role.setRoleType(1L);
		role.setRoleName("机构管理员");
		role.setStatus(1L);
		role.setTypeId(BosConstants.memberType);
		role.setSys(Constants.current_sys);
		role.setIsValid(1);
		this.save(role, ut);

		role = new PRole();
		role.setRoleId(BosConstants.role_depAdmin);
		role.setRoleType(1L);
		role.setRoleName("部门管理员");
		role.setStatus(1L);
		role.setTypeId(BosConstants.memberType);
		role.setSys(Constants.current_sys);
		role.setIsValid(1);
		this.save(role, ut);

		role = new PRole();
		role.setRoleId(BosConstants.role_sysAdmin);
		role.setRoleType(1L);
		role.setRoleName("系统管理员");
		role.setStatus(1L);
		role.setTypeId(BosConstants.memberType_admin);
		role.setSys(Constants.current_sys);
		role.setIsValid(1);
		this.save(role, ut);
		
		
		role = new PRole();
		role.setRoleId(BosConstants.role_tech_manager);
		role.setRoleType(1L);
		role.setRoleName("技术开发负责人");
		role.setStatus(1L);
		role.setTypeId(BosConstants.memberType_admin);
		role.setSys(Constants.current_sys);
		role.setIsValid(1);
		this.save(role, ut);

		role = new PRole();
		role.setRoleId(BosConstants.role_db_manager);
		role.setRoleType(1L);
		role.setRoleName("数据库设计/开发");
		role.setStatus(1L);
		role.setTypeId(BosConstants.memberType_admin);
		role.setSys(Constants.current_sys);
		role.setIsValid(1);
		this.save(role, ut);
		
		role = new PRole();
		role.setRoleId(BosConstants.role_tech);
		role.setRoleType(1L);
		role.setRoleName("技术开发");
		role.setStatus(1L);
		role.setTypeId(BosConstants.memberType_admin);
		role.setSys(Constants.current_sys);
		role.setIsValid(1);
		this.save(role, ut);

		role = new PRole();
		role.setRoleId(BosConstants.role_product_manager);
		role.setRoleType(1L);
		role.setRoleName("产品经理");
		role.setStatus(1L);
		role.setTypeId(BosConstants.memberType_admin);
		role.setSys(Constants.current_sys);
		role.setIsValid(1);
		this.save(role, ut);
		
		
		role = new PRole();
		role.setRoleId(BosConstants.role_testing);
		role.setRoleType(1L);
		role.setRoleName("测试");
		role.setStatus(1L);
		role.setTypeId(BosConstants.memberType_admin);
		role.setSys(Constants.current_sys);
		role.setIsValid(1);
		this.save(role, ut);

		role = new PRole();
		role.setRoleId(BosConstants.role_oisAdmin);
		role.setRoleType(1L);
		role.setRoleName("运营人员");
		role.setStatus(1L);
		role.setTypeId(BosConstants.memberType_admin);
		role.setSys(Constants.current_sys);
		role.setIsValid(1);
		this.save(role,ut);

		try {
			// List<BosEntity> list=new ArrayList<BosEntity>();

			InputCheck check=this.getById("string", InputCheck.class);
			if(check==null) {
				check=new InputCheck("string", "纯字符", DataCheck.string);
				this.save(check, ut);
			}
			BosConstants.getExpireHash().putObj(InputCheck.class.getName(), check.getCheckId(), check,24*3600);
			
			check=this.getById("mobile", InputCheck.class);
			if(check==null) {
				check=new InputCheck("mobile", "手机", DataCheck.mobile);
				this.save(check, ut);
			}
			BosConstants.getExpireHash().putObj(InputCheck.class.getName(), check.getCheckId(), check,24*3600);
			
			check=this.getById("mail", InputCheck.class);
			if(check==null) {
				check=new InputCheck("mail", "邮件", DataCheck.mail);
				this.save(check, ut);
			}
			BosConstants.getExpireHash().putObj(InputCheck.class.getName(), check.getCheckId(), check,24*3600);
			
			check=this.getById("idcardnum", InputCheck.class);
			if(check==null) {
				check=new InputCheck("idcardnum", "身份证", DataCheck.idCardNum);
				this.save(check, ut);
			}
			BosConstants.getExpireHash().putObj(InputCheck.class.getName(), check.getCheckId(), check,24*3600);
			
			check=this.getById("loginname", InputCheck.class);
			if(check==null) {
				check=new InputCheck("loginname", "登录名", DataCheck.loginName);
				this.save(check, ut);
			}
			BosConstants.getExpireHash().putObj(InputCheck.class.getName(), check.getCheckId(), check,24*3600);
			
			check=this.getById("password", InputCheck.class);
			if(check==null) {
				check=new InputCheck("password", "密码", DataCheck.passwd);
				this.save(check, ut);
			}
			BosConstants.getExpireHash().putObj(InputCheck.class.getName(), check.getCheckId(), check,24*3600);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (true) {

			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramScanDB, "扫描业务数据库", "db", null, null,
					this);

			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramMapTJClassName, "地图统计类对象", "mapTJ", null,
					null, this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramMapTJClassNameListFields, "地图统计类对象属性集",
					"mapTJ", null, null, this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramMapTJMethod, "地图统计月/年统计方式", "mapTJ", null,
					"213", this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramMapTJGroupCache, "指标分组统计缓存", "mapTJ", "0",
					"3", this);

			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramChartXNum, "图表X的最大数量", "chart", "31",
					null, this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramChartDimensionaNum, "图表维度最大数量", "chart",
					"6", null, this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.param_current_cityId, "当前地区", null, "", null,
					this);
			
			
			
			// PSysParam.create(Constants.sys_console, "1",
			// Constants.param_db_query_cache_seconds, "数据库查询结果缓存时间(秒)", null, "30",null);
			PSysParam.create(BosConstants.current_sys, "1", "innerIp", "内网ip规则", null, "192.168.*.*", null, this);
			PSysParam.create(BosConstants.current_sys, "1", "storeTpye", "文件存储方式", null, "1", "47", this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramLoginFaildNum, "允许登录失败的次数", "login", "3",null, this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramStandaloneLoginPwdForMember, "机构用户密码独立验证", "login", "0", "3", this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramStandaloneLoginPwdForPerson, "机构所属个人用户密码独立验证", "login", "0", "3",this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramAutoUnlockingLogin, "登录失败自动解锁时间(分钟)",
					"login", "10", null, this);
			// PSysParam.create(Constants.sys_console,"1", Constants.paramDefaultDomain,
			// "默认的域名",null,"",null);
			// PSysParam.create(Constants.sys_console,"1",
			// Constants.paramDefaultLevelForTree, "默认的树形展开层级",null,"2",null);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramIndexPage, "首页地址", null, "/index.html",
					null, this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramLogExpires, "服务日志保存天数", "log", "60", null,
					this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramSM, "短信支持", null, "0", "3", this);// 0:不支持
																												// 1:阿里云
																												// 2:其他
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramUserRegister, "用户注册", "user", "0", "3",
					this);// 0:不支持
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramPUserUnique, "用户唯一性", "user", "1", "3",
					this);// 0:不支持
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramTokenTimeForMember, "机构用户token过期时间(分钟)",
					"token", "120", "", this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramTokenTimeForUser, "个人用户token过期时间(分钟)",
					"token", "30", "", this);

			// 1:不支持
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramLoginLogType, "每日登录日志方式", "login", "0",
					"129", this);//

			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramFileServerIp, "文件服务器sftp内网Ip",
					"fileServer", "", null, this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramFileServerDomain, "文件服务器域名(公网)",
					"fileServer", "", null, this);
			PSysParam.create(BosConstants.current_sys, "1", "fileServerPort", "文件服务器sftp端口", "fileServer", "22", null,
					this);
			PSysParam.create(BosConstants.current_sys, "1", "fileServerLogin", "文件服务器sftp账号", "fileServer", "root",
					null, this);
			PSysParam.create(BosConstants.current_sys, "1", "fileServerPwd", "文件服务器sftp密码", "fileServer",
					"123123FFFfff", null, this);
			PSysParam.create(BosConstants.current_sys, "1", "fileServerRootPath", "文件服务器根目录(文件服务必须是webRoot目录)",
					"fileServer", "/server/static", null, this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramFileUploadMax, "上传文件限制(M)", "fileServer",
					"150", null, this);

			PSysParam.create(BosConstants.current_sys, "1", "checkCodeType", "验证码类型", "checkCode", "1", "130", this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramYearChartMaxCount, "年度图表最大年份数量", "chart",
					"3", null, this);

			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramWebCheckCode, "网页验证码", "checkCode", "1",
					"3", this);
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramForceUserChangePwd, "强制用户修改密码", "user",
					"1", "3", this);

			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramForceUserPwdLength, "密码最低位数", "user", "6",
					null, this);

			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramTrackFieldNum, "轨迹留痕字段最大数量", "track", "6",
					null, this);

			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramCacheDataForDB, "缓存DB列表数据默认时间(秒)",
					"cache", "0", null, this);
			
			
			
			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramCacheDataForEntityDisplay, "实体类显示本地缓存时间(秒)",
					"cache", "30", null, this);
			
			
			// PSysParam.create(Constants.sys_console, "1",
			// Constants.paramCacheDataForMongo, "缓存MONGO列表数据", "listCache", "0", 3L);

			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramListDataPerFun, "启用数据权限判断函数", "listCache",
					"0", "3", this);

			PSysParam.create(BosConstants.current_sys, "1", BosConstants.paramMatchingVillageByXY, "坐标自动匹配社村距离标准", "",
					"500", null, this);

			// PSysParam.create(Constants.sys_console, "1", Constants.paramLansSupport,
			// "多语种支持", "", "0", 3L);

			CacheTools.getSFTPConfig(true);
		}

		if (true) {
			QueryListInfo<PSysParam> list = this.getMgClient().getAll(PSysParam.class);
			for (PSysParam x : list.getList()) {
				if (x.getParamTpye() == null) {
					x.setParamTpye(1L);
					this.save(x, ut);
				}
			}
		}

		if (true) {

			LableLanDisplay dis = new LableLanDisplay();
			dis.setLan("CN");
			this.getMgClient().delete(dis,ut,this);

			this.getMgClient().deleteByPK("CN", PLanguage.class,null,this);

			PLanguage lan = this.getMgClient().getById("EN", PLanguage.class);
			if (lan == null) {
				lan = new PLanguage();
				lan.setLan("EN");
				lan.setStatus(0L);
				lan.setRemark("English");
				this.save(lan, ut);
			}

			lan = this.getMgClient().getById("JAP", PLanguage.class);
			if (lan == null) {
				lan = new PLanguage();
				lan.setLan("JAP");
				lan.setStatus(0L);
				lan.setRemark("にほんご ");
				this.save(lan, ut);
			}

			lan = this.getMgClient().getById("KO", PLanguage.class);
			if (lan == null) {
				lan = new PLanguage();
				lan.setLan("KO");
				lan.setStatus(0L);
				lan.setRemark("한국어");
				this.save(lan, ut);
			}

			lan = this.getMgClient().getById("ES", PLanguage.class);
			if (lan == null) {
				lan = new PLanguage();
				lan.setLan("ES");
				lan.setStatus(0L);
				lan.setRemark("Español");
				this.save(lan, ut);
			}

		}

		Timezone tz = this.getById("E8", Timezone.class);
		if (tz == null) {
			tz = new Timezone();
			tz.setTimeZoneId("E8");
			tz.setTimeZoneName("东八区");
		}
		tz.setSeq(8L);
		this.save(tz, ut);

		tz = this.getById("ew0", Timezone.class);
		if (tz == null) {
			tz = new Timezone();
			tz.setTimeZoneId("EW0");
			tz.setTimeZoneName("中时区");
		}
		tz.setSeq(0L);
		this.save(tz,ut);

		tz = this.getById("W8", Timezone.class);
		if (tz == null) {
			tz = new Timezone();
			tz.setTimeZoneId("W8");
			tz.setTimeZoneName("西八区");
		}
		tz.setSeq(-8L);
		this.save(tz, ut);
		
		
		
		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_DIY_INF, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_DIY_INF);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("接口配置列表");
			}
			// obj.setMapId("getListForMember");
			obj.setOrderBy("sys,className,code");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(DiyInf.class.getName());
			//obj.setInitScript("formType=${1} status=${1}");
			obj.setRegType(0L);
			//obj.setParentField("className");
			obj.setValueFormula("${title}[${className}--${infType}--${userId}]${jsonKey}");
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}
		
		
		
		if(true) {
 			PTreeForm  obj=this.getMgClient().getById(Constants.LIST_ALL_OIS, PTreeForm.class);
     		if(obj==null) {
     			obj=new PTreeForm();
     			obj.setCode(Constants.LIST_ALL_OIS);
     		}
     		if(DataChange.isEmpty(obj.getName())) {
     			obj.setName("所有运营方");
     		}
     		obj.setOrderBy("name");
     		obj.setSys(Constants.current_sys);
     		obj.setClassName(PMember.class.getName());
     		obj.setInitScript("isOis=${1}");
     		obj.setRegType(0L);
     		if(obj.getStatus()==null) {
     			obj.setStatus(1L);
     		}
     		this.save(obj, ut);
	 }
		
		
		
		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_PRODUCT_MENU, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_PRODUCT_MENU);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("产品的菜单列表");
			}
			obj.setOrderBy("menuName");
			obj.setSys(BosConstants.current_sys);
			obj.setClassName(ProductMenuDiy.class.getName());
			// obj.setInitScript("isOis=${1}");
			obj.setParentField("productId");
			obj.setRegType(0L);
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}
		
		
		
		

		if (true) {
			TreeSelectForm obj = this.getMgClient().getById(BosConstants.TREE_DATA_PER_ORG, TreeSelectForm.class);
			if (obj == null) {
				obj = new TreeSelectForm();
				obj.setCode(BosConstants.TREE_DATA_PER_ORG);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("数据权限设置(部门树形结构)");
			}
			obj.setRsys(Constants.current_sys);
			obj.setRclassName(DataPer.class.getName());
			obj.setRinitScript("memberId=${token.memberId} type=${1}");
			obj.setSelectedIdField("idValue");
			obj.setBizIdField("userId");
			obj.setSys(Constants.current_sys);
			obj.setClassName(POrg.class.getName());
			obj.setInitScript("memberId=${token.memberId}");
			obj.setOnlyLeafCheck(0L);
			obj.setCascadeCheck(1L);
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}

		if (true) {
			TreeSelectForm obj = this.getMgClient().getById(BosConstants.TREE_DATA_PER_PLACE, TreeSelectForm.class);
			if (obj == null) {
				obj = new TreeSelectForm();
				obj.setCode(BosConstants.TREE_DATA_PER_PLACE);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("本机构数据权限设置(地区树形结构)");
			}
			
			obj.setRsys(Constants.current_sys);
			obj.setRclassName(DataPer.class.getName());
			obj.setRinitScript("memberId=${token.memberId} type=${1}");
			obj.setSelectedIdField("idValue");
			obj.setBizIdField("userId");
			obj.setSys(Constants.current_sys);
			obj.setClassName(PPlaceInfo.class.getName());
			obj.setInitScript("memberId=${token.memberId}");
			obj.setOnlyLeafCheck(0L);
			obj.setCascadeCheck(1L);
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}

		if (true) {
			TreeSelectForm obj = this.getMgClient().getById(BosConstants.TREE_DATA_PER_PLACE_FOR_ADMIN,
					TreeSelectForm.class);
			if (obj == null) {
				obj = new TreeSelectForm();
				obj.setCode(BosConstants.TREE_DATA_PER_PLACE_FOR_ADMIN);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("运营方数据权限设置(地区树形结构)");
			}
			obj.setRsys(Constants.current_sys);
			obj.setRclassName(DataPer.class.getName());
			obj.setRinitScript("memberId=${req.memberId} type=${5}");
			obj.setSelectedIdField("idValue");
			obj.setBizIdField("userId");
			obj.setSys(Constants.current_sys);
			obj.setClassName(PPlaceInfo.class.getName());
			// obj.setInitScript("memberId=${token.memberId}");
			obj.addToMust("initScript");
			obj.setOnlyLeafCheck(0L);
			obj.setCascadeCheck(0L);
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.TREE_ORG, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.TREE_ORG);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("当前用户所属机构部门树形结构");
			}
			obj.setOrderBy("seq,orgName");
			obj.setSys(Constants.current_sys);
			obj.setClassName(POrg.class.getName());
			obj.setInitScript("memberId=${token.memberId}");
			obj.setRegType(0L);
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_ALL_MEMBER, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_ALL_MEMBER);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("所有机构");
			}

			obj.setOrderBy("name");
			obj.setSys(Constants.current_sys);
			obj.setClassName(PMember.class.getName());
			obj.setInitScript("");
			obj.addToMust("initScript");
			obj.setRegType(0L);
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_ALL_COUNTRY, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_ALL_COUNTRY);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("所有国家");
			}
			obj.setOrderBy("seq,placeName");
			obj.setInitScript("levelType=${0}");
			obj.setRegType(0L);
			obj.addToMust("initScript");
			obj.setSys(Constants.current_sys);
			obj.setClassName(PPlaceInfo.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_PLACE_BY_PARENT, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_PLACE_BY_PARENT);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("下级地区");
			}

			obj.setOrderBy("seq,placeName");
			// obj.setInitScript("");
			obj.addToMust("initScript");
			obj.setRegType(0L);
			obj.setSys(Constants.current_sys);
			obj.setClassName(PPlaceInfo.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			obj.setParentField("parentId");
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_ALL_PLACE, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_ALL_PLACE);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("所有地区列表");
			}
			// obj.setInitScript("countryId=${China}");
			obj.addToMust("initScript");
			obj.setOrderBy("seq,placeName");
			obj.setRegType(0L);
			obj.setValueFormula("${placeName} ${parentId}");
			obj.setSys(Constants.current_sys);
			obj.setClassName(PPlaceInfo.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_PLACE_BY_PARENT, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_PLACE_BY_PARENT);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("下级地区");
			}

			obj.setOrderBy("seq,placeName");
			// obj.setInitScript("");
			obj.addToMust("initScript");
			obj.setRegType(0L);
			obj.setSys(Constants.current_sys);
			obj.setClassName(PPlaceInfo.class.getName());
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			obj.setParentField("parentId");
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_PARENT_ORG_BYCURRENTMEMBER, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_PARENT_ORG_BYCURRENTMEMBER);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("当前机构某个部门的可选的上级部门列表");
			}
			obj.setOrderBy("seq,orgName");
			obj.setSys(Constants.current_sys);
			obj.setClassName(POrg.class.getName());
			obj.addToMust("initScript");
			obj.setInitScript("memberId=${token.memberId}");
			obj.setValueFormula("${orgName} ${parentId}");
			obj.setRegType(0L);
			obj.setParentField("");
			obj.addToMust("parentField");
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}

		if (true) {
			PTreeForm obj = this.getMgClient().getById(BosConstants.LIST_ALL_ORG_BYMEMBER, PTreeForm.class);
			if (obj == null) {
				obj = new PTreeForm();
				obj.setCode(BosConstants.LIST_ALL_ORG_BYMEMBER);
			}
			if (DataChange.isEmpty(obj.getName())) {
				obj.setName("某个机构的部门列表");
			}
			obj.setOrderBy("seq,orgName");
			obj.setSys(Constants.current_sys);
			obj.setClassName(POrg.class.getName());
			obj.addToMust("initScript");
			// obj.setInitScript("memberId=${token.memberId}");
			obj.setValueFormula("${orgName} ${parentId}");
			obj.setRegType(0L);
			obj.setParentField("memberId");
			if (obj.getStatus() == null) {
				obj.setStatus(1L);
			}
			this.save(obj, ut);
		}
		
		
		
		
		
		try {
			
			PMember member=this.getById("1", PMember.class);
			if(member==null) {
				member=new PMember();
				member.setMemberId("1");
			}
			
			if(DataChange.isEmpty(member.getName())) {
				member.setName(BosConstants.copyright);
			}
			member.setAdminId("1");
			member.setStatus(1L);
			this.save(member, ut);
			
			
			
			PUser user=this.getById("1", PUser.class); 
			if(user==null) {
				user=new PUser();
				user.setUserId("1");
				user.setMemberId("1");
				user.setLoginPwd("111111");
			}
			if(DataChange.isEmpty(user.getLoginName())) {
				user.setLoginName("admin");
			}
			if(DataChange.isEmpty(user.getUserName())) {
				user.setUserName("超级管理员");
			}
			user.setStatus(1L);
			this.save(user, ut);
			
		}catch(Exception e) {
			e.printStackTrace();
		}

	}



}
