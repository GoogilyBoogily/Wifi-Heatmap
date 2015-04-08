package com.googboog.wifiheatmap;

import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.util.Date;


public class MainActivity extends Activity
		implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
	// Logcat tag
	protected static final String TAG = MainActivity.class.getSimpleName();


	private GoogleApiClient mGoogleApiClient;
	private Location mLastLocation;
	protected Location mCurrentLocation;
	private LocationRequest mLocationRequest;

	protected String mLastUpdateTime;

	private boolean mRequestingLocationUpdates = false;

	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
	// Location updates intervals in sec
	private static int UPDATE_INTERVAL = 3000; // 3 sec
	private static int FASTEST_INTERVAL = 1000; // 1 sec
	private static int DISPLACEMENT = 10; // 10 meters

	// Wifi scan delay (i.e., wait $delay between completion of scan and start of next scan)
	static final long WIFI_SCAN_DELAY_MILLIS = 2000;

	int wifiStrength;
	String wifiSSID;
	String wifiSpeed;

	// UI vars
	private TextView wifiSSIDTextView;
	private TextView wifiStrengthTextView;
	private TextView wifiSpeedTextView;

	private Button startLocationUpdatesButton;
	private Button getDataPointButton;

	private TextView latitudeTextView;
	private TextView longitudeTextView;
	private TextView lastUpdateTimeTextView;
	private TextView locationAccuracyTextView;

	private Button timerToggleButton;
	private EditText intervalEditText;

	// Timer stuff
	private boolean timerRunning = false;
	private long timerInterval = 1000;

	// Wifi scanner stuff
	static WifiManager wifiManager;
	private static IntentFilter wifiIntentFilter;
	private static WifiBroadcastReceiver wifiBroadcastReceiver;

	String wifiListString;
	Date lastWifiScanTime;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Setting the UI vars
		assignUIElements();

		//updateValuesFromBundle(savedInstanceState);

		// First we need to check availability of play services
		if (checkPlayServices()) {
			// Building the GoogleApi client
			buildGoogleApiClient();
		} // end if

		initWifiScan();
		//wifiManager.startScan();
	} // end onCreate()

	public void showToast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
	} // end showToast()


	private void initWifiScan() {
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiBroadcastReceiver = new WifiBroadcastReceiver(this);
		wifiIntentFilter = new IntentFilter();
		wifiIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		this.registerReceiver(wifiBroadcastReceiver, wifiIntentFilter);

		showToast("Registered WifiBroadcastReceiver");
	} // end initWifiScan()


	// Runs without a timer by reposting this handler at the end of the runnable
	Handler timerHandler = new Handler();
	Runnable timerRunnable = new Runnable() {
		@Override
		public void run() {


			timerHandler.postDelayed(this, timerInterval);
		} // end run()
	};

	private void assignUIElements() {
		startLocationUpdatesButton = (Button) findViewById(R.id.buttonLocationUpdates);
		getDataPointButton = (Button) findViewById(R.id.buttonGetData);

		wifiSSIDTextView = (TextView) findViewById(R.id.textViewWifiSSID);
		wifiStrengthTextView = (TextView) findViewById(R.id.textViewWifiStrength);
		wifiSpeedTextView = (TextView) findViewById(R.id.textViewWifiSpeed);

		latitudeTextView = (TextView) findViewById(R.id.textViewLatitude);
		longitudeTextView = (TextView) findViewById(R.id.textViewLongitude);
		locationAccuracyTextView = (TextView) findViewById(R.id.textViewLocationAccuracy);
		lastUpdateTimeTextView = (TextView) findViewById(R.id.textViewLastUpdateTime);

		timerToggleButton = (Button) findViewById(R.id.buttonTimerToggle);
		intervalEditText = (EditText) findViewById(R.id.editTextInterval);

		timerToggleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(timerRunning) {
					timerHandler.removeCallbacks(timerRunnable);

					timerRunning = false;
					timerToggleButton.setText("Start Timer");
				} else {
					// Set the interval to the value the user entered
					timerInterval = Long.parseLong(intervalEditText.getText().toString());
					timerHandler.postDelayed(timerRunnable, 0);

					timerRunning = true;
					timerToggleButton.setText("Stop Timer");
				} // end else/if
			} // end onClick()
		});

		// Toggling the periodic location updates
		startLocationUpdatesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				togglePeriodicLocationUpdates();
			} // end onClick()
		});

		getDataPointButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

				/*
					The level of 100% is equivalent to the signal level of -35 dBm and higher, e.g.
					both -25 dBm and -15 dBm will be shown as 100%, because this level of signal is very high.
					The level of 1% is equivalent to the signal level of -95 dBm.
					Between -95 dBm and -35 dBm, the percentage scale is linear, i.e. 50% is equivalent to -65 dBm
				*/
				wifiSSID = WifiUtils.getWifiSSID(wifi);
				wifiSSIDTextView.setText("Wifi SSID: \"" + wifiSSID + "\"");

				wifiStrength = WifiUtils.getWifiStrength(wifi);
				wifiStrengthTextView.setText("Wifi Strength: " + wifiStrength);

				wifiSpeed = WifiUtils.getWifiLinkSpeed(wifi);
				wifiSpeedTextView.setText("Wifi Speed: " + wifiSpeed);

				createLocationRequest();
				getLastKnownLocation();

				// Write the collected data to JSON file on SD card
				writeDataToSDCard();
			} // end onClick()
		});
	} // end assignUIElements()

	protected void updateUI() {
		latitudeTextView.setText(String.valueOf("Latitude: " + mCurrentLocation.getLatitude()));
		longitudeTextView.setText(String.valueOf("Longitude: " + mCurrentLocation.getLongitude()));
		locationAccuracyTextView.setText("Location Accuracy: " + mCurrentLocation.getAccuracy() + "m");
		lastUpdateTimeTextView.setText("Last Update Time: " + mLastUpdateTime);

		//showToast("updateUI() fired");
	} // end updateUI()

	@Override
	protected void onStart() {
		super.onStart();

		if (mGoogleApiClient != null) {
			mGoogleApiClient.connect();
		} // end if
	} // end onStart()

	protected void onPause() {
		super.onPause();

		stopLocationUpdates();
	} // end onPause()

	protected void onResume() {
		super.onResume();

		if(mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
			startLocationUpdates();
		} // end if
	} // end onResume()

	@Override
	protected void onStop() {
		super.onStop();

		disconnectGoogleApiClient();
	} // end onStop()

	@Override
	protected void onDestroy() {
		super.onDestroy();

		disconnectGoogleApiClient();
	} // end onDestroy()

	/*
	public void onSaveInstanceState(Bundle savedInstanceState) {
		savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
		savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
		savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
		super.onSaveInstanceState(savedInstanceState);
	}

	private void updateValuesFromBundle(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			// Update the value of mRequestingLocationUpdates from the Bundle, and
			// make sure that the Start Updates and Stop Updates buttons are
			// correctly enabled or disabled.
			if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
				mRequestingLocationUpdates = savedInstanceState.getBoolean(REQUESTING_LOCATION_UPDATES_KEY);
				setButtonsEnabledState();
			}

			// Update the value of mCurrentLocation from the Bundle and update the
			// UI to show the correct latitude and longitude.
			if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
				// Since LOCATION_KEY was found in the Bundle, we can be sure that
				// mCurrentLocationis not null.
				mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
			}

			// Update the value of mLastUpdateTime from the Bundle and update the UI.
			if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
				mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
			}
			updateUI();
		}
	}
	*/

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	} // end onCreateOptionsMenu()

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if(id == R.id.action_settings) {
			return true;
		} // end if

		return super.onOptionsItemSelected(item);
	} // end onOptionsItemSelected()

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if(Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		} // end if

		return false;
	} // end isExternalStorageWritable()

	public void writeDataToSDCard() {
		// Create new JSON object and populate it with the data
		JSONObject dataToWrite = new JSONObject();
		try {
			dataToWrite.put("WiFi_SSID", wifiSSID);
			dataToWrite.put("WiFi_Strength", wifiStrength);
			dataToWrite.put("WiFi_Speed", wifiSpeed);
			dataToWrite.put("Latitude", mLastLocation.getLatitude());
			dataToWrite.put("Longitude", mLastLocation.getLongitude());
			dataToWrite.put("Altitude", mLastLocation.getAltitude());
		} catch (JSONException e) {
			e.printStackTrace();
		} // end try/catch


		// If we can write to the SD card, then do it
		if (isExternalStorageWritable()) {
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File(sdCard.getAbsolutePath() + "/WiFiHeatMap/DataPoints");
			// Make the dirs, if we have to
			dir.mkdirs();
			// Get a list of the files in the current dir
			File files[] = dir.listFiles();

			// Make a new JSON file with the SSID plus the number of files there are as the name
			File file = new File(dir, wifiSSID + String.valueOf(files.length) + ".json");

			try {
				FileOutputStream f = new FileOutputStream(file);
				f.write(dataToWrite.toString().getBytes());

				showToast("Wrote \"" + file.getName() + "\"");
			} catch (Exception e) {
				e.printStackTrace();
			} // end try/catch
		} // end if
	} // end writeDataToSDCard()

	/**
	 * Method to verify google play services on the device
	 */
	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
			} else {
				showToast("This device does not have Google Play Services.");

				finish();
			} // end if/else

			return false;
		} // end if

		return true;
	} // end checkPlayServices()

	/**
	 * Creating location request object
	 */
	protected void createLocationRequest() {
		mLocationRequest = new LocationRequest();

		// Sets the desired interval for active location updates. This interval is
		// inexact. You may not receive updates at all if no location sources are available, or
		// you may receive them slower than requested. You may also receive updates faster than
		// requested if other applications are requesting location at a faster interval.
		mLocationRequest.setInterval(UPDATE_INTERVAL);

		// Sets the fastest rate for active location updates. This interval is exact, and your
		// application will never receive updates faster than this value.
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		//mLocationRequest.setSmallestDisplacement(DISPLACEMENT);

		//showToast("Created location request");
	} // end createLocationRequest()

	/**
	 * Starting the location updates
	 */
	protected void startLocationUpdates() {
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
		//showToast("Started location updates");
	} // end startLocationUpdates()

	/**
	 * Stopping location updates
	 */
	protected void stopLocationUpdates() {
		LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
		//showToast("Stopped location updates");
	} // end stopLocationUpdates()

	/**
	 * Method to toggle periodic location updates
	 */
	private void togglePeriodicLocationUpdates() {
		if (!mRequestingLocationUpdates) {
			// Changing the button text
			startLocationUpdatesButton.setText(getString(R.string.stop_location_updates_button));

			mRequestingLocationUpdates = true;

			// Starting the location updates
			startLocationUpdates();

			Log.d(TAG, "Periodic location updates started!");
		} else {
			// Changing the button text
			startLocationUpdatesButton.setText(getString(R.string.start_location_updates_button));

			mRequestingLocationUpdates = false;

			// Stopping the location updates
			stopLocationUpdates();

			Log.d(TAG, "Periodic location updates stopped!");
		} // end if/else
	} // end togglePeriodicLocationUpdates()


	@Override
	public void onLocationChanged(Location location) {
		mCurrentLocation = location;
		mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
		updateUI();

		//showToast("onLocationChanged() fired");
	} // end onLocationChanged()

	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();

		createLocationRequest();

		//showToast("Built Google API Client!");
	} // end buildGoogleApiClient()

	private void disconnectGoogleApiClient() {
		if(mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		} // end if
	} // end disconnectGoogleApiClient()

	public void getLastKnownLocation() {
		mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

		if (mLastLocation != null) {
			latitudeTextView.setText(String.valueOf("Latitude: " + mLastLocation.getLatitude()));
			longitudeTextView.setText(String.valueOf("Longitude:" + mLastLocation.getLongitude()));
		} // end if
	} // end getLastKnownLocation()

	@Override
	public void onConnected(Bundle connectionHint) {
		getLastKnownLocation();


		if(mRequestingLocationUpdates) {
			startLocationUpdates();
		} // end if

		//showToast("Connected to location services");
	} // end onConnected()

	@Override
	public void onConnectionSuspended(int arg0) {
		mGoogleApiClient.connect();
	} // end onConnectionSuspended()

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
	} // end onConnectionFailed()

}
