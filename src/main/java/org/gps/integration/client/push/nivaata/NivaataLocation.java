package org.gps.integration.client.push.nivaata;

import java.util.List;

public class NivaataLocation {
	private String vehiclename;
	private List<NivaataLocationModel> location;

	public String getVehiclename() {
		return vehiclename;
	}

	public void setVehiclename(String vehiclename) {
		this.vehiclename = vehiclename;
	}

	public List<NivaataLocationModel> getLocation() {
		return location;
	}

	public void setLocation(List<NivaataLocationModel> location) {
		this.location = location;
	}
	

}