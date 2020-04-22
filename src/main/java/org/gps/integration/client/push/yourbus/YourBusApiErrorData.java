package org.gps.integration.client.push.yourbus;

public class YourBusApiErrorData {
	private YourBusApiRequestData packet;
	private String error;

	public YourBusApiRequestData getPacket() {
		return packet;
	}

	public void setPacket(YourBusApiRequestData packet) {
		this.packet = packet;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	@Override
	public String toString() {
		return "YourBusApiErrorData [packet=" + packet + ", error=" + error + "]";
	}
	
	

}
