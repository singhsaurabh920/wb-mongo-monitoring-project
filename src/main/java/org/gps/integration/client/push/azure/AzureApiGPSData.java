package org.gps.integration.client.push.azure;

public class AzureApiGPSData {
	private String location;
	private String latitude;
	private String longitude;
	private String speed;
	private String altitude;
	private String gpsStatus;
	private String satellites;
	private String networkInfo;
	private String gpsDateTime;
	
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
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
	public String getSpeed() {
		return speed;
	}
	public void setSpeed(String speed) {
		this.speed = speed;
	}
	public String getAltitude() {
		return altitude;
	}
	public void setAltitude(String altitude) {
		this.altitude = altitude;
	}
	public String getGpsStatus() {
		return gpsStatus;
	}
	public void setGpsStatus(String gpsStatus) {
		this.gpsStatus = gpsStatus;
	}
	public String getSatellites() {
		return satellites;
	}
	public void setSatellites(String satellites) {
		this.satellites = satellites;
	}
	public String getNetworkInfo() {
		return networkInfo;
	}
	public void setNetworkInfo(String networkInfo) {
		this.networkInfo = networkInfo;
	}
	public String getGpsDateTime() {
		return gpsDateTime;
	}
	public void setGpsDateTime(String gpsDateTime) {
		this.gpsDateTime = gpsDateTime;
	}
}
