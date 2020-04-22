package org.gps.integration.client.push.Senseable;

import java.util.List;

public class SenseableApiData {

	private List<SenseableApiRequestModel> data;

	public List<SenseableApiRequestModel> getData() {
		return data;
	}

	public void setData(List<SenseableApiRequestModel> data) {
		this.data = data;
	}

	@Override
	public String toString() {
		return "SenseableApiData [data=" + data + "]";
	}
}
