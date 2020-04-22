package org.gps.integration.client.push.azure;

public class AzureApiRequestData {

	private String vehicleID;
	private String deviceID;
	private String customerId;
	private String provider;
	private String dateTime;
	private AzureApiGPSData gpsData;
	
	public String getVehicleID() {
		return vehicleID;
	}
	public void setVehicleID(String vehicleID) {
		this.vehicleID = vehicleID;
	}
	public String getDeviceID() {
		return deviceID;
	}
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public String getProvider() {
		return provider;
	}
	public void setProvider(String provider) {
		this.provider = provider;
	}
	public String getDateTime() {
		return dateTime;
	}
	public void setDateTime(String dateTime) {
		this.dateTime = dateTime;
	}
	public AzureApiGPSData getGpsData() {
		return gpsData;
	}
	public void setGpsData(AzureApiGPSData gpsData) {
		this.gpsData = gpsData;
	}
}
