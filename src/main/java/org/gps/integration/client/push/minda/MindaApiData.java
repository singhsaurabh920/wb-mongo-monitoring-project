package org.gps.integration.client.push.minda;

import java.util.List;

public class MindaApiData {
	private int count;
	private List<MindaApiRequestModel> mindaApiRequestModels;
	
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public List<MindaApiRequestModel> getMindaApiRequestModels() {
		return mindaApiRequestModels;
	}
	public void setMindaApiRequestModels(List<MindaApiRequestModel> mindaApiRequestModels) {
		this.mindaApiRequestModels = mindaApiRequestModels;
	}
	

}
