package com.googboog.wifiheatmap;

public class WifiFingerprint {
	/*
		The level of 100% is equivalent to the signal level of -35 dBm and higher, e.g.
		both -25 dBm and -15 dBm will be shown as 100%, because this level of signal is very high.
		The level of 1% is equivalent to the signal level of -95 dBm.
		Between -95 dBm and -35 dBm, the percentage scale is linear, i.e. 50% is equivalent to -65 dBm
	*/
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
