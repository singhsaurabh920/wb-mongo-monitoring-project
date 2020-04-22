package org.gps.integration.client.push.mahamining;

import java.util.List;

public class MahaMiningApiResponseData {
	
	private String data;
	private List<MahaMiningResponseId> data1;

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public List<MahaMiningResponseId> getData1() {
		return data1;
	}

	public void setData1(List<MahaMiningResponseId> data1) {
		this.data1 = data1;
	}

	@Override
	public String toString() {
		return "MahaMiningApiResponseData [data=" + data + ", data1=" + data1 + "]";
	}
}
