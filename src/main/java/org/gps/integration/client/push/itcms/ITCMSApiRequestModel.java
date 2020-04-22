package org.gps.integration.client.push.itcms;

public class ITCMSApiRequestModel {
	private String IMEI;
	private double LAT;
	private double LON;
	private String CTIME;
	private int SPEED;
	private int DISTANCE;
	public String getIMEI() {
		return IMEI;
	}
	public void setIMEI(String iMEI) {
		IMEI = iMEI;
	}
	public double getLAT() {
		return LAT;
	}
	public void setLAT(double lAT) {
		LAT = lAT;
	}
	public double getLON() {
		return LON;
	}
	public void setLON(double lON) {
		LON = lON;
	}
	public String getCTIME() {
		return CTIME;
	}
	public void setCTIME(String cTIME) {
		CTIME = cTIME;
	}
	public int getSPEED() {
		return SPEED;
	}
	public void setSPEED(int sPEED) {
		SPEED = sPEED;
	}
	public int getDISTANCE() {
		return DISTANCE;
	}
	public void setDISTANCE(int dISTANCE) {
		DISTANCE = dISTANCE;
	}
	
	@Override
	public String toString() {
		return "ITCMSApiRequestModel [IMEI=" + IMEI + ", LAT=" + LAT + ", LON=" + LON + ", CTIME=" + CTIME + ", SPEED="
				+ SPEED + ", DISTANCE=" + DISTANCE + "]";
	}
	
}
