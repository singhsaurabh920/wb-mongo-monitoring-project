package org.gps.integration.client.push.fareye;

public class FareyeResponseData {
	private boolean success;
	private String reason;
	
	public boolean isSuccess() {
		return success;
	}
	public void setSuccess(boolean success) {
		this.success = success;
	}
	public String getReason() {
		return reason;
	}
	public void setReason(String reason) {
		this.reason = reason;
	}
	
}
