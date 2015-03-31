package com.googboog.wifiheatmap;

import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class WifiUtils {
	public static int getWifiStrength(WifiManager wifiManager) {
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();

		return wifiInfo.getRssi();
	} // end getWifiStrength()

	public static String getWifiSSID(WifiManager wifiManager) {
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();

		return wifiInfo.getSSID().replaceAll("\"", "");
	} // end getWifiSSID()


	public static String getWifiLinkSpeed(WifiManager wifiManager) {
		WifiInfo wifiInfo = wifiManager.getConnectionInfo();
		int speed = wifiInfo.getLinkSpeed();

		return speed + wifiInfo.LINK_SPEED_UNITS;
	} // end getWifiLinkSpeed()

}
