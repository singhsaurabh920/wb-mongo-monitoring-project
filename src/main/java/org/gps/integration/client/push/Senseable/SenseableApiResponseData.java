package org.gps.integration.client.push.Senseable;

public class SenseableApiResponseData {
	
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
	
	
	@Override
	public String toString() {
		return "SenseableApiResponseData [success=" + success + ", reason=" + reason + "]";
	}
	
}
