package com.buykop.console.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.alibaba.fastjson.JSONObject;
import com.buykop.console.entity.LoginLog;
import com.buykop.console.service.FileUploadService;
import com.buykop.console.util.Constants;
import com.buykop.framework.aliyun.AliyunOSSUtil;
import com.buykop.framework.aliyun.POSSConfig;
import com.buykop.framework.annotation.Menu;
import com.buykop.framework.annotation.Module;
import com.buykop.framework.annotation.Security;
import com.buykop.framework.entity.FileUpload;
import com.buykop.framework.http.HttpEntity;
import com.buykop.framework.http.NetWorkTime;
import com.buykop.framework.http.PageInfo;
import com.buykop.framework.log.Logger;
import com.buykop.framework.log.LoggerFactory;
import com.buykop.framework.oauth2.UserToken;
import com.buykop.framework.redis.JedisPool;
import com.buykop.framework.redis.RdClient;
import com.buykop.framework.scan.PSysParam;
import com.buykop.framework.scan.Table;
import com.buykop.framework.util.CacheTools;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.SFTPUtil;
import com.buykop.framework.util.data.DataChange;
import com.buykop.framework.util.data.MyString;
import com.buykop.framework.util.type.BaseController;
import com.buykop.framework.util.type.QueryFetchInfo;
import com.buykop.framework.util.type.QueryListInfo;
import com.buykop.framework.util.type.SelectBidding;
import com.buykop.framework.util.type.ServiceInf;

@Module(display = "文件", sys = Constants.current_sys)
@RestController
@RequestMapping("/file")
public class FileController extends BaseController {

	private static Logger logger = LoggerFactory.getLogger(FileController.class);

	@Autowired
	private FileUploadService service;

	@Security(accessType = "1*,2*", displayName = "列表for机构", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/fetchForMember", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetchForMember(@RequestBody HttpEntity json, HttpServletRequest request,@RequestHeader String token) throws Exception {

		try {

			json.setSys(Constants.current_sys);
			json.setUri("/file/fetchForMember");
			if(DataChange.isEmpty(json.getTokenKey())) {
				json.setTokenKey(token);
			}
			

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			FileUpload search = json.getSearch(FileUpload.class, null, ut, this.service);
			search.setOwnerMemberId(ut.getMemberId());
			PageInfo page = json.getPageInfo(FileUpload.class);

			QueryFetchInfo<FileUpload> fetch = this.service.getFetch(search, "!createTime", page.getCurrentPage(),
					page.getPageSize());
			for (FileUpload x : fetch.getList()) {
				if (DataChange.isEmpty(x.getUrl())) {
					x.setUrl(x.initFileUrl());
				}
			}

			super.fetchToJson(fetch, json, FileUpload.class.getSimpleName(), search.showTable().listDBFields(false),
					BosConstants.getTable(FileUpload.class));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1*,2*", displayName = "列表for个人", needLogin = true, isEntAdmin = false, isSysAdmin = false)
	@RequestMapping(value = "/fetchForMe", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetchForMe(@RequestBody HttpEntity json, HttpServletRequest request,@RequestHeader String token) throws Exception {

		try {

			json.setSys(Constants.current_sys);
			json.setUri("/file/fetchForMe");
			if(DataChange.isEmpty(json.getTokenKey())) {
				json.setTokenKey(token);
			}
			
			
			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			FileUpload search = json.getSearch(FileUpload.class, null, ut, this.service);
			search.setOwnerUserId(ut.getUserId());
			PageInfo page = json.getPageInfo(FileUpload.class);

			QueryFetchInfo<FileUpload> fetch = this.service.getFetch(search, "!createTime", page.getCurrentPage(),
					page.getPageSize());
			for (FileUpload x : fetch.getList()) {
				if (DataChange.isEmpty(x.getUrl())) {
					x.setUrl(x.initFileUrl());
				}
			}
			super.fetchToJson(fetch, json, FileUpload.class.getSimpleName(), search.showTable().listDBFields(false),
					BosConstants.getTable(FileUpload.class));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Menu(js = "file", name = "文件查询", trunk = "基础信息,数据管理")
	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_oisAdmin)
	@RequestMapping(value = "/fetch", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject fetch(@RequestBody HttpEntity json, HttpServletRequest request,@RequestHeader String token) throws Exception {

		
		json.setSys(Constants.current_sys);
		json.setUri("/file/fetch");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		
		
		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			FileUpload search = json.getSearch(FileUpload.class, null, ut, this.service);
			PageInfo page = json.getPageInfo(FileUpload.class);

			QueryFetchInfo<FileUpload> fetch = this.service.getFetch(search, "!createTime", page.getCurrentPage(),
					page.getPageSize());
			for (FileUpload x : fetch.getList()) {
				if (DataChange.isEmpty(x.getUrl())) {
					x.setUrl(x.initFileUrl());
				}
			}

			super.fetchToJson(fetch, json, BosConstants.getTable(FileUpload.class));

			json.setSuccess();
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "列表", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_oisAdmin)
	@RequestMapping(value = "/saveList", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject saveList(@RequestBody HttpEntity json, HttpServletRequest request,@RequestHeader String token) throws Exception {

		
		json.setSys(Constants.current_sys);
		json.setUri("/file/saveList");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		
		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			List<FileUpload> list = json.getList(FileUpload.class, "fileId,!folderId", this.service);
			for (FileUpload x : list) {
				this.service.save(x,ut);
			}

			json.setSuccess("保存成功");
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "详情", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_oisAdmin)
	@RequestMapping(value = "/info", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject info(@RequestBody HttpEntity json, HttpServletRequest request,@RequestHeader String token) throws Exception {

		
		
		json.setSys(Constants.current_sys);
		json.setUri("/file/info");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		
		
		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/file/info", FileUpload.class.getName(), null,this.getService());

			if (!DataChange.isEmpty(id)) {

				FileUpload obj = this.service.getById(id, FileUpload.class);

				if (obj == null) {
					json.setUnSuccessForNoRecord(FileUpload.class,id);
					return json.jsonValue();
				} else {
					super.objToJson(obj, json);
					json.setSuccess();
				}

			} else {
				json.setUnSuccessForParamNotIncorrect();
				return json.jsonValue();
			}

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	@Security(accessType = "1", displayName = "删除", needLogin = true, isEntAdmin = false, isSysAdmin = false, roleId = BosConstants.role_oisAdmin)
	@RequestMapping(value = "/delete", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject delete(@RequestBody HttpEntity json, HttpServletRequest request,@RequestHeader String token) throws Exception {

		
		
		json.setSys(Constants.current_sys);
		json.setUri("/file/delete");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		
		
		try {

			UserToken ut = super.securityCheck(json, request);

			if (ut == null) {
				return json.jsonValue();
			}

			String id = json.getSelectedId(Constants.current_sys, "/file/delete", FileUpload.class.getName(), null,this.getService());

			if (!DataChange.isEmpty(id)) {
				this.service.deleteById(id, FileUpload.class.getName(), ut);
			}

			json.setSuccess("删除成功");
		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		}

		return json.jsonValue();
	}

	/**
	 * 文件上传
	 * 
	 * @param file
	 * @throws Exception
	 * @throws MongoException
	 */
	// @ParamOut(data = { @ParamData(cl = null, display = "", help = "", key = "")
	// }, obj = { })
	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject upload(HttpServletRequest req) throws Exception {

		BosConstants.debug("*************************upload******************************");

		String token=req.getHeader("token");
		
		HttpEntity json = new HttpEntity();
		json.setSys(Constants.current_sys);
		json.setUri("/file/upload");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		
		UserToken ut = super.securityCheck(json, req);

		String storeTpye = DataChange.replaceNull(PSysParam.paramValue("1", "storeTpye"));
		if (DataChange.isEmpty(storeTpye)) {
			storeTpye = "1";
		}
		if (storeTpye.equals("2")) {
			POSSConfig config = this.service.getMgClient().getById(POSSConfig.aliyunId, POSSConfig.class);
			if (config == null) {
				json.setUnSuccess(-1, "系统未启用文件上传功能");
				return json.jsonValue();
			}
			if (!config.isOssOK()) {
				json.setUnSuccess(-1, "系统未启用文件上传功能");
				return json.jsonValue();
			}
		}


		// fileServerIp fileServerPort fileServerDomain, fileServerPwd
		// fileServerRootPath
		JSONObject sftpConfig = CacheTools.getSFTPConfig(false);

		SFTPUtil sftp = null;

		try {

			Map<String, MultipartFile> fs = ((MultipartHttpServletRequest) req).getFileMap();
			Iterator<String> its = fs.keySet().iterator();
			while (its.hasNext()) {

				String key = its.next();

				MultipartFile file = fs.get(key);

				BosConstants.debug("FILES1 name=" + file.getName() + "     originalFilename="
						+ file.getOriginalFilename() + "    size=" + file.getSize());

				if (!"".equals(file.getOriginalFilename())) {

					if (storeTpye.equals("0")) {

						FileUpload upload = new FileUpload();
						upload.setFileId(FileUpload.next());
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
						String dateStr = format.format(NetWorkTime.getCurrentDatetime());
						upload.setPath("upload/" + dateStr);
						upload.setSrcName(file.getOriginalFilename());
						upload.setCreateTime(NetWorkTime.getCurrentDatetime());
						upload.setUpdateTime(NetWorkTime.getCurrentDatetime());
						if (token != null) {
							upload.setCreateUserId(ut.getUserId());
							upload.setUpdateUserId(ut.getUserId());
							upload.setOwnerUserId(ut.getUserId());
							upload.setOwnerMemberId(ut.getMemberId());
						}

						upload.setFileSize(file.getSize());
						String extend = "";
						String fileUrl = upload.getFileId();
						if (file.getOriginalFilename().indexOf(".") != -1) {
							extend = file.getOriginalFilename().substring(
									file.getOriginalFilename().lastIndexOf(".") + 1,
									file.getOriginalFilename().length());
							fileUrl += "." + extend;
						}
						upload.setFileExtend(extend);
						upload.setSaveInDb(0L);

						InputStream is = file.getInputStream();
						FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + File.separator
								+ "upload" + File.separator + dateStr + File.separator + fileUrl);
						byte[] buf = new byte[1024];
						int len = -1;
						while ((len = is.read(buf)) != -1) {
							fos.write(buf, 0, len);
						}

						is.close();
						fos.close();

						this.service.save(upload, ut);

						json.getData().put(key, upload.getFileId());
						json.getData().put("_" + key + "_json", upload._getJson());

					} else if (storeTpye.equals("1")) {

						sftp = new SFTPUtil(sftpConfig.getString("fileServerLogin"),
								sftpConfig.getString("fileServerPwd"),
								sftpConfig.getString(BosConstants.paramFileServerIp),
								DataChange.StringToInteger(sftpConfig.getString("fileServerPort")));

						sftp.login();

						FileUpload upload = new FileUpload();
						upload.setFileId(FileUpload.next());
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
						String dateStr = format.format(NetWorkTime.getCurrentDatetime());
						upload.setPath("upload/" + dateStr);
						upload.setSrcName(file.getOriginalFilename());
						upload.setCreateTime(NetWorkTime.getCurrentDatetime());
						upload.setUpdateTime(NetWorkTime.getCurrentDatetime());
						if (token != null) {
							upload.setCreateUserId(ut.getUserId());
							upload.setUpdateUserId(ut.getUserId());
							upload.setOwnerUserId(ut.getUserId());
							upload.setOwnerMemberId(ut.getMemberId());
						}

						upload.setFileSize(DataChange.intToLong(file.getInputStream().available()));
						String extend = "";
						String fileUrl = upload.getFileId();
						if (file.getOriginalFilename().indexOf(".") != -1) {
							extend = file.getOriginalFilename().substring(
									file.getOriginalFilename().lastIndexOf(".") + 1,
									file.getOriginalFilename().length());
							fileUrl += "." + extend;
						}
						upload.setFileExtend(extend);
						upload.setSaveInDb(1L);

						sftp.upload(file.getInputStream(), sftpConfig.getString("fileServerRootPath"), upload.getPath(),
								fileUrl);
						this.service.save(upload, ut);
						json.getData().put(key, upload.getFileId());
						json.getData().put("_" + key + "_json", upload._getJson());
					} else {

						AliyunOSSUtil util = new AliyunOSSUtil();
						FileUpload upload = util.upload(file.getOriginalFilename(), file.getInputStream(), ut,
								service);
						if (upload != null) {
							this.service.save(upload, ut);
							json.getData().put(key, upload.getFileId());
							json.getData().put("_" + key + "_json", upload._getJson());
						} else {
							json.setUnSuccess(-1, "上传失败");
							return json.jsonValue();
						}
					}

				}
			}

			json.getData().put("link", "1");
			json.setSuccess();

			BosConstants.debug(json.getData().toString());

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		} finally {
			try {
				if (sftp != null)
					sftp.logout();
			} catch (Exception e) {

			}
		}

		return json.jsonValue();

	}

	@RequestMapping(value = "/uploadSingle", method = RequestMethod.POST)
	@ResponseBody
	public JSONObject uploadSingle(HttpServletRequest request, MultipartFile file) throws Exception {

		BosConstants.debug("*************************uploadSingle******************************");

		String token=request.getHeader("token");
		
		HttpEntity json = new HttpEntity();
		json.setSys(Constants.current_sys);
		json.setUri("/file/uploadSingle");
		if(DataChange.isEmpty(json.getTokenKey())) {
			json.setTokenKey(token);
		}
		
		

		UserToken ut = super.securityCheck(json, request);

		String storeTpye = DataChange.replaceNull(PSysParam.paramValue("1", "storeTpye"));
		if (DataChange.isEmpty(storeTpye)) {
			storeTpye = "1";
		}

		if (storeTpye.equals("2")) {
			POSSConfig config = this.service.getMgClient().getById(POSSConfig.aliyunId, POSSConfig.class);
			if (config == null) {
				json.setUnSuccess(-1, "系统未启用文件上传功能");
				return json.jsonValue();
			}
			if (!config.isOssOK()) {
				json.setUnSuccess(-1, "系统未启用文件上传功能");
				return json.jsonValue();
			}
		}

		// fileServerIp fileServerPort fileServerDomain, fileServerPwd
		// fileServerRootPath
		JSONObject sftpConfig = CacheTools.getSFTPConfig(false);

		BosConstants.debug("sftpConfig=" + sftpConfig.toString());

		SFTPUtil sftp = null;


		try {

			// Map<String, MultipartFile> fs = ((MultipartHttpServletRequest)
			// req).getFileMap();
			// Iterator<String> its = fs.keySet().iterator();
			if (true) {

				// MultipartFile file = fs.get(key);

				BosConstants.debug("FILES1 key=file  name=" + file.getName() + "     originalFilename="
						+ file.getOriginalFilename() + "    size=" + file.getSize());

				if (!"".equals(file.getOriginalFilename())) {

					// 0:本地 1:远程(FTP上传) 2:阿里OSS 3:华为云OSS

					if (storeTpye.equals("0")) {

						FileUpload upload = new FileUpload();
						upload.setFileId(FileUpload.next());
	
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
						String dateStr = format.format(NetWorkTime.getCurrentDatetime());
						upload.setPath("upload/" + dateStr);
						upload.setSrcName(file.getOriginalFilename());
						upload.setCreateTime(NetWorkTime.getCurrentDatetime());
						upload.setUpdateTime(NetWorkTime.getCurrentDatetime());
						
						if(ut!=null) {
							upload.setCreateUserId(ut.getUserId());
							upload.setUpdateUserId(ut.getUserId());
							upload.setOwnerUserId(ut.getUserId());
							upload.setOwnerMemberId(ut.getMemberId());
							upload.setOwnerMemberId(ut.getMemberId());
							upload.setOwnerUserId(ut.getUserId());
							upload.setOwnerOisId(ut.getOisId());
						}

						upload.setFileSize(DataChange.intToLong(file.getInputStream().available()));
						String extend = "";
						String fileUrl = upload.getFileId();
						if (file.getOriginalFilename().indexOf(".") != -1) {
							extend = file.getOriginalFilename().substring(
									file.getOriginalFilename().lastIndexOf(".") + 1,
									file.getOriginalFilename().length());
							fileUrl += "." + extend;
						}
						upload.setFileExtend(extend);
						upload.setSaveInDb(0L);

						InputStream is = file.getInputStream();
						FileOutputStream fos = new FileOutputStream(System.getProperty("user.dir") + File.separator
								+ "upload" + File.separator + dateStr + File.separator + fileUrl);
						byte[] buf = new byte[1024];
						int len = -1;
						while ((len = is.read(buf)) != -1) {
							fos.write(buf, 0, len);
						}

						is.close();
						fos.close();

						json.getData().put("file", upload.getFileId());
						json.getData().put("_file_json", upload._getJson());
						upload.setUrl(upload.initFileUrl());
						upload.setUploadTime(NetWorkTime.getCurrentDatetime());
						this.service.save(upload, null);

					} else if (storeTpye.equals("1")) {

						sftp = new SFTPUtil(sftpConfig.getString("fileServerLogin"),
								sftpConfig.getString("fileServerPwd"),
								sftpConfig.getString(BosConstants.paramFileServerIp),
								DataChange.StringToInteger(sftpConfig.getString("fileServerPort")));

						sftp.login();

						FileUpload upload = new FileUpload();
						upload.setFileId(FileUpload.next());
						SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
						String dateStr = format.format(NetWorkTime.getCurrentDatetime());
						upload.setPath("upload/" + dateStr);
						upload.setSrcName(file.getOriginalFilename());
						upload.setCreateTime(NetWorkTime.getCurrentDatetime());
						upload.setUpdateTime(NetWorkTime.getCurrentDatetime());
						if (ut != null) {
							upload.setCreateUserId(ut.getUserId());
							upload.setUpdateUserId(ut.getUserId());
							upload.setOwnerUserId(ut.getUserId());
							upload.setOwnerMemberId(ut.getMemberId());
							upload.setOwnerMemberId(ut.getMemberId());
							upload.setOwnerUserId(ut.getUserId());
							upload.setOwnerOisId(ut.getOisId());
						}

						upload.setFileSize(DataChange.intToLong(file.getInputStream().available()));
						String extend = "";
						String fileUrl = upload.getFileId();
						if (file.getOriginalFilename().indexOf(".") != -1) {
							extend = file.getOriginalFilename().substring(
									file.getOriginalFilename().lastIndexOf(".") + 1,
									file.getOriginalFilename().length());
							fileUrl += "." + extend;
						}
						upload.setFileExtend(extend);
						upload.setSaveInDb(1L);
						sftp.upload(file.getInputStream(), sftpConfig.getString("fileServerRootPath"), upload.getPath(),
								fileUrl);
						upload.setUrl(upload.initFileUrl());
						upload.setUploadTime(NetWorkTime.getCurrentDatetime());
						this.service.save(upload,null);
						json.getData().put("file", upload.getFileId());
						json.getData().put("_file_json", upload._getJson());

					} else {

						AliyunOSSUtil util = new AliyunOSSUtil();
						FileUpload upload = util.upload(file.getOriginalFilename(), file.getInputStream(), ut,
								service);
						if (upload != null) {
							json.getData().put("file", upload.getFileId());
							json.getData().put("_file_json", upload._getJson());
							upload.setCreateTime(NetWorkTime.getCurrentDatetime());
							upload.setUpdateTime(NetWorkTime.getCurrentDatetime());
							if (ut != null) {
								upload.setCreateUserId(ut.getUserId());
								upload.setUpdateUserId(ut.getUserId());
								upload.setOwnerUserId(ut.getUserId());
								upload.setOwnerMemberId(ut.getMemberId());
								upload.setOwnerOisId(ut.getOisId());
							}
							upload.setUrl(upload.initFileUrl());
							upload.setUploadTime(NetWorkTime.getCurrentDatetime());
							this.service.save(upload, null);
						} else {
							json.setUnSuccess(-1, "上传失败");
							return json.jsonValue();
						}
					}

				}
			}

			BosConstants.debug(json.getData().toJSONString());

			json.setSuccess();

		} catch (Exception e) {
			json.setUnSuccess(e);
			return json.jsonValue();
		} finally {
			try {
				if (sftp != null)
					sftp.logout();
			} catch (Exception e) {

			}
		}

		return json.jsonValue();

	}

	public ServiceInf getService() throws Exception {
		// TODO Auto-generated method stub
		return this.service;
	}
}
