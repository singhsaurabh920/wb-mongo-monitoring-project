package org.gps.integration.client.push.enmovil;

public class EnmovilLoginSuccessData {
	private boolean auth;
	private String token;
	
	public boolean isAuth() {
		return auth;
	}
	public void setAuth(boolean auth) {
		this.auth = auth;
	}
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	
	@Override
	public String toString() {
		return "EnmovilLoginSuccessData [auth=" + auth + ", token=" + token + "]";
	}
	
}
