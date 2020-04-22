package org.gps.integration.client.push.nivaata;

public class NivaataApiResponseModel {
	private NivaataResponse response;

	public NivaataResponse getResponse() {
		return response;
	}

	public void setResponse(NivaataResponse response) {
		this.response = response;
	}

	@Override
	public String toString() {
		return response.toString();
	}

}
