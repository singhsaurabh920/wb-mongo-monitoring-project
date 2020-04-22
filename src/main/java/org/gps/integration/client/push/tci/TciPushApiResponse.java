package org.gps.integration.client.push.tci;

public class TciPushApiResponse {
	private String MESSAGE;
	private String SUCCESS;
	
	public String getMESSAGE() {
		return MESSAGE;
	}
	public void setMESSAGE(String mESSAGE) {
		MESSAGE = mESSAGE;
	}
	public String getSUCCESS() {
		return SUCCESS;
	}
	public void setSUCCESS(String sUCCESS) {
		SUCCESS = sUCCESS;
	}
	@Override
	public String toString() {
		return "TciPushApiResponse [MESSAGE=" + MESSAGE + ", SUCCESS=" + SUCCESS + "]";
	}
	
	
}
