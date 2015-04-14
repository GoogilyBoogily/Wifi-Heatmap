package com.googboog.wifiheatmap;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/*
 * Receives Wifi scan result whenever WifiManager has them,
 * updates `wifiListString` and `lastWifiScanTime`,
 * logs location (and accuracy) and Wifis (SSID, BSSID, strength) to disk
 */
class WifiBroadcastReceiver extends BroadcastReceiver {
	private final MainActivity m;

	private static final int NOT_SPECIAL = 0;
	private static final int SPECIAL_NO_VISIBLE_WIFI = 1;

	private final Comparator<ScanResult> RSSI_ORDER = new Comparator<ScanResult>() {
		public int compare(ScanResult e1, ScanResult e2) {
			return Integer.compare(e2.level, e1.level);
		} // end compare()
	};

	private static final String WIFI_SCAN_TIMER = "wifi-scan-timer";
	private static Timer wifiScanTimer;

	public WifiBroadcastReceiver(MainActivity m) {
		this.m = m;
		wifiScanTimer = new Timer(WIFI_SCAN_TIMER);
	} // end constructor

	@Override
	public void onReceive(Context context, Intent intent) {
		List<ScanResult> scanResultList = MainActivity.wifiManager.getScanResults();
		m.lastWifiScanTime = new Date();

		//Collections.sort(scanResultList, RSSI_ORDER);


		// Schedule next scan after short delay

		// Only schedule a new scan if we're currently scanning
		if(m.currentlyScanning) {
			// Create and begin populating a new data fingerprint
			DataFingerprint fingerprint = new DataFingerprint();
			fingerprint.setLatitude(m.mCurrentLocation.getLatitude());
			fingerprint.setLongitude(m.mCurrentLocation.getLongitude());
			fingerprint.setAltitude(m.mCurrentLocation.getAltitude());
			fingerprint.setAccuracy(m.mCurrentLocation.getAccuracy());
			fingerprint.setTimestamp(m.mLastUpdateTime);

			ArrayList<WifiFingerprint> detectedWifis = new ArrayList<>();
			for (ScanResult wifi : scanResultList) {
				// Create and populate the wifi result
				WifiFingerprint wifiResult = new WifiFingerprint();
				wifiResult.setSSID(wifi.SSID);
				wifiResult.setBSSID(wifi.BSSID);
				wifiResult.setFrequency(wifi.frequency);
				wifiResult.setLevel(wifi.level);

				// Add the wifi to the list of all detected wifis
				detectedWifis.add(wifiResult);

				log(wifi, NOT_SPECIAL);
			} // end for

			fingerprint.setDetectedWifis(detectedWifis);


			m.writeDataToSDCard(fingerprint);

			wifiScanTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					MainActivity.wifiManager.startScan();
				}
			}, MainActivity.WIFI_SCAN_DELAY_MILLIS);
		} // end if
	} // end onReceive()

	private static int convertFrequencyToChannel(int freq) {
		if(freq >= 2412 && freq <= 2484) {
			return (freq - 2412) / 5 + 1;
		} else if (freq >= 5170 && freq <= 5825) {
			return (freq - 5170) / 5 + 34;
		} else {
			throw new IllegalArgumentException(Integer.toString(freq));
		} // end if/else block
	} // end convertFrequencyToChannel()

	private void log(ScanResult wifi, int specialCode) {
		String csvLine = Build.MODEL + "," +
		//		m.mCurrentLocation.getLatitude() + "," +
		//		m.mCurrentLocation.getLongitude() + "," +
		//		m.mCurrentLocation.getAltitude() + "," +
		//		m.mCurrentLocation.getAccuracy() + "," +
		//		m.mCurrentLocation.getSpeed() + "," +
				specialCode;

		if (specialCode == NOT_SPECIAL) {
			csvLine += "," + wifi.SSID // FIXME: escape commas
					+ ", " + wifi.level + ", " + convertFrequencyToChannel(wifi.frequency);
		} else if (specialCode == SPECIAL_NO_VISIBLE_WIFI) {
			csvLine += ",,,,";
		} else {
			throw new IllegalArgumentException(Integer.toString(specialCode));
		} // end if/else block

		Log.d(m.TAG, csvLine);

		//m.diskLog.info(csvLine);
	} // end log()

}
