package org.gps.integration.client.push.enmovil;

public class EnmovilApiResponse {
	private Boolean auth;
	private String message;
	private String result;
	
	public Boolean getAuth() {
		return auth;
	}
	public void setAuth(Boolean auth) {
		this.auth = auth;
	}
	
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	
	@Override
	public String toString() {
		return "EnmovilApiResponse [auth=" + auth + ", message=" + message + ", result=" + result + "]";
	}
	
}
