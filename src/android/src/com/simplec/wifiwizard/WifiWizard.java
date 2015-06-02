/*
 * Copyright 2015 Matt Parsons
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.simplec.wifiwizard;

import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiWizard extends CordovaPlugin {

	private static final String ADD_NETWORK = "addNetwork";
	private static final String REMOVE_NETWORK = "removeNetwork";
	private static final String CONNECT_NETWORK = "connectNetwork";
	private static final String DISCONNECT_NETWORK = "disconnectNetwork";
	private static final String DISCONNECT = "disconnect";
	private static final String LIST_NETWORKS = "listNetworks";
	private static final String START_SCAN = "startScan";
	private static final String SIGNAL_STRENGTH = "wifiSignalStrength";
	private static final String GET_SCAN_RESULTS = "getScanResults";
	private static final String GET_CONNECTED_SSID = "getConnectedSSID";
	private static final String IS_WIFI_ENABLED = "isWifiEnabled";
	private static final String SET_WIFI_ENABLED = "setWifiEnabled";
	private static final String TAG = "WifiWizard";

	private WifiManager wifiManager;
	private CallbackContext callbackContext;

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView) {
		Log.e(TAG, "WifiWizard: initialize.");
		super.initialize(cordova, webView);
		this.wifiManager = (WifiManager) cordova.getActivity().getSystemService(Context.WIFI_SERVICE);
	}

	@Override
	public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

		this.callbackContext = callbackContext;

		if (action.equals(IS_WIFI_ENABLED)) {
			return this.isWifiEnabled(callbackContext);
		} else if (action.equals(SIGNAL_STRENGTH)) {
			return this.getWifiSignalStrength(callbackContext);
		} else if (action.equals(SET_WIFI_ENABLED)) {
			return this.setWifiEnabled(callbackContext, data);
		} else if (!wifiManager.isWifiEnabled()) {
			callbackContext.error("Wifi is not enabled.");
			return false;
		} else if (action.equals(ADD_NETWORK)) {
			return this.addNetwork(callbackContext, data);
		} else if (action.equals(REMOVE_NETWORK)) {
			return this.removeNetwork(callbackContext, data);
		} else if (action.equals(CONNECT_NETWORK)) {
			return this.connectNetwork(callbackContext, data);
		} else if (action.equals(DISCONNECT_NETWORK)) {
			return this.disconnectNetwork(callbackContext, data);
		} else if (action.equals(LIST_NETWORKS)) {
			return this.listNetworks(callbackContext);
		} else if (action.equals(START_SCAN)) {
			return this.startScan(callbackContext);
		} else if (action.equals(GET_SCAN_RESULTS)) {
			return this.getScanResults(callbackContext, data);
		} else if (action.equals(DISCONNECT)) {
			return this.disconnect(callbackContext);
		} else if (action.equals(GET_CONNECTED_SSID)) {
			return this.getConnectedSSID(callbackContext);
		} else {
			callbackContext.error("Incorrect action parameter: " + action);
		}

		return false;
	}

	/**
	 * This methods adds a network to the list of available WiFi networks. If
	 * the network already exists, then it updates it.
	 *
	 * @params callbackContext A Cordova callback context.
	 * @params data JSON Array with [0] == SSID, [1] == password
	 * @return true if add successful, false if add fails
	 */
	private boolean addNetwork(CallbackContext callbackContext, JSONArray data) {
		// Initialize the WifiConfiguration object
		Log.d(TAG, "WifiWizard: addNetwork entered.");

		try {
			// data's order for ANY object is 0: ssid, 1: authentication
			// algorithm,
			// 2+: authentication information.
			String newSSID = data.getString(0); // needs to be in quotes
			String authType = data.getString(1);
			String newPass = data.getString(2);
			boolean isHidden = data.length() > 3 ? data.getBoolean(3) : false;

			Log.e(TAG, "AddNetwork:  " + authType + " " + newSSID + "  " + newPass);

			if (authType.equals("WPA")) {
				WifiConfiguration wifi = new WifiConfiguration();

				wifi.SSID = newSSID;
				wifi.preSharedKey = newPass;
				wifi.hiddenSSID = isHidden;

				wifi.status = WifiConfiguration.Status.ENABLED;
				wifi.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
				wifi.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
				wifi.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				wifi.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
				wifi.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
				wifi.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

				boolean res1 = wifiManager.setWifiEnabled(true);
				wifi.networkId = ssidToNetworkId(newSSID);
				int res = 0;
				if (wifi.networkId == -1) {
					res = wifiManager.addNetwork(wifi);
					Log.e(TAG, newSSID + " successfully add returned " + res);
				} else {
					res = wifiManager.updateNetwork(wifi);
					Log.e(TAG, newSSID + " successfully update returned " + res);
				}
				if (res < 0) {
					callbackContext.error(newSSID + " was not added.");
				} else {
					boolean es = wifiManager.saveConfiguration();
					Log.d("WifiPreference", "saveConfiguration returned " + es);
					boolean b = wifiManager.enableNetwork(res, true);
					Log.d("WifiPreference", "enableNetwork returned " + b);

					if (b) {
						callbackContext.success(newSSID + " successfully added.");
					} else {
						callbackContext.error(newSSID + " failed to be added.");
					}
				}

				wifiManager.saveConfiguration();
				return true;
			} else if (authType.equals("WEP")) {
				WifiConfiguration wifi = new WifiConfiguration();
				wifi.SSID = newSSID; // IMP! This should be in Quotes!!
				wifi.hiddenSSID = isHidden;
				wifi.status = WifiConfiguration.Status.ENABLED;

				wifi.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
				wifi.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
				wifi.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				wifi.wepTxKeyIndex = 0;

				wifi.wepKeys[0] = "\"" + newPass + "\""; // This is the WEP Password
				wifi.wepTxKeyIndex = 0;

				boolean res1 = wifiManager.setWifiEnabled(true);
				wifi.networkId = ssidToNetworkId(newSSID);
				int res = 0;
				if (wifi.networkId == -1) {
					res = wifiManager.addNetwork(wifi);
					Log.e(TAG, newSSID + " successfully add returned " + res);
				} else {
					res = wifiManager.updateNetwork(wifi);
					Log.e(TAG, newSSID + " successfully update returned " + res);
				}
				if (res < 0) {
					callbackContext.error(newSSID + " was not added.");
				} else {
					boolean es = wifiManager.saveConfiguration();
					Log.d("WifiPreference", "saveConfiguration returned " + es);
					boolean b = wifiManager.enableNetwork(res, true);
					Log.d("WifiPreference", "enableNetwork returned " + b);

					if (b) {
						callbackContext.success(newSSID + " successfully added.");
					} else {
						callbackContext.error(newSSID + " failed to be added.");
					}
				}

				return true;
			} else if (authType.equals("EAP")) {/*
												 * WifiEnterpriseConfig
												 * enterpriseConfig = new
												 * WifiEnterpriseConfig();
												 * WifiConfiguration wifi = new
												 * WifiConfiguration(); wifi =
												 * new WifiConfiguration();
												 * wifi.SSID = newSSID;
												 * wifi.hiddenSSID = isHidden;
												 * wifi
												 * .allowedKeyManagement.set(
												 * KeyMgmt.WPA_EAP);
												 * wifi.allowedKeyManagement
												 * .set(KeyMgmt.IEEE8021X);
												 * 
												 * if (newPass.indexOf("/")<0) {
												 * callbackContext.error(newSSID
												 * +
												 * " could not be added. EAP requires username/password format in the password field."
												 * ); }
												 * 
												 * String userName =
												 * newPass.substring(0,
												 * newPass.indexOf("/")); String
												 * passWord =
												 * newPass.substring(newPass
												 * .indexOf("/")+1);
												 * enterpriseConfig
												 * .setIdentity(userName);
												 * enterpriseConfig
												 * .setPassword(passWord);
												 * enterpriseConfig
												 * .setEapMethod(
												 * WifiEnterpriseConfig
												 * .Eap.PEAP);
												 * 
												 * wifi.enterpriseConfig =
												 * enterpriseConfig;
												 * 
												 * boolean res1 =
												 * wifiManager.setWifiEnabled
												 * (true); wifi.networkId =
												 * ssidToNetworkId(newSSID); int
												 * res = 0; if (wifi.networkId
												 * == -1) { res =
												 * wifiManager.addNetwork(wifi);
												 * Log.e(TAG, newSSID +
												 * " successfully add returned "
												 * +res); } else { res =
												 * wifiManager
												 * .updateNetwork(wifi);
												 * Log.e(TAG, newSSID +
												 * " successfully update returned "
												 * +res); } if (res<0) {
												 * callbackContext.error(newSSID
												 * + " was not added."); } else
												 * { boolean es =
												 * wifiManager.saveConfiguration
												 * (); Log.d("WifiPreference",
												 * "saveConfiguration returned "
												 * + es); boolean b =
												 * wifiManager
												 * .enableNetwork(res, true);
												 * Log.d("WifiPreference",
												 * "enableNetwork returned " +
												 * b);
												 * 
												 * if (b) {
												 * callbackContext.success
												 * (newSSID +
												 * " successfully added."); }
												 * else {
												 * callbackContext.error(newSSID
												 * + " failed to be added."); }
												 * }
												 * 
												 * return true;
												 */
				return false;
			} else if (authType.equals("NONE")) {
				WifiConfiguration wifi = new WifiConfiguration();

				wifi.SSID = newSSID;
				wifi.hiddenSSID = isHidden;
				wifi.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				boolean res1 = wifiManager.setWifiEnabled(true);
				wifi.networkId = ssidToNetworkId(newSSID);
				int res = 0;
				if (wifi.networkId == -1) {
					res = wifiManager.addNetwork(wifi);
					Log.e(TAG, newSSID + " successfully add returned " + res);
				} else {
					res = wifiManager.updateNetwork(wifi);
					Log.e(TAG, newSSID + " successfully update returned " + res);
				}
				if (res < 0) {
					callbackContext.error(newSSID + " was not added.");
				} else {
					boolean es = wifiManager.saveConfiguration();
					Log.d("WifiPreference", "saveConfiguration returned " + es);
					boolean b = wifiManager.enableNetwork(res, true);
					Log.d("WifiPreference", "enableNetwork returned " + b);

					if (b) {
						callbackContext.success(newSSID + " successfully added.");
					} else {
						callbackContext.error(newSSID + " failed to be added.");
					}
				}

				return true;
			}
			// TODO: Add more authentications as necessary
			else {
				Log.d(TAG, "Wifi Authentication Type Not Supported.");
				callbackContext.error("Wifi Authentication Type Not Supported: " + authType);
				return false;
			}
		} catch (Exception e) {
			callbackContext.error(e.getMessage());
			Log.d(TAG, e.getMessage());
			return false;
		}
	}

	/**
	 * This method removes a network from the list of configured networks.
	 *
	 * @param callbackContext
	 *            A Cordova callback context
	 * @param data
	 *            JSON Array, with [0] being SSID to remove
	 * @return true if network removed, false if failed
	 */
	private boolean removeNetwork(CallbackContext callbackContext, JSONArray data) {
		Log.d(TAG, "WifiWizard: removeNetwork entered.");

		if (!validateData(data)) {
			callbackContext.error("WifiWizard: removeNetwork data invalid");
			Log.d(TAG, "WifiWizard: removeNetwork data invalid");
			return false;
		}

		// TODO: Verify the type of data!
		try {
			String ssidToDisconnect = data.getString(0);

			int networkIdToRemove = ssidToNetworkId(ssidToDisconnect);

			if (networkIdToRemove >= 0) {
				wifiManager.removeNetwork(networkIdToRemove);
				wifiManager.saveConfiguration();
				callbackContext.success("Network removed.");
				return true;
			} else {
				callbackContext.error("Network not found.");
				Log.d(TAG, "WifiWizard: Network not found, can't remove.");
				return false;
			}
		} catch (Exception e) {
			callbackContext.error(e.getMessage());
			Log.d(TAG, e.getMessage());
			return false;
		}
	}

	/**
	 * This method connects a network.
	 *
	 * @param callbackContext
	 *            A Cordova callback context
	 * @param data
	 *            JSON Array, with [0] being SSID to connect
	 * @return true if network connected, false if failed
	 */
	private boolean connectNetwork(CallbackContext callbackContext, JSONArray data) {
		Log.d(TAG, "WifiWizard: connectNetwork entered.");
		if (!validateData(data)) {
			callbackContext.error("WifiWizard: connectNetwork invalid data");
			Log.d(TAG, "WifiWizard: connectNetwork invalid data.");
			return false;
		}
		String ssidToConnect = "";

		try {
			ssidToConnect = data.getString(0);
		} catch (Exception e) {
			callbackContext.error(e.getMessage());
			Log.d(TAG, e.getMessage());
			return false;
		}

		int networkIdToConnect = ssidToNetworkId(ssidToConnect);

		if (networkIdToConnect >= 0) {
			// We disable the network before connecting, because if this was the
			// last connection before
			// a disconnect(), this will not reconnect.
			wifiManager.disableNetwork(networkIdToConnect);
			wifiManager.enableNetwork(networkIdToConnect, true);
			callbackContext.success("Network " + ssidToConnect + " connected!");
			return true;
		} else {
			callbackContext.error("Network " + ssidToConnect + " not found!");
			Log.d(TAG, "WifiWizard: Network not found to connect.");
			return false;
		}
	}

	/**
	 * This method disconnects a network.
	 *
	 * @param callbackContext
	 *            A Cordova callback context
	 * @param data
	 *            JSON Array, with [0] being SSID to connect
	 * @return true if network disconnected, false if failed
	 */
	private boolean disconnectNetwork(CallbackContext callbackContext, JSONArray data) {
		Log.d(TAG, "WifiWizard: disconnectNetwork entered.");
		if (!validateData(data)) {
			callbackContext.error("WifiWizard: disconnectNetwork invalid data");
			Log.d(TAG, "WifiWizard: disconnectNetwork invalid data");
			return false;
		}
		String ssidToDisconnect = "";
		// TODO: Verify type of data here!
		try {
			ssidToDisconnect = data.getString(0);
		} catch (Exception e) {
			callbackContext.error(e.getMessage());
			Log.d(TAG, e.getMessage());
			return false;
		}

		int networkIdToDisconnect = ssidToNetworkId(ssidToDisconnect);

		if (networkIdToDisconnect > 0) {
			wifiManager.disableNetwork(networkIdToDisconnect);
			callbackContext.success("Network " + ssidToDisconnect + " disconnected!");
			return true;
		} else {
			callbackContext.error("Network " + ssidToDisconnect + " not found!");
			Log.d(TAG, "WifiWizard: Network not found to disconnect.");
			return false;
		}
	}

	/**
	 * This method disconnects current network.
	 *
	 * @param callbackContext
	 *            A Cordova callback context
	 * @return true if network disconnected, false if failed
	 */
	private boolean disconnect(CallbackContext callbackContext) {
		Log.d(TAG, "WifiWizard: disconnect entered.");
		if (wifiManager.disconnect()) {
			callbackContext.success("Disconnected from current network");
			return true;
		} else {
			callbackContext.error("Unable to disconnect from the current network");
			return false;
		}
	}

	/**
	 * This method uses the callbackContext.success method to send a JSONArray
	 * of the currently configured networks.
	 *
	 * @param callbackContext
	 *            A Cordova callback context
	 * @param data
	 *            JSON Array, with [0] being SSID to connect
	 * @return true if network disconnected, false if failed
	 */
	private boolean listNetworks(CallbackContext callbackContext) {
		Log.d(TAG, "WifiWizard: listNetworks entered.");
		List<WifiConfiguration> wifiList = wifiManager.getConfiguredNetworks();

		JSONArray returnList = new JSONArray();

		for (WifiConfiguration wifi : wifiList) {
			returnList.put(wifi.SSID);
		}

		callbackContext.success(returnList);

		return true;
	}

	/**
	 * This method uses the callbackContext.success method to send a JSONArray
	 * of the scanned networks.
	 *
	 * @param callbackContext
	 *            A Cordova callback context
	 * @param data
	 *            JSONArray with [0] == JSONObject
	 * @return true
	 */
	private boolean getScanResults(CallbackContext callbackContext, JSONArray data) {
		List<ScanResult> scanResults = wifiManager.getScanResults();

		JSONArray returnList = new JSONArray();

		Integer numLevels = null;

		if (!data.isNull(0)) {
			try {
				JSONObject options = data.getJSONObject(0);

				if (options.has("numLevels")) {
					Integer levels = options.optInt("numLevels");

					if (levels > 0) {
						numLevels = levels;
					} else if (options.optBoolean("numLevels", false)) {
						// use previous default for {numLevels: true}
						numLevels = 5;
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		for (ScanResult scan : scanResults) {
			/*
			 * @todo - breaking change, remove this notice when tidying new
			 * release and explain changes, e.g.: 0.y.z includes a breaking
			 * change to WifiWizard.getScanResults(). Earlier versions set
			 * scans' level attributes to a number derived from
			 * wifiManager.calculateSignalLevel. This update returns scans' raw
			 * RSSI value as the level, per Android spec / APIs. If your
			 * application depends on the previous behaviour, we have added an
			 * options object that will modify behaviour: - if `(n == true || n
			 * < 2)`, `*.getScanResults({numLevels: n})` will return data as
			 * before, split in 5 levels; - if `(n > 1)`,
			 * `*.getScanResults({numLevels: n})` will calculate the signal
			 * level, split in n levels; - if `(n == false)`,
			 * `*.getScanResults({numLevels: n})` will use the raw signal level;
			 */

			int level;

			if (numLevels == null) {
				level = scan.level;
			} else {
				level = wifiManager.calculateSignalLevel(scan.level, numLevels);
			}

			JSONObject lvl = new JSONObject();
			try {
				lvl.put("level", level);
				lvl.put("SSID", scan.SSID);
				lvl.put("BSSID", scan.BSSID);
				returnList.put(lvl);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		callbackContext.success(returnList);
		return true;
	}

	/**
	 * This method uses the callbackContext.success method. It starts a wifi
	 * scanning
	 *
	 * @param callbackContext
	 *            A Cordova callback context
	 * @return true if started was successful
	 */
	private boolean startScan(CallbackContext callbackContext) {
		if (wifiManager.startScan()) {
			callbackContext.success();
			return true;
		} else {
			callbackContext.error("Scan failed");
			return false;
		}
	}

	/**
	 * This method retrieves the SSID for the currently connected network
	 *
	 * @param callbackContext
	 *            A Cordova callback context
	 * @return true if SSID found, false if not.
	 */
	private boolean getConnectedSSID(CallbackContext callbackContext) {
		if (!wifiManager.isWifiEnabled()) {
			callbackContext.error("Wifi is disabled");
			return false;
		}

		WifiInfo info = wifiManager.getConnectionInfo();

		if (info == null) {
			callbackContext.error("Unable to read wifi info");
			return false;
		}

		String ssid = info.getSSID();
		if (ssid.isEmpty()) {
			ssid = info.getBSSID();
		}
		if (ssid.isEmpty()) {
			callbackContext.error("SSID is empty");
			return false;
		}

		callbackContext.success(ssid);
		return true;
	}

	/**
	 * This method retrieves the current WiFi status
	 *
	 * @param callbackContext
	 *            A Cordova callback context
	 * @return true if WiFi is enabled, fail will be called if not.
	 */
	private boolean isWifiEnabled(CallbackContext callbackContext) {
		boolean isEnabled = wifiManager.isWifiEnabled();
		callbackContext.success(isEnabled ? "1" : "0");
		return isEnabled;
	}

	private boolean getWifiSignalStrength(CallbackContext callbackContext) {
		int linkSpeed = wifiManager.getConnectionInfo().getRssi();
		callbackContext.success("" + linkSpeed);
		return true;
	}

	/**
	 * This method takes a given String, searches the current list of configured
	 * WiFi networks, and returns the networkId for the network if the SSID
	 * matches. If not, it returns -1.
	 */
	private int ssidToNetworkId(String ssid) {
		List<WifiConfiguration> currentNetworks = wifiManager.getConfiguredNetworks();
		int networkId = -1;

		// For each network in the list, compare the SSID with the given one
		for (WifiConfiguration test : currentNetworks) {
			if (test.SSID.equals(ssid)) {
				networkId = test.networkId;
			}
		}

		return networkId;
	}

	/**
	 * This method enables or disables the wifi
	 */
	private boolean setWifiEnabled(CallbackContext callbackContext, JSONArray data) {
		if (!validateData(data)) {
			callbackContext.error("WifiWizard: disconnectNetwork invalid data");
			Log.d(TAG, "WifiWizard: disconnectNetwork invalid data");
			return false;
		}

		String status = "";

		try {
			status = data.getString(0);
		} catch (Exception e) {
			callbackContext.error(e.getMessage());
			Log.d(TAG, e.getMessage());
			return false;
		}

		if (wifiManager.setWifiEnabled(status.equals("true"))) {
			callbackContext.success();
			return true;
		} else {
			callbackContext.error("Cannot enable wifi");
			return false;
		}
	}

	private boolean validateData(JSONArray data) {
		try {
			if (data == null || data.get(0) == null) {
				callbackContext.error("Data is null.");
				return false;
			}
			return true;
		} catch (Exception e) {
			callbackContext.error(e.getMessage());
		}
		return false;
	}

}
