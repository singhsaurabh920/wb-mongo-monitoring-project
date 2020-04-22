package org.gps.integration.client.push.nivaata;

import java.util.List;

public class NivaataApiRequestModel {
	public final String authkey = "e0cb4b2c-2260-536f-ae06-c77fdd36d8fa";
	public final String vendor = "mobilfox";
	public final String version = "2.0";
	private List<NivaataData> data;
	public List<NivaataData> getData() {
		return data;
	}
	public void setData(List<NivaataData> data) {
		this.data = data;
	}
	public String getAuthkey() {
		return authkey;
	}
	public String getVendor() {
		return vendor;
	}
	public String getVersion() {
		return version;
	}
	
	
	

}
