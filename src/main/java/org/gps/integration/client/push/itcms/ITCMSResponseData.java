package org.gps.integration.client.push.itcms;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ITCMSResponseData {
	@JsonProperty("IsSuccess")
	private boolean isSuccess;
	@JsonProperty("Data")
	private String data;
	@JsonProperty("Message")
	private String message;
	
	public boolean isSuccess() {
		return isSuccess;
	}
	public void setSuccess(boolean isSuccess) {
		this.isSuccess = isSuccess;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	@Override
	public String toString() {
		return "ITCMSResponseData [isSuccess=" + isSuccess + ", data=" + data + ", message=" + message + "]";
	}
	
}
