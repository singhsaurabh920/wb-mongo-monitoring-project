package org.gps.integration.client.push.srtms;

import java.util.List;

public class SrtmsApiResponseData {
	private String status;
	List<String> invalid_coordinate_index;
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public List<String> getInvalid_coordinate_index() {
		return invalid_coordinate_index;
	}
	public void setInvalid_coordinate_index(List<String> invalid_coordinate_index) {
		this.invalid_coordinate_index = invalid_coordinate_index;
	}
	
	@Override
	public String toString() {
		return "SrtmsApiResponseData [status=" + status + ", invalid_coordinate_index=" + invalid_coordinate_index
				+ "]";
	}
	
	

}
