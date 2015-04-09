package com.googboog.wifiheatmap;

import java.util.ArrayList;

public class DataFingerprint {
	// Private vars
	private double latitude;
	private double longitude;
	private double altitude;
	private float accuracy;
	private String timestamp;
	private ArrayList<WifiFingerprint> detectedWifis = new ArrayList<>();


	// Mutaters
	public void setLatitude(double l) {
		latitude = l;
	}
	public void setLongitude(double l) {
		longitude = l;
	}
	public void setAltitude(double a) {
		altitude = a;
	}
	public void setAccuracy(float a) {
		accuracy = a;
	}
	public void setTimestamp(String t) {
		timestamp = t;
	}
	public void setDetectedWifis(ArrayList<WifiFingerprint> list) {
		detectedWifis = list;
	}
	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}
	public double getAltitude() {
		return altitude;
	}
	public float getAccuracy() {
		return accuracy;
	}
	public String getTimestamp() {
		return timestamp;
	}
	public ArrayList<WifiFingerprint> getDetectedWifis() {
		return detectedWifis;
	}

} // end class
