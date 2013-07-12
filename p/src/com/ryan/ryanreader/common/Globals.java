package com.ryan.ryanreader.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 
 * @author ryanlee
 *
 */
public class Globals {

	public static final boolean AD_MODE = false;// 广告模式
	public static boolean NETWORK_ENABLE = false;// 
	public static final String AD_KEY = "0npyuclq78s818";// 

	public static boolean isConnect(Context context) {
		try {
			ConnectivityManager connectivity = (ConnectivityManager) context
					.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connectivity != null) {
				NetworkInfo info = connectivity.getActiveNetworkInfo();
				if (info != null && info.isConnected()) {
					if (info.getState() == NetworkInfo.State.CONNECTED) {
						return true;
					}
				}
			}
		} catch (Exception e) {

		}

		return false;

	}
}
