package org.gps.integration.client.push.mahamining;

public class MahaMiningApiRequestModel {
	private long unit;
	private String reason;
	private String commandKey;
	private String commandkeyValue;
	private int ignition;
	private int powerCut;
	private int boxOpen;
	private String msgKey;
	private long odometer;
	private int speed;
	private int satVisible;
	private String gpsFixed;
	private String latitude;
	private String longitude;
	private int altitude;
	private int direction;
	private String time;
	private String date;
	private int gsmStrength;
	private int deviceCompanyId;
	
	public long getUnit() {
		return unit;
	}
	public void setUnit(long unit) {
		this.unit = unit;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	public String getCommandKey() {
		return commandKey;
	}
	public void setCommandKey(String commandKey) {
		this.commandKey = commandKey;
	}
	public String getCommandkeyValue() {
		return commandkeyValue;
	}
	public void setCommandkeyValue(String commandkeyValue) {
		this.commandkeyValue = commandkeyValue;
	}
	public int getIgnition() {
		return ignition;
	}
	public void setIgnition(int ignition) {
		this.ignition = ignition;
	}
	public int getPowerCut() {
		return powerCut;
	}
	public void setPowerCut(int powerCut) {
		this.powerCut = powerCut;
	}
	public int getBoxOpen() {
		return boxOpen;
	}
	public void setBoxOpen(int boxOpen) {
		this.boxOpen = boxOpen;
	}
	public String getMsgKey() {
		return msgKey;
	}
	public void setMsgKey(String msgKey) {
		this.msgKey = msgKey;
	}
	public long getOdometer() {
		return odometer;
	}
	public void setOdometer(long odometer) {
		this.odometer = odometer;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public int getSatVisible() {
		return satVisible;
	}
	public void setSatVisible(int satVisible) {
		this.satVisible = satVisible;
	}
	public String getGpsFixed() {
		return gpsFixed;
	}
	public void setGpsFixed(String gpsFixed) {
		this.gpsFixed = gpsFixed;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	public int getAltitude() {
		return altitude;
	}
	public void setAltitude(int altitude) {
		this.altitude = altitude;
	}
	public int getDirection() {
		return direction;
	}
	public void setDirection(int direction) {
		this.direction = direction;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public int getGsmStrength() {
		return gsmStrength;
	}
	public void setGsmStrength(int gsmStrength) {
		this.gsmStrength = gsmStrength;
	}
	public int getDeviceCompanyId() {
		return deviceCompanyId;
	}
	public void setDeviceCompanyId(int deviceCompanyId) {
		this.deviceCompanyId = deviceCompanyId;
	}

}