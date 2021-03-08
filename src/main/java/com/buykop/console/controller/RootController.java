package com.buykop.console.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.service.RootService;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.BizFieldModifyTrack;
import com.buykop.framework.entity.PROrgRole;
import com.buykop.framework.entity.PUserMember;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.mysql.Import;
import com.buykop.framework.oauth2.PMemberType;
import com.buykop.framework.oauth2.PRMemberRoot;
import com.buykop.framework.oauth2.PRMemberType;
import com.buykop.framework.oauth2.PRRoleFun;
import com.buykop.framework.oauth2.PRole;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.DBIndexExec;
import com.buykop.framework.scan.Field;
import com.buykop.framework.scan.PDBFun;
import com.buykop.framework.scan.PDBProc;
import com.buykop.framework.scan.LabelDisplay;
import com.buykop.framework.scan.LableLanDisplay;
import com.buykop.framework.scan.PLanguage;
import com.buykop.framework.scan.PRoot;
import com.buykop.framework.scan.PServiceParam;
import com.buykop.framework.scan.PServiceUri;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.ServerConfig;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.ClassInnerNotice;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.data.CheckUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.BosEntity;
import com.buykop.framework.util.type.ListData;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;
import com.buykop.console.util.Constants;

@Module(display = "系统", sys = Constants.current_sys)
@RestController
@RequestMapping("/root")
public class RootController extends BaseController {

	// private static Logger log=Logger.getLogger(RootController.class);

	@Autowired
	private RootService service;

	@Menu(name = "子系统", trunk = "开发服务,开发管理", js = "root")
	@Security(accessType = "1", displayName = "子系统列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/list", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject list(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			PRoot search = json.getSearch(PRoot.class, null, ut, this.service);
			QueryListInfo<PRoot> list = this.service.getMgClient().getList(search, "sort",this.service);

			for (PRoot r : list.getList()) {
				ServerConfig config = new ServerConfig();
				config.setSys(r.getCode());
				QueryListInfo<ServerConfig> sList = this.service.getMgClient().getList(config,this.service);
				StringBuffer sb = new StringBuffer();
				for (ServerConfig s : sList.getList()) {
					sb.append(s.getIp() + ":" + s.getPort() + ",");
				}
				String s = sb.toString();
				if (s.length() > 1)
					s = s.substring(0, s.length() - 1);
				r.setServerInfo(s);
			}

			BosConstants.debug("root list size=" + list.size());

			super.listToJson(list, json, BosConstants.getTable(PRoot.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "扫描数据库", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/scanDB", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject scanDB(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			Import importUtil = new Import(this.service.getBaseDao());

			Vector<String> sysV = Field
					.split(DataChange.replaceNull(PSysParam.paramValue("1", BosConstants.paramScanDB)));

			System.out.println("扫描库:" + PSysParam.paramValue("1", BosConstants.paramScanDB));

			for (String sys : sysV) {

				System.out.println("扫描 sys=" + sys + "  userId=" + ut.getUserId() + "   memberId=" + ut.getMemberId());

				PRMemberRoot rmr = new PRMemberRoot();
				rmr.setMemberId(ut.getMemberId());
				rmr.setSys(sys);
				this.service.save(rmr, ut);

				PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
				if (root == null) {
					root = new PRoot();
					root.setCode(sys);
					root.setStatus(0L);
					root.setRegType(1L);
				}
				root.setIsValid(1);
				this.service.save(root,ut);

				importUtil.createDatabase(sys);
				Vector<String> tv = importUtil.getAllTable(sys);

				System.out.println("table size=" + tv.size());

				for (String t : tv) {

					String simpleName = t.toLowerCase();
					if (simpleName.startsWith("t_")) {
						simpleName = simpleName.substring(2, simpleName.length());
					} else if (simpleName.endsWith("_t")) {
						simpleName = simpleName.substring(0, simpleName.length() - 2);
					}
					simpleName = CheckUtil.getJavaNameByDB(simpleName);
					simpleName = simpleName.substring(0, 1).toUpperCase()
							+ simpleName.substring(1, simpleName.length());

					Table table = this.service.getMgClient().getTableById(sys.toLowerCase() + "." + simpleName);
					if (table == null) {
						table = new Table();
						table.setClassName(sys.toLowerCase() + "." + simpleName);
						table.setDisplayName(simpleName);
						table.setIdAutoIncrement(0L);
					}
					table.setRegType(1L);
					table.setSimpleName(simpleName);
					table.setCode(t.toUpperCase());
					table.setSys(sys);
					table.setCache(0L);
					if (table.getIdAutoIncrement() == null) {
						table.setIdAutoIncrement(0L);
					}
					
					this.service.getRdClient().putClassNameBySimpleName(table);

					List<Field> fields = importUtil.getFieldList(table);

					BosConstants.debug("scan field table className=" + table.getClassName() + "   code="
							+ table.getCode() + "  fields size=" + fields.size());

					for (Field f : fields) {
						f.setClassName(table.getClassName());
						if (DataChange.isEmpty(f.getDisplay())) {
							f.setDisplay(f.getProperty());
						}
						this.service.save(f, ut);
						// System.out.println("className="+f.getClassName()+"
						// property="+f.getProperty()+" valueClass="+f.getValueClass()+"
						// dbLen="+f.getDbLen()+" display="+f.getDisplay()+" isKey="+f.getIsKey());
					}

					this.service.save(table, ut);

					Vector<Field> fs = table.addedField(this.service);
					for (Field f : fs) {
						this.service.save(f, ut);
					}

					// 删除数据字段
					Field fss = new Field();
					fss.setClassName(table.getClassName());
					fss.setPropertyType(1L);
					QueryListInfo<Field> fList = this.service.getMgClient().getList(fss,this.service);
					for (Field x : fList.getList()) {
						boolean exist = false;
						for (Field x1 : fields) {
							if (x1.getProperty().equals(x.getProperty())) {
								exist = true;
								break;
							}
						}
						if (!exist) {
							this.service.getMgClient().deleteByPK(x.getPk(), Field.class,ut,this.service);
						}
					}

					BosConstants.removeTable(table.getClassName());
					new ClassInnerNotice().invoke(BosConstants.innerNotice_synTable, table.getClassName());// 通知其他服务

				}
			}

			json.setSuccess("扫描成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "保存子系统列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			Import imp = new Import(this.service.getBaseDao());

			Vector<String> autoV = new Vector<String>();

			long i = 0;
			List<PRoot> list = json.getList(PRoot.class, "code,displayName,regType,status", this.service);

			for (PRoot x : list) {
				
				if(x.getCode().equalsIgnoreCase("redis") || x.getCode().equalsIgnoreCase("json") || x.getCode().equals("token") || x.getCode().equals("req") ) {
					json.setUnSuccess(-1, "系统代码非法,不允许使用  redis json req token等关键字");
					return json.jsonValue();
				}

				if (BosConstants._sysV.contains(x.getCode())) {
					x.setStatus(1L);
				}

				if (x.getRegType().intValue() == 0) {
					autoV.add(x.getCode().toLowerCase());
				}
			}

			BosConstants.debug(autoV);

			Vector<String> v = new Vector<String>();

			for (PRoot x : list) {

				String code = x.getCode();

				i++;

				if (v.contains(x.getCode().toLowerCase().trim())) {
					continue;
				}

				PRoot src = this.service.getMgClient().getById(x.getCode(), PRoot.class);

				if (src != null) {
					x.setPackagePath(src.getPackagePath());
				}

				v.add(x.getCode().toLowerCase().trim());

				imp.createDatabase(x.getCode());

				BosConstants.debug(i + "  " + x.getCode() + "  reg=" + x.getRegType() + "   "
						+ autoV.contains(x.getCode().toLowerCase().trim()));

				if (x.getRegType().intValue() == 0) {
					if (src == null)
						continue;
					src.setSort(x.getSort());
					src.setDisplayName(x.getDisplayName());
					src.setStatus(x.getStatus());
					this.service.save(src,ut);
				} else {
					// 手工的系统代码不能与自动的重复
					if (autoV.contains(x.getCode().toLowerCase().trim())) {
						json.setUnSuccess(-1, x.getCode() + LabelDisplay.get("为自动注册,不允许手工重复建立", json.getLan()), true);
						return json.jsonValue();
					}

					x.setCode(code);
					x.setPK(code);
					// x.setSort(i++);
					if (x.getRegType() != null && x.getRegType().intValue() == 1
							&& DataChange.isEmpty(x.getPackagePath())) {
						x.setPackagePath("cn.powerbos.spring." + x.getCode());
					}
					this.service.save(x, ut);

				}

				if (x.getStatus().intValue() == 1) {
					PRMemberRoot rmr = new PRMemberRoot();
					rmr.setMemberId(ut.getMemberId());
					rmr.setSys(x.getCode());
					this.service.save(rmr, ut);
				}

			}

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	/**
	 * @Security(accessType = "1", displayName = "下载JSON配置", needLogin = true,
	 *                      isEntAdmin = false, isSysAdmin = false,ipCheck=2)
	 * @RequestMapping(value = "/exportJson",method = RequestMethod.POST)
	 * @ResponseBody public HttpEntity exportJson(@RequestBody HttpEntity
	 *               json,HttpServletRequest request) throws Exception{
	 * 
	 *               //Constants.debug("***************1\n"+json.stringValue());
	 * 
	 * 
	 *               Client conn=JedisPool.getInstance().getConn();
	 *               conn.setLan(json.getLan());
	 * 
	 *               try {
	 * 
	 * 
	 *               long start=NetWorkTime.getCurrentDatetime().getTime();
	 * 
	 *               ServiceInvoke log=new ServiceInvoke();
	 * 
	 *               UserToken ut = super.securityCheck(json,request,log);
	 * 
	 *               if(ut==null) { return json.jsonValue(); }
	 * 
	 * 
	 * 
	 *               String sys=json.getSelectedId(Constants.current_sys,
	 *               "/root/exportJson",PRoot.class.getName(),null,true);
	 * 
	 *               PRoot root=this.service.getMgClient().getByPK(sys,
	 *               PRoot.class);
	 * 
	 *               if(root==null) { json.setUnSuccessForNoRecord(PRoot.class);
	 *               return json.jsonValue(); }
	 * 
	 * 
	 *               Util.saveToJson(sys);
	 * 
	 *               log.setExecTime(NetWorkTime.getCurrentDatetime().getTime() - start);
	 *               Pool.getInstance().getConn().save(log, null);
	 *               json.setSuccess("导出成功");
	 * 
	 *               }catch(Exception e){ json.setUnSuccess(e);
	 * 
	 *               }
	 * 
	 *               return json.jsonValue(); }
	 */

	@Security(accessType = "1", displayName = "重建索引", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/rebuildIndex", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject rebuildIndex(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		// Constants.debug("***************1\n"+json.stringValue());

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/rebuildIndex", Table.class.getName(), "",
					true, this.getService());

			PRoot obj = this.service.getMgClient().getById(sys, PRoot.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			Table table = new Table();
			table.setSys(sys);
			QueryListInfo<Table> tList = this.service.getMgClient().getList(table,this.service);

			for (Table x : tList.getList()) {
				
				if(x.getCache()==null) {
					x.setCache(0L);
				}
				
				if(x.getCache().intValue()==0) {
					DBIndexExec exec = new DBIndexExec();
					exec.setClassName(x.getClassName());
					this.service.getMgClient().delete(exec,ut,this.service);
					Import importDB = new Import(service.getBaseDao());
					importDB.createIndex(x, service.getBaseDao().getDbType(), true);
				}else if(x.getCache().intValue()==2) {
					this.service.getMgClient().synIndex(x.getClassName(),this.service);
				}
				
			}

			json.setSuccess("重建索引成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();

	}

	@Security(accessType = "1", displayName = "子系统详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/root/info", PRoot.class.getName(), null, true,
					this.getService());

			PRoot obj = this.service.getById(id, PRoot.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PRoot.class, id);
				return json.jsonValue();
			}

			obj.resetMenuJson(this.service);

			// if(obj.getRegType()!=null && obj.getRegType().intValue()==1) {
			// obj.setPackagePath("cn.powerbos.spring."+obj.getCode());
			// }

			super.objToJson(obj, json);

			// 机构类型及角色
			PMemberType type = new PMemberType();
			type.setSys(id);
			QueryListInfo<PMemberType> typeList = this.service.getMgClient().getList(type, "typeName",this.service);
			super.listToJson(typeList, json, BosConstants.getTable(PMemberType.class));
			// 绑定机构实体类
			super.selectToJson(Table.getFKJsonForSelectMemberType(id, 0L, this.service), json, PMemberType.class,
					"className");

			PRole role = new PRole();
			role.setSys(id);
			super.listToJson(this.service.getMgClient().getList(role, "typeId,roleName",this.service), json,
					BosConstants.getTable(PRole.class));
			super.selectToJson(PMemberType.selectForSys(id,this.service), json, PRole.class.getSimpleName(), "typeId");
			super.selectToJson(PRoot.getJsonForDev(ut.getMemberId(),this.service), json, PRole.class.getSimpleName(), "sys");
			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "清理参数", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/uriClear", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject uriClear(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/uriClear", PRoot.class.getName(), null, true,
					this.getService());

			PServiceParam search = new PServiceParam();
			search.setSys(sys);
			this.service.getMgClient().delete(search,ut,this.service);

			json.setSuccess("清理成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "删除系统", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/root/delete", PRoot.class.getName(), "", true,
					this.getService());

			if (BosConstants._sysV.contains(id)) {
				json.setUnSuccess(-1, "该系统不允许删除");
				return json.jsonValue();
			}

			Table table = new Table();
			table.setSys(id);
			long num = this.service.getMgClient().getCount(table,this.service);
			if (num > 0) {
				json.setUnSuccess(-1, "该系统下含有" + num + "个业务类而不能删除");
				return json.jsonValue();
			}

			PRoot.delete(id, this.service);

			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "删除机构类型", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/deleteMemberType", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deleteMemberType(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/root/deleteMemberType", PMemberType.class.getName(),
					"", true, this.getService());
			PMemberType obj = this.service.getMgClient().getById(id, PMemberType.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PMemberType.class);
				return json.jsonValue();
			}

			PRMemberType rmt = new PRMemberType();
			rmt.setTypeId(id);
			rmt.setStatus(2L);
			long count = this.service.getMgClient().getCount(rmt,this.service);
			if (count > 0) {
				json.setUnSuccess(-1, "该机构类型关联了机构,不允许删除,请先解除关联");
				return json.jsonValue();
			}

			PRole role = new PRole();
			role.setTypeId(id);
			count = this.service.getMgClient().getCount(role,this.service);
			if (count > 0) {
				json.setUnSuccess(-1, "该机构类型关联角色,不允许删除,请先解除关联");
				return json.jsonValue();
			}

			this.service.getMgClient().deleteByPK(id, PMemberType.class,ut,this.service);

			rmt = new PRMemberType();
			rmt.setTypeId(id);
			this.service.getMgClient().delete(rmt,ut,this.service);

			role = new PRole();
			role.setTypeId(id);
			this.service.getMgClient().delete(role,ut,this.service);

			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "删除角色", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/deleteRole", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject deleteRole(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/root/deleteRole", PRole.class.getName(), "", true,
					this.getService());
			PRole obj = this.service.getMgClient().getById(id, PRole.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PRole.class);
				return json.jsonValue();
			}

			this.service.getMgClient().deleteByPK(id, PRole.class,ut,this.service);

			
			PUserMember um=new PUserMember();
			QueryListInfo<PUserMember> umList=this.service.getList(um, null);
			for(PUserMember x:umList.getList()) {
				if(DataChange.isEmpty(x.getRoles())) continue;
				Vector<String> rx=Field.split(x.getRoles());
				if(rx.contains(id)) {
					rx.remove(id);
					x.setRoles(MyString.CombinationBy(rx, ","));
					this.service.save(x, ut);
				}
				
			}
			
			// 删除角色关联权限
			PRRoleFun rf = new PRRoleFun();
			rf.setRoleId(id);
			this.service.getMgClient().delete(rf,ut,this.service);
			
			
			PROrgRole or=new PROrgRole();
			or.setRoleId(id);
			this.service.delete(or, ut);
			
			

			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "子系统详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/save", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject save(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			// String id=json.getSelectedId(Constants.current_sys,
			// "/root/info",PRoot.class.getName(),null,true);

			PRoot obj = json.getObj(PRoot.class, null, this.service);

			PRoot src = this.service.getMgClient().getById(obj.getCode(), PRoot.class);

			if (src == null) {
				json.setUnSuccessForNoRecord(PRoot.class, obj.getCode());
				return json.jsonValue();
			}

			// obj.setCode(id);
			this.service.save(obj, ut);

			List<PMemberType> typeList = json.getList(PMemberType.class, "typeId,typeName,status,!feeType",
					this.service);
			for (PMemberType x : typeList) {
				if (x.getTypeId().equals(BosConstants.memberType_admin)) {
					x.setStatus(1L);
				}
				if (x.getFeeType() == null) {
					x.setFeeType(0L);
				}
				x.setSys(obj.getCode());
				if (x.getStatus().intValue() == 0) {// 机构类型禁用,则关联角色全部禁用
					PRole r = new PRole();
					r.setTypeId(x.getTypeId());
					QueryListInfo<PRole> rList = this.service.getMgClient().getList(r,this.service);
					for (PRole x1 : rList.getList()) {
						x1.setStatus(0L);
						this.service.save(x1,ut);
					}
				}
				this.service.save(x, ut);
			}

			List<PRole> rList = json.getList(PRole.class, "roleId,roleName,typeId,status", this.service);
			for (PRole x : rList) {
				PMemberType type = this.service.getMgClient().getById(x.getTypeId(), PMemberType.class);
				if (type == null) {
					continue;
				}
				if (type.getStatus() == null || type.getStatus().intValue() != 1) {
					x.setStatus(0L);
				}
				if(DataChange.isEmpty(x.getSys())) {
					x.setSys(obj.getCode());
				}
				this.service.save(x,ut);
			}

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	// @Security(accessType = "1", displayName = "数据库索引", needLogin = true,
	// isEntAdmin = false, isSysAdmin = true)
	@RequestMapping(value = "/script", method = RequestMethod.GET)
	@ResponseBody
	public void script(HttpServletRequest request, HttpServletResponse response, String token, String sys)
			throws Exception {

		response.setHeader("Pragma", "No-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setContentType("text/plain;charset=UTF-8");

		if (DataChange.isEmpty(token)) {
			response.getWriter().write("请求参数有误");
			return;
		}

		if (DataChange.isEmpty(sys)) {
			response.getWriter().write("请求参数有误");
			return;
		}

		

		try {

			UserToken ut = this.getService().getRdClient().getUserToken(token);

			if (ut == null) {
				response.getWriter().write("您没有权限");
				return;
			}

			if (!DataChange.replaceNull(ut.getUserId()).equals("1")) {
				response.getWriter().write("您没有权限");
				return;
			}

			List<Table> list = Table.getSysTableList(sys, this.service);
			// Constants.debug("sql("+sys+") list
			// size="+list.size()+"/"+Table.getSysTableList(null).size());

			response.getWriter().write("/**-----------以下是删除表脚本-----------**/;\n");
			for (Table obj : list) {
				if (obj.getCache() == null || obj.getCache().intValue() != 0)
					continue;
				response.getWriter().write(
						BosConstants.getTable(obj.getClassName()).dropTableSQL(this.service.getBaseDao().getDbType())
								+ ";\n");
			}

			response.getWriter().write("\n\n\n\n\n\n\n\n");
			response.getWriter().write("/**-----------以下是创建表脚本-----------**/;\n");
			for (Table obj : list) {
				if (obj.getCache() == null || obj.getCache().intValue() != 0)
					continue;
				response.getWriter().write(
						BosConstants.getTable(obj.getClassName()).createTableSQL(this.service.getBaseDao().getDbType())
								+ ";\n");
			}

			response.getWriter().write("\n\n\n\n\n\n\n\n");
			response.getWriter().write("/**-----------以下是索引脚本-----------**/;\n");
			for (Table obj : list) {
				if (obj.getCache() == null || obj.getCache().intValue() != 0)
					continue;
				Vector<String> sqlV = BosConstants.getTable(obj.getClassName())
						.getIndexSQL(this.service.getBaseDao().getDbType(),this.service);
				for (String sql : sqlV) {
					response.getWriter().write(sql + ";\n");
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally {
			response.getWriter().flush();
		}

	}

	@RequestMapping(value = "/menuJson", method = RequestMethod.GET)
	public void menuJson(HttpServletRequest request, HttpServletResponse response, String token, String sys)
			throws Exception {

		response.setHeader("Pragma", "No-cache");
		response.setHeader("Cache-Control", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setHeader("Content-type", "text/json;charset=UTF-8");

		try {

			UserToken ut = this.getService().getRdClient().getUserToken(token);
			if (ut == null) {
				return;
			}

			HttpEntity json = new HttpEntity();
			json.setTokenKey(token);

			if (DataChange.isEmpty(sys)) {
				response.getWriter().write("{}");
				return;
			}

			PRoot obj = this.service.getById(sys, PRoot.class);
			if (obj == null) {
				response.getWriter().write("{}");
				return;
			}

			response.getWriter().write(obj.getMenuJson(json, ut, this.service).toString());

		} catch (Exception e) {
			e.printStackTrace();
			response.getWriter().write("{}");
		}

	}

	@Security(accessType = "1", displayName = "数据库函数及存储过程", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbFunProcList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbFunProcList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {



		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/dbFunProcList", PRoot.class.getName(), null,
					true, this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			PDBFun sf = json.getSearch(PDBFun.class, null, ut, this.service);
			sf.setSys(sys);
			QueryListInfo<PDBFun> list = this.service.getMgClient().getList(sf, "className,funCode",this.service);
			super.listToJson(list, json, BosConstants.getTable(PDBFun.class));

			PDBProc sp = json.getSearch(PDBProc.class, null, ut, this.service);
			sp.setSys(sys);
			QueryListInfo<PDBProc> spList = this.service.getMgClient().getList(sp, "className,procCode",this.service);
			super.listToJson(spList, json, BosConstants.getTable(PDBProc.class));

			super.selectToJson(Table.getDBJsonForSelect(sys, this.service), json, PDBFun.class, "className");
			super.selectToJson(Table.getDBJsonForSelect(sys, this.service), json, PDBProc.class, "className");

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据库函数", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbFunList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbFunList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/dbFunProcList", PRoot.class.getName(), null,
					true, this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			PDBFun sf = new PDBFun();
			sf.setSys(sys);
			QueryListInfo<PDBFun> list = this.service.getMgClient().getList(sf, "className,funCode",this.service);
			super.listToJson(list, json, BosConstants.getTable(PDBFun.class));

			super.selectToJson(Table.getDBJsonForSelect(sys, this.service), json, PDBFun.class, "className");

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据库函数", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbFunListSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbFunListSave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/dbFunProcList", PRoot.class.getName(), null,
					true, this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			List<PDBFun> list = json.getList(PDBFun.class, "className,funCode,!property,!inputFields,note,status",
					this.service);
			for (PDBFun x : list) {
				x.setSys(sys);
				x.checkInputFields(this.service);

				if (!DataChange.isEmpty(x.getProperty())) {
					Field field = new Field();
					field.setClassName(x.getClassName());
					field.setProperty(x.getProperty());
					field = this.service.getMgClient().get(field,this.service);
					if (field == null) {
						json.setUnSuccess(-1, LabelDisplay.get("输入绑定属性:", json.getLan()) + x.getInputFields()
								+ LabelDisplay.get(",但是表对象不存在属性:", json.getLan()) + x.getProperty(), true);
						return json.jsonValue();
					}
					if (field.getPropertyType().intValue() != 0) {
						json.setUnSuccess(-1,
								LabelDisplay.get("输入绑定属性:", json.getLan()) + x.getInputFields()
										+ LabelDisplay.get(",但是表对象属性:", json.getLan()) + x.getProperty()
										+ LabelDisplay.get(" 必须是扩展属性", json.getLan()),
								true);
						return json.jsonValue();
					}
				}
				x.setDbType(this.service.getBaseDao().getDbType());
				this.service.save(x, ut);
			}

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据库函数详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbFunInfo", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbFunInfo(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/root/dbFunInfo", PDBFun.class.getName(), null, true,
					this.getService());

			PDBFun obj = this.service.getMgClient().getById(id, PDBFun.class);

			if (obj == null) {
				json.setUnSuccessForNoRecord(PDBFun.class);
				return json.jsonValue();
			}

			super.objToJson(obj, json);

			BizFieldModifyTrack track = new BizFieldModifyTrack();
			track.setClassName(PDBFun.class.getName());
			track.setIdValue(obj.getPk());
			QueryListInfo<BizFieldModifyTrack> tlist = this.service.getMgClient().getList(track, "!changeDate",this.service);
			super.listToJson(tlist, json, BosConstants.getTable(BizFieldModifyTrack.class));

			super.selectToJson(Table.getDBJsonForSelect(obj.getSys(), this.service), json, PDBFun.class, "className");

			QueryListInfo<Field> fList = new QueryListInfo<Field>();
			if (!DataChange.isEmpty(obj.getClassName())) {
				super.selectToJson(Field.getJsonForSelect(obj.getClassName(), 0L, json.getLan(), this.service), json,
						PDBFun.class, "property");
				Field fs = new Field();
				fs.setPropertyType(1L);
				fs.setClassName(obj.getClassName());
				fList = this.service.getMgClient().getList(fs, "!isKey,!propertyType,property",this.service);
			}
			super.listToJson(fList, json, BosConstants.getTable(Field.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据库函数保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbFunSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbFunSave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {



		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/dbFunProcList", PRoot.class.getName(), null,
					true, this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			PDBFun obj = json.getObj(PDBFun.class,
					"className,funCode,!property,!inputFields,note,content,searchCondition", this.service);

			PDBFun fun = this.service.getMgClient().getById(obj.getPk(), PDBFun.class);

			obj.setSys(sys);
			obj.checkInputFields(this.service);
			obj.setDbType(this.service.getBaseDao().getDbType());

			if (!obj.getContent().equals(fun.getContent()) && !DataChange.isEmpty(fun.getContent())) {
				BizFieldModifyTrack track = new BizFieldModifyTrack();
				track.setClassName(PDBFun.class.getName());
				track.setTrackId(BizFieldModifyTrack.next());
				track.setIdValue(fun.getPk());
				track.setProperty("content");
				track.setPropertyValue(fun.getContent());
				track.setChangeUserId(ut.getUserId());
				track.setChangeDate(NetWorkTime.getCurrentDatetime());
				track.setActionType(2L);
				this.service.save(track, ut);
			}

			this.service.save(obj,ut);

			obj = this.service.getMgClient().getById(obj.getPk(), PDBFun.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PDBFun.class);
				return json.jsonValue();
			}

			Vector<String> sqlV = obj.showSqlV(this.service.getBaseDao().getDbType(), false);

			for (String sql : sqlV) {
				BosConstants.debug(sql);
				this.service.getBaseDao().execute(sql);
			}

			// BosEntity tj=new TableJson(obj.getClassName());
			// Table table=tj.showTable();

			// if(table.table()) {
			// this.service.getBaseDao().get(tj);
			// }

			Long cacheData = PSysParam.paramLongValue("1", BosConstants.paramCacheDataForDB);
			if (cacheData == null) {
				cacheData = 0L;
			}

			if (obj.getSearchCondition().intValue() == 1 && cacheData.intValue() > 0) {
				new ClassInnerNotice().invoke(ListData.class.getSimpleName(), obj.getClassName());
			}

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据库函数删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbFunDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbFunDelete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/root/dbFunDelete", PDBFun.class.getName(), null,
					true, this.getService());

			PDBFun obj = this.service.getMgClient().getById(id, PDBFun.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PDBFun.class);
				return json.jsonValue();
			}

			Vector<String> sqlV = obj.showSqlV(this.service.getBaseDao().getDbType(), true);

			for (String sql : sqlV) {
				BosConstants.debug(sql);
				this.service.getBaseDao().execute(sql);
				break;// 执行第一个
			}

			this.service.getMgClient().deleteByPK(id, PDBFun.class,ut,this.service);

			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	// ---------------------------------

	@Security(accessType = "1", displayName = "数据库存储过程", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbProcList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbProcList(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/dbFunProcList", PRoot.class.getName(), null,
					true, this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			PDBProc sf = new PDBProc();
			sf.setSys(sys);
			QueryListInfo<PDBProc> list = this.service.getMgClient().getList(sf, "className,procCode",this.service);
			super.listToJson(list, json, BosConstants.getTable(PDBProc.class));

			super.selectToJson(Table.getDBJsonForSelect(sys, this.service), json, PDBProc.class, "className");

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据库存储过程列表保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbProcListSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbProcListSave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/dbFunProcList", PRoot.class.getName(), null,
					true, this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			List<PDBProc> list = json.getList(PDBProc.class, "className,procCode,!propertys,!inputFields,note,status",
					this.service);
			for (PDBProc x : list) {
				x.setSys(sys);
				x.checkParam();

				x.setDbType(this.service.getBaseDao().getDbType());
				this.service.save(x, ut);
			}

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据库存储过程详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbProcInfo", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbProcInfo(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/root/dbProcInfo", PDBFun.class.getName(), null,
					true, this.getService());

			PDBProc obj = this.service.getMgClient().getById(id, PDBProc.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PDBFun.class);
				return json.jsonValue();
			}

			Table table = this.service.getMgClient().getTableById(obj.getClassName());

			super.objToJson(obj, json);

			super.selectToJson(Table.getDBJsonForSelect(obj.getSys(), this.service), json, PDBProc.class, "className");

			QueryListInfo<Field> fList = new QueryListInfo<Field>();
			if (!DataChange.isEmpty(obj.getClassName())) {

				if (table.getRegType().intValue() == 1) {
					Field fs = new Field();
					fs.setCustomer(0L);
					fs.setClassName(obj.getClassName());
					fList = this.service.getMgClient().getList(fs, "!isKey,!propertyType,property",this.service);
				} else {
					Field fs = new Field();
					fs.setClassName(obj.getClassName());
					fList = this.service.getMgClient().getList(fs, "!isKey,!propertyType,property",this.service);
				}

			}
			super.listToJson(fList, json, BosConstants.getTable(Field.class));

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据库存储过程保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbProcSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbProcSave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {



		try {



			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/dbFunProcList", PRoot.class.getName(), null,
					true, this.getService());

			PRoot root = this.service.getMgClient().getById(sys, PRoot.class);
			if (root == null) {
				json.setUnSuccessForNoRecord(PRoot.class, sys);
				return json.jsonValue();
			}

			PDBProc obj = json.getObj(PDBProc.class, "className,procCode,!propertys,!inputFields,note,content",
					this.service);

			obj.checkParam();
			obj.setSys(sys);
			obj.setDbType(this.service.getBaseDao().getDbType());

			this.service.save(obj, ut);

			obj = this.service.getMgClient().getById(obj.getPk(), PDBProc.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PDBProc.class);
				return json.jsonValue();
			}

			Vector<String> sqlV = obj.showSqlV(this.service.getBaseDao().getDbType(), false);

			for (String sql : sqlV) {
				BosConstants.debug(sql);
				this.service.getBaseDao().execute(sql);
			}

			json.setSuccess("保存成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "数据库存储过程删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/dbProcDelete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject dbProcDelete(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/root/dbProcDelete", PDBFun.class.getName(), null,
					true, this.getService());

			PDBProc obj = this.service.getMgClient().getById(id, PDBProc.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PDBFun.class);
				return json.jsonValue();
			}

			Vector<String> sqlV = obj.showSqlV(this.service.getBaseDao().getDbType(), true);

			for (String sql : sqlV) {
				BosConstants.debug(sql);
				this.service.getBaseDao().execute(sql);
				break;// 执行第一个
			}

			this.service.getMgClient().deleteByPK(id, PDBProc.class,ut,this.service);

			json.setSuccess("删除成功");

		} catch (Exception e) {
			json.setUnSuccess(e);

		}

		return json.jsonValue();
	}

	// ---------------------------------------以下是菜单多语言设置--------------------------------------------------------
	@Security(accessType = "1", displayName = "菜单的多语种设置", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/menuListForLan", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject menuListForLan(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		// Constants.debug("***************1\n"+json.stringValue());

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/menuListForLan", PRoot.class.getName(), null,
					true, this.getService());

			PRoot obj = this.service.getMgClient().getById(sys, PRoot.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PRoot.class);
				return json.jsonValue();
			}
			super.objToJson(obj, json);

			// 多语言支持
			HashMap<String, QueryListInfo<PServiceUri>> fhash = new HashMap<String, QueryListInfo<PServiceUri>>();

			PLanguage lan = new PLanguage();
			lan.setStatus(1L);
			QueryListInfo<PLanguage> llist = this.service.getMgClient().getList(lan, "seq",this.service);
			if (llist.size() <= 0) {
				json.setUnSuccess(-1, "系统未配置多语言");
				return json.jsonValue();
			}

			SelectBidding sb = new SelectBidding();
			for (PLanguage x : llist.getList()) {
				fhash.put(x.getLan(), new QueryListInfo<PServiceUri>());
				sb.put(x.getLan(), x.getRemark());
			}

			if (true) {

				PServiceUri search = new PServiceUri();
				search.setSys(sys);
				search.setIsMenu(1L);
				QueryListInfo<PServiceUri> list = this.service.getMgClient().getList(search, "menu",this.service);
				for (PServiceUri x : list.getList()) {

					HashMap<String, String> dHash = new HashMap<String, String>();
					String labelId = PServiceUri.class.getSimpleName() + "_" + x.getPk() + "_menu";
					LableLanDisplay s = new LableLanDisplay();
					s.setLabelId(labelId);
					QueryListInfo<LableLanDisplay> dlist = this.service.getMgClient().getList(s, "lan",this.service);
					for (LableLanDisplay x1 : dlist.getList()) {
						dHash.put(x1.getLan() + "_menu", x1.getDisplay());
					}

					labelId = PServiceUri.class.getSimpleName() + "_" + x.getPk() + "_displayName";
					s = new LableLanDisplay();
					s.setLabelId(labelId);
					dlist = this.service.getMgClient().getList(s, "lan",this.service);
					for (LableLanDisplay x1 : dlist.getList()) {
						dHash.put(x1.getLan() + "_displayName", x1.getDisplay());
					}

					for (PLanguage xl : llist.getList()) {

						QueryListInfo<PServiceUri> fList = fhash.get(xl.getLan());
						PServiceUri uri = new PServiceUri();
						uri.setSys(sys);
						uri.setUri(x.getUri());
						uri.setValue(x.getMenu());
						uri.setClassName(x.getClassName());
						uri.setMethod(x.getMethod());

						if (dHash.containsKey(xl.getLan() + "_displayName")) {
							uri.setDisplayName(dHash.get(xl.getLan() + "_displayName"));
						}

						if (dHash.containsKey(xl.getLan() + "_menu")) {
							uri.setMenu(dHash.get(xl.getLan() + "_menu"));
						}

						fList.getList().add(uri);
					}
				}

			}

			for (PLanguage x : llist.getList()) {
				QueryListInfo<PServiceUri> fList = fhash.get(x.getLan());
				super.listToJson(fList, json, PServiceUri.class.getSimpleName() + x.getLan(),
						"sys,uri,className,method,value,displayName,menu", BosConstants.getTable(PServiceUri.class));
			}

			json.getData().put("lanList", sb.showJSON());

			if (true) {

				String labelId = PRoot.class.getSimpleName() + "_" + obj.getPk() + "_displayName";

				QueryListInfo<LableLanDisplay> lanDisList = new QueryListInfo<LableLanDisplay>();

				HashMap<String, LableLanDisplay> hash = new HashMap<String, LableLanDisplay>();

				if (true) {
					LableLanDisplay s = new LableLanDisplay();
					s.setLabelId(labelId);
					QueryListInfo<LableLanDisplay> dlist = this.service.getMgClient().getList(s, "lan",this.service);
					for (LableLanDisplay x : dlist.getList()) {
						hash.put(x.getLan(), x);
					}
				}

				if (true) {
					for (PLanguage x : llist.getList()) {
						LableLanDisplay dis = hash.get(x.getLan());
						if (dis == null) {
							dis = new LableLanDisplay();
							dis.setLabelId(labelId);
							dis.setClassName(PRoot.class.getName());
						}
						dis.setLan(x.getLan());
						dis.setRemark(x.getRemark());
						lanDisList.getList().add(dis);
					}
				}

				super.listToJson(lanDisList, json, LableLanDisplay.class.getSimpleName(),
						BosConstants.getTable(LableLanDisplay.class));

			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "菜单的多语种设置保存", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_tech_manager+","+BosConstants.role_db_manager)
	@RequestMapping(value = "/menuListForLanSave", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject menuListForLanSave(@RequestBody HttpEntity json, HttpServletRequest request) throws Exception {

		// Constants.debug("***************1\n"+json.stringValue());

		try {


			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String sys = json.getSelectedId(Constants.current_sys, "/root/menuListForLan", PRoot.class.getName(), null,
					true, this.getService());

			PRoot obj = this.service.getMgClient().getById(sys, PRoot.class);
			if (obj == null) {
				json.setUnSuccessForNoRecord(PRoot.class);
				return json.jsonValue();
			}

			PLanguage lan = new PLanguage();
			lan.setStatus(1L);
			QueryListInfo<PLanguage> llist = this.service.getMgClient().getList(lan, "seq",this.service);
			if (llist.size() <= 0) {
				json.setUnSuccess(-1, "系统未配置多语言");
				return json.jsonValue();
			}

			if (true) {
				// 保存子系统显示名的多语言设置
				String labelId = PRoot.class.getSimpleName() + "_" + obj.getPk() + "_displayName";
				List<BosEntity> labelList = json.getList(LableLanDisplay.class.getSimpleName(),
						LableLanDisplay.class.getName(), "lan,!display", null, 0, this.service);
				for (BosEntity x : labelList) {
					x.putValue("labelId", labelId);
					x.putValue("className", PRoot.class.getName());
					String display = x.propertyValueString("display");
					if (DataChange.isEmpty(display)) {
						this.service.getMgClient().deleteByPK(x.getPk(), LableLanDisplay.class.getName(),ut,true,this.service);
					} else {
						this.service.save(x, ut);
					}
				}
				BosConstants.getExpireHash().removeJSONObject(LableLanDisplay.class.getSimpleName() + "_" + labelId);
				new ClassInnerNotice().invoke(LableLanDisplay.class.getSimpleName(), labelId);
			}

			if (true) {

				// 保存字段的多语言设置
				for (PLanguage x : llist.getList()) {

					List<BosEntity> fList = json.getList(PServiceUri.class.getSimpleName() + x.getLan(),
							PServiceUri.class.getName(), "uri,!displayName,!menu", null, 0, this.service);
					for (BosEntity f : fList) {
						f.putMustValue("sys", sys);

						Vector<String> v = Field.split("menu");// displayName,

						for (String x1 : v) {

							String value = f.propertyValueString(x1);
							String labelId = PServiceUri.class.getSimpleName() + "_" + f.getPk() + "_" + x1;
							LableLanDisplay display = new LableLanDisplay();
							display.setLabelId(labelId);
							display.setLan(x.getLan());
							if (!DataChange.isEmpty(value)) {
								display.setClassName(PServiceUri.class.getName());
								display.setDisplay(value);
								this.service.save(display, ut);
							} else {
								this.service.getMgClient().deleteByPK(display.getPk(), LableLanDisplay.class,ut,this.service);
							}

							BosConstants.getExpireHash()
									.removeJSONObject(LableLanDisplay.class.getSimpleName() + "_" + labelId);
							new ClassInnerNotice().invoke(LableLanDisplay.class.getSimpleName(), labelId);
						}

					}

				}

			}

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}

}
