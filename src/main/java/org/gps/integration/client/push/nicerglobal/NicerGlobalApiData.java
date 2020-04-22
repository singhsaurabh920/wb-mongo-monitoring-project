package org.gps.integration.client.push.nicerglobal;

public class NicerGlobalApiData {
	private NicerGlobalRequestData DATAELEMENTS;
	private String VEHICLENO;
	private String GPSPROVIDERKEY;

	public NicerGlobalRequestData getDATAELEMENTS() {
		return DATAELEMENTS;
	}

	public void setDATAELEMENTS(NicerGlobalRequestData dATAELEMENTS) {
		DATAELEMENTS = dATAELEMENTS;
	}
	
	public String getVEHICLENO() {
		return VEHICLENO;
	}
	public void setVEHICLENO(String vEHICLENO) {
		VEHICLENO = vEHICLENO;
	}
	public String getGPSPROVIDERKEY() {
		return GPSPROVIDERKEY;
	}
	public void setGPSPROVIDERKEY(String gPSPROVIDERKEY) {
		GPSPROVIDERKEY = gPSPROVIDERKEY;
	}
}
