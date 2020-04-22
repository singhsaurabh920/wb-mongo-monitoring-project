package org.gps.integration.client.push.minda;

public class MindaApiRequestModel {
	private String vehicleUIN;
	private int speed;
	private double distanceCovered;
	private long timestamp;
	private double latitude;
	private double longitude;
	private String ignitionStatus;
	public String getVehicleUIN() {
		return vehicleUIN;
	}
	public void setVehicleUIN(String vehicleUIN) {
		this.vehicleUIN = vehicleUIN;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	
	public double getDistanceCovered() {
		return distanceCovered;
	}
	public void setDistanceCovered(double distanceCovered) {
		this.distanceCovered = distanceCovered;
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
	
	public String getIgnitionStatus() {
		return ignitionStatus;
	}
	public void setIgnitionStatus(String ignitionStatus) {
		this.ignitionStatus = ignitionStatus;
	}
	
	
	
	
	

}
