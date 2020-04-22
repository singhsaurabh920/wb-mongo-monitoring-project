package org.gps.integration.client.push.fretron;

public class FretronApiRequestModel {
	private String vehicle;
	private String imei;
	private String gpsName;
	private long time;
	private double latitude;
	private double longitude;
	private String venderName;
	public String getVehicle() {
		return vehicle;
	}
	public void setVehicle(String vehicle) {
		this.vehicle = vehicle;
	}
	public String getImei() {
		return imei;
	}
	public void setImei(String imei) {
		this.imei = imei;
	}
	public String getGpsName() {
		return gpsName;
	}
	public void setGpsName(String gpsName) {
		this.gpsName = gpsName;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public String getVenderName() {
		return venderName;
	}
	public void setVenderName(String venderName) {
		this.venderName = venderName;
	}
}
