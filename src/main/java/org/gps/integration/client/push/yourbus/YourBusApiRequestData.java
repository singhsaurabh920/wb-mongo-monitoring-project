package org.gps.integration.client.push.yourbus;

public class YourBusApiRequestData {
	private String gpsId;
	private long timestamp;
	private float lat;
	private float lng;
	private int speed;
	private int acStatus;
	private int orientation;
	private int ignitionStatus;
	private String address;

	public String getGpsId() {
		return gpsId;
	}

	public void setGpsId(String gpsId) {
		this.gpsId = gpsId;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public float getLat() {
		return lat;
	}

	public void setLat(float lat) {
		this.lat = lat;
	}

	public float getLng() {
		return lng;
	}

	public void setLng(float lng) {
		this.lng = lng;
	}

	public int getSpeed() {
		return speed;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	public int getAcStatus() {
		return acStatus;
	}

	public void setAcStatus(int acStatus) {
		this.acStatus = acStatus;
	}

	public int getOrientation() {
		return orientation;
	}

	public void setOrientation(int orientation) {
		this.orientation = orientation;
	}

	public int getIgnitionStatus() {
		return ignitionStatus;
	}

	public void setIgnitionStatus(int ignitionStatus) {
		this.ignitionStatus = ignitionStatus;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "YourBusApiRequestData [gpsId=" + gpsId + ", timestamp=" + timestamp + ", lat=" + lat + ", lng=" + lng + ", speed=" + speed + ", acStatus=" + acStatus + ", orientation=" + orientation + ", ignitionStatus="+ ignitionStatus + ", address=" + address + "]";
	}
	

}
