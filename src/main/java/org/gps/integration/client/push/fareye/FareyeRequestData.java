package org.gps.integration.client.push.fareye;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FareyeRequestData {
	private String vehicle_no;
	private double latitude;
	private double longitude;
	private double speed;
	private String timestamp;
	private String ignition_status;
	
	public String getVehicle_no() {
		return vehicle_no;
	}
	public void setVehicle_no(String vehicle_no) {
		this.vehicle_no = vehicle_no;
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
	public double getSpeed() {
		return speed;
	}
	public void setSpeed(double speed) {
		this.speed = speed;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}
	public String getIgnition_status() {
		return ignition_status;
	}
	public void setIgnition_status(String ignition_status) {
		this.ignition_status = ignition_status;
	}
	@Override
	public String toString() {
		return "FareyeRequestData [vehicle_no=" + vehicle_no + ", latitude=" + latitude + ", longitude=" + longitude
				+ ", speed=" + speed + ", timestamp=" + timestamp + "]";
	}
	
	
}
