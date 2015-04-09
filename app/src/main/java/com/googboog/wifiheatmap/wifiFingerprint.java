package com.googboog.wifiheatmap;

public class WifiFingerprint {
	private String SSID;
	private String BSSID;
	private int frequency;
	private int level;

	public void WifiFingerprint() {
		SSID = "";
		BSSID = "";
		frequency = 0;
		level = 0;
	} // end constructor

	// Mutaters
	public void setSSID(String s) {
		SSID = s;
	}
	public void setBSSID(String b) {
		BSSID = b;
	}
	public void setFrequency(int f) {
		frequency = f;
	}
	public void setLevel(int l) {
		level = l;
	}
	public String getSSID() {
		return SSID;
	}
	public String getBSSID() {
		return BSSID;
	}
	public int getFrequency() {
		return frequency;
	}
	public int getLevel() {
		return level;
	}

} // end class
