package org.gps.integration.client.push.nivaata;

public class NivaataLocationModel {
	
	private long ts;
	//
	private double lat;
	private double lng;
	//
	private int ign;
	private int speed;
	private int accuracy;
	
	public long getTs() {
		return ts;
	}
	public void setTs(long ts) {
		this.ts = ts;
	}
	public double getLat() {
		return lat;
	}
	public void setLat(double lat) {
		this.lat = lat;
	}
	public double getLng() {
		return lng;
	}
	public void setLng(double lng) {
		this.lng = lng;
	}
	public int getIgn() {
		return ign;
	}
	public void setIgn(int ign) {
		this.ign = ign;
	}
	public int getSpeed() {
		return speed;
	}
	public void setSpeed(int speed) {
		this.speed = speed;
	}
	public int getAccuracy() {
		return accuracy;
	}
	public void setAccuracy(int accuracy) {
		this.accuracy = accuracy;
	}
	

}
