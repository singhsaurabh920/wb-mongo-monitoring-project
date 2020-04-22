package org.gps.integration.client.push.itcms;

import java.util.List;

public class ITCMSApiData {
	private List<ITCMSApiRequestModel> GData;

	public List<ITCMSApiRequestModel> getGData() {
		return GData;
	}

	public void setGData(List<ITCMSApiRequestModel> gData) {
		GData = gData;
	}

	@Override
	public String toString() {
		return "ITCMSApiData [GData=" + GData + "]";
	}
	
}
