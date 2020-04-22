package org.gps.integration.client.push.srtms;

public class SrtmsApiRequestData {
	private String deviceIMEI;
	private long timestamp;
	private double latitude;
	private double longitude;
	private int speed;
	private long distanceCovered;
	private String ignitionStatus;
	
	public String getDeviceIMEI() {
		return deviceIMEI;
	}
	public void setDeviceIMEI(String deviceIMEI) {
		this.deviceIMEI = deviceIMEI;
	}
	public long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
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
	public void setLongitude(float longitude) {
		this.longitude = longitude;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public long getDistanceCovered() {
		return distanceCovered;
	}
	public void setDistanceCovered(long distanceCovered) {
		this.distanceCovered = distanceCovered;
	}
	public String getIgnitionStatus() {
		return ignitionStatus;
	}
	public void setIgnitionStatus(String ignitionStatus) {
		this.ignitionStatus = ignitionStatus;
	}
	@Override
	public String toString() {
		return "SrtmsApiRequestData [deviceIMEI=" + deviceIMEI + ", timestamp=" + timestamp + ", latitude=" + latitude
				+ ", longitude=" + longitude + ", speed=" + speed + ", distanceCovered=" + distanceCovered
				+ ", ignitionStatus=" + ignitionStatus + "]";
	}
	
}
