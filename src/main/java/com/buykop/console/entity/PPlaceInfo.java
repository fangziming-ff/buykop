package com.buykop.console.entity;

import com.buykop.framework.annotation.CodeValue;
import com.buykop.framework.annotation.Column;
import com.buykop.framework.annotation.Display;
import com.buykop.framework.annotation.ExColumn;
import com.buykop.framework.annotation.FK;
import com.buykop.framework.annotation.Mongo;
import com.buykop.framework.annotation.PK;
import com.buykop.framework.annotation.Parent;
import com.buykop.framework.annotation.Sort;
import com.buykop.framework.annotation.per.MemberId;
import com.buykop.framework.annotation.per.OrgId;
import com.buykop.framework.annotation.per.UserId;
import com.buykop.framework.annotation.util.DBLen;
import com.buykop.framework.scan.Timezone;
import com.buykop.framework.util.BosConstants;
import com.buykop.framework.util.type.Entity;
import com.buykop.console.util.Constants;



/**
 * TABLE:T_PLACE_INFO
 * 
 * @author POWERBOS ADP
 * 
 */
@Mongo(code = PPlaceInfo.table, sys = Constants.current_sys, display = "行政区域", defaultOrderByField = "", retainDays = 0)
public class PPlaceInfo extends Entity {

	protected static final String table = "T_PLACE_INFO";

	/**
	 * 
	 */
	private static final long serialVersionUID = -5537778783132939635L;

	@PK
	@Column(dbLen = DBLen.idLen, displayName = "地区", isNotNull = false, label = "")
	protected String placeId;//

	@Display
	@Column(dbLen = DBLen.len256, displayName = "地区名称", isNotNull = true, label = "",fieldType=2)
	protected String placeName;//

	@Column(dbLen = DBLen.len32, displayName = "地区编码", isNotNull = false, label = "",fieldType=2)
	protected String placeCode;

	@CodeValue(codeType = "45")
	@Column(dbLen = DBLen.len2, displayName = "级别", isNotNull = false, label = "")
	protected Long levelType;//-1:洲 0:国家 1:地区(华东、华北)  2:省  3:地市  4:县区 5:乡镇街道[大网格] 6:社区(村)[中网格] 7:楼院、村组、单位场所 [小网格]

	@Parent
	@Column(dbLen = DBLen.idLen, displayName = "上级地区", isNotNull = false, label = "", defaultValue = "0")
	@FK(table = PPlaceInfo.class, display = "")
	protected String parentId;//

	@Column(dbLen = DBLen.len32, displayName = "邮政编码", isNotNull = false, label = "",fieldType=2)
	protected String zipcode;//

	@Column(dbLen = DBLen.len32, displayName = "车牌", isNotNull = false, label = "")
	protected String carHead;

	@Column(dbLen = DBLen.len32, displayName = "区号", isNotNull = false, label = "",fieldType=2)
	protected String telcode;//

	@Column(dbLen = DBLen.len64, displayName = "外部编码", isNotNull = false, label = "")
	private String outKey;

	@CodeValue(codeType = "3")
	@Column(dbLen = DBLen.len1, displayName = "是否乡村", isNotNull = true, label = "", defaultValue = "0")
	protected Long countrySide;// 1:乡村

	@Sort
	@Column(dbLen = DBLen.len8, displayName = "排序", isNotNull = false, label = "")
	private Long seq;

	@Column(dbLen = "9,6", displayName = "经度坐标", isNotNull = false, label = "")
	private java.math.BigDecimal x;

	@Column(dbLen = "9,6", displayName = "纬度坐标", isNotNull = false, label = "")
	private java.math.BigDecimal y;

	@MemberId
	@Column(dbLen = DBLen.idLen, displayName = "负责机构", isNotNull = false, label = "", bindingFormId = BosConstants.LIST_ALL_MEMBER)
	protected String chargeMember;// 负责机构

	@OrgId
	@Column(dbLen = DBLen.idLen, displayName = "负责部门", isNotNull = false, label = "", parent = "chargeMember")
	protected String chargeOrg;// 负责人

	@UserId
	@Column(dbLen = DBLen.idLen, displayName = "负责人", isNotNull = false, label = "", parent = "chargeOrg")
	protected String chargeUser;// 负责人
	
	
	@Column(dbLen = DBLen.idLen, displayName = "法定币种", isNotNull = false, label = "")
	private String currency;
	
	
	@FK(table=Timezone.class)
	@Column(dbLen = DBLen.idLen, displayName = "所属时区", isNotNull = false, label = "",bindingFormId=BosConstants.LIST_ALL_TIMEZONE)
	private String timeZone;
	

	@CodeValue(codeType = "3")
	@ExColumn(dbLen = DBLen.len1, displayName = "开通服务", label = "")
	protected Long openService;

	@ExColumn(dbLen = DBLen.len8, displayName = "距离", label = "")
	protected Long distance;

	public String getChargeMember() {
		return chargeMember;
	}

	public void setChargeMember(String chargeMember) {
		this.chargeMember = chargeMember;
	}

	public String getChargeOrg() {
		return chargeOrg;
	}

	public void setChargeOrg(String chargeOrg) {
		this.chargeOrg = chargeOrg;
	}

	public String getChargeUser() {
		return chargeUser;
	}

	public void setChargeUser(String chargeUser) {
		this.chargeUser = chargeUser;
	}

	public Long getCountrySide() {
		return countrySide;
	}

	public void setCountrySide(Long countrySide) {
		this.countrySide = countrySide;
	}

	public String getPlaceId() {
		return placeId;
	}

	public void setPlaceId(String placeId) {
		this.placeId = placeId;
	}

	public String getPlaceName() {
		return placeName;
	}

	public void setPlaceName(String placeName) {
		this.placeName = placeName;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getZipcode() {
		return zipcode;
	}

	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}

	public String getTelcode() {
		return telcode;
	}

	public void setTelcode(String telcode) {
		this.telcode = telcode;
	}

	public String getPlaceCode() {
		return placeCode;
	}

	public void setPlaceCode(String placeCode) {
		this.placeCode = placeCode;
	}

	public String getOutKey() {
		return outKey;
	}

	public void setOutKey(String outKey) {
		this.outKey = outKey;
	}

	public Long getDistance() {
		return distance;
	}

	public void setDistance(Long distance) {
		this.distance = distance;
	}

	public Long getLevelType() {
		return levelType;
	}

	public void setLevelType(Long levelType) {
		this.levelType = levelType;
	}

	public Long getSeq() {
		return seq;
	}

	public void setSeq(Long seq) {
		this.seq = seq;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public java.math.BigDecimal getX() {
		return x;
	}

	public void setX(java.math.BigDecimal x) {
		this.x = x;
	}

	public java.math.BigDecimal getY() {
		return y;
	}

	public void setY(java.math.BigDecimal y) {
		this.y = y;
	}

	public String getCarHead() {
		return carHead;
	}

	public void setCarHead(String carHead) {
		this.carHead = carHead;
	}

	public Long getOpenService() {
		return openService;
	}

	public void setOpenService(Long openService) {
		this.openService = openService;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}
	
	

}
