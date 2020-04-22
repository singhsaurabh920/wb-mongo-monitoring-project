package org.gps.integration.client.push.yourbus;

import java.util.List;

public class YourBusApiResponseData {
	private int code;
	private String status;
	private String message;
	private List<YourBusApiErrorData> data;
	private String error;
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public List<YourBusApiErrorData> getData() {
		return data;
	}
	public void setData(List<YourBusApiErrorData> data) {
		this.data = data;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}
	@Override
	public String toString() {
		return "YourBusApiResponseData [code=" + code + ", status=" + status + ", message=" + message + ", data=" + data+ ", error=" + error + "]";
	}
	
	

}
