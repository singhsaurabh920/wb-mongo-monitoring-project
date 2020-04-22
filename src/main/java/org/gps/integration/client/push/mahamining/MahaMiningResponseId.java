package org.gps.integration.client.push.mahamining;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MahaMiningResponseId {

	@JsonProperty("Id")
	private int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

}
