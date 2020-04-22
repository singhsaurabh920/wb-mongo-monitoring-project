package org.gps.integration.client.push.azure;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AzureApiResponseData {
	@JsonProperty("IsSuccess")
	private boolean isSuccess;
	@JsonProperty("Message")
	private String message;
	@JsonProperty("VehicleID")
	private String vehicleID;
	@JsonProperty("DeviceID")
	private String deviceID;
	
	public boolean isSuccess() {
		return isSuccess;
	}
	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
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
	@Override
	public String toString() {
		return "AzureApiResponseData [isSuccess=" + isSuccess + ", message=" + message + ", vehicleID=" + vehicleID
				+ ", deviceID=" + deviceID + "]";
	}
	
}
