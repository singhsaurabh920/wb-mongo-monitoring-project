package org.gps.integration.client.push.Senseable;

public class SenseableApiRequestModel {
	
	private String vehicle_no;
	private String imei;
	private double latitude;
	private double longitude;
	private double speed;
	private String timestamp;
	private String ignitionStatus;
	private int satellites;
	private int gsmSignals;
	
	public String getVehicle_no() {
		return vehicle_no;
	}
	public void setVehicle_no(String vehicle_no) {
		this.vehicle_no = vehicle_no;
	}
	public String getImei() {
		return imei;
	}
	public void setImei(String imei) {
		this.imei = imei;
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
	public String getIgnitionStatus() {
		return ignitionStatus;
	}
	public void setIgnitionStatus(String ignitionStatus) {
		this.ignitionStatus = ignitionStatus;
	}
	public int getSatellites() {
		return satellites;
	}
	public void setSatellites(int satellites) {
		this.satellites = satellites;
	}
	public int getGsmSignals() {
		return gsmSignals;
	}
	public void setGsmSignals(int gsmSignals) {
		this.gsmSignals = gsmSignals;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SenseableApiRequestModel [vehicle_no=").append(vehicle_no).append(", imei=").append(imei)
				.append(", latitude=").append(latitude).append(", longitude=").append(longitude).append(", speed=")
				.append(speed).append(", timestamp=").append(timestamp).append(", ignitionStatus=")
				.append(ignitionStatus).append(", satellites=").append(satellites).append(", gsmSignals=")
				.append(gsmSignals).append("]");
		return builder.toString();
	}

}