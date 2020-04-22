package org.gps.integration.client.push.wetrack;

public class WeTrackApiRequestData {
	private String vehiclenumber;
	private String imei;
	private String latitude;
	private String longitude;
	private String odometer;
	private String gpstime;
	private String speed;
	private String angle;
	private String ignition;
	
	public String getVehiclenumber() {
		return vehiclenumber;
	}
	public void setVehiclenumber(String vehiclenumber) {
		this.vehiclenumber = vehiclenumber;
	}
	public String getImei() {
		return imei;
	}
	public void setImei(String imei) {
		this.imei = imei;
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
	public String getOdometer() {
		return odometer;
	}
	public void setOdometer(String odometer) {
		this.odometer = odometer;
	}
	public String getGpstime() {
		return gpstime;
	}
	public void setGpstime(String gpstime) {
		this.gpstime = gpstime;
	}
	public String getSpeed() {
		return speed;
	}
	public void setSpeed(String speed) {
		this.speed = speed;
	}
	public String getAngle() {
		return angle;
	}
	public void setAngle(String angle) {
		this.angle = angle;
	}
	public String getIgnition() {
		return ignition;
	}
	public void setIgnition(String ignition) {
		this.ignition = ignition;
	}
	
}
