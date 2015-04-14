package com.googboog.wifiheatmap;

import android.app.Fragment;
import android.content.Context;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;


public class MainActivity
		extends ActionBarActivity
		implements NavigationDrawerCallbacks, ScannerFragment.OnScannerSelectedListener, MapFragment.OnMapSelectedListener,
		GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

	// Logcat tag
	protected static final String TAG = MainActivity.class.getSimpleName();


	private GoogleApiClient mGoogleApiClient;
	private Location mLastLocation;
	protected Location mCurrentLocation;
	private LocationRequest mLocationRequest;

	protected String mLastUpdateTime;

	protected boolean mRequestingLocationUpdates = false;

	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
	// Location updates intervals in sec
	private static int UPDATE_INTERVAL = 2000; // 2 sec
	private static int FASTEST_INTERVAL = 1000; // 1 sec
	private static int DISPLACEMENT = 10; // 10 meters

	// Wifi scan delay (i.e., wait delay between completion of scan and start of next scan)
	static final long WIFI_SCAN_DELAY_MILLIS = 2000;

	int wifiStrength;
	String wifiSSID;
	String wifiSpeed;

	// Timer stuff
	private boolean timerRunning = false;
	private long timerInterval = 1000;

	// Wifi scanner stuff
	static WifiManager wifiManager;
	private static IntentFilter wifiIntentFilter;
	private static WifiBroadcastReceiver wifiBroadcastReceiver;

	Date lastWifiScanTime;

	// Bool for if we're currently scanning
	boolean currentlyScanning = false;


	/**
	 * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;
	private Toolbar mToolbar;

	public void onScannerSelected(MainActivity m) {

	}

	public void onMapSelected(MainActivity m) {

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
		setSupportActionBar(mToolbar);

		mNavigationDrawerFragment = (NavigationDrawerFragment) getFragmentManager().findFragmentById(R.id.fragment_drawer);

		// Set up the drawer.
		mNavigationDrawerFragment.setup(R.id.fragment_drawer, (DrawerLayout) findViewById(R.id.drawer), mToolbar);

		// First we need to check availability of play services
		if (checkPlayServices()) {
			// Building the GoogleApi client
			buildGoogleApiClient();
		} // end if
	}

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

		if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
			startLocationUpdates();
		} // end if
	} // end onResume()

	@Override
	protected void onStop() {
		super.onStop();

		disconnectGoogleApiClient();

		this.unregisterReceiver(wifiBroadcastReceiver);
	} // end onStop()

	@Override
	protected void onDestroy() {
		super.onDestroy();

		disconnectGoogleApiClient();
	} // end onDestroy()

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// update the main content by replacing fragments
		Toast.makeText(this, "Menu item selected -> " + position, Toast.LENGTH_SHORT).show();

		Fragment fragment;
		switch (position) {
			case 0: // Scanner fragment
				fragment = getFragmentManager().findFragmentByTag(ScannerFragment.TAG);

				if (fragment == null) {
					fragment = new ScannerFragment();
				} // end if

				getFragmentManager().beginTransaction().replace(R.id.container, fragment, ScannerFragment.TAG).commit();

				break;
			case 1: // Map fragment
				fragment = getFragmentManager().findFragmentByTag(MapFragment.TAG);

				if (fragment == null) {
					fragment = new MapFragment();
				} // end if

				getFragmentManager().beginTransaction().replace(R.id.container, fragment, MapFragment.TAG).commit();

				break;
		} // end switch()
	} // end onNavigationDrawerItemSelected()


	@Override
	public void onBackPressed() {
		if (mNavigationDrawerFragment.isDrawerOpen()) {
			mNavigationDrawerFragment.closeDrawer();
		} else {
			super.onBackPressed();
		} // end if/else
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			return true;
		} // end if

		return super.onCreateOptionsMenu(menu);
	} // end onCreateOptionsMenu()


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		} // end if

		return super.onOptionsItemSelected(item);
	} // end onOptionsItemSelected()

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

	/* Checks if external storage is available for read and write */
	public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			return true;
		} // end if

		return false;
	} // end isExternalStorageWritable()

	public void writeDataToSDCard(DataFingerprint fingerprint) {
		// If we can write to the SD card, then do it
		if (isExternalStorageWritable()) {
			// Get SD card path
			File sdCard = Environment.getExternalStorageDirectory();
			// Grab the app directory
			File dir = new File(sdCard.getAbsolutePath() + "/WiFiHeatMap/DataPoints");

			// Make the dirs, if we have to
			dir.mkdirs();

			// Get a list of the files in the current dir
			File files[] = dir.listFiles();

			// Figure out what to name the new file based on the number of data points we have so far
			String dataPointName = String.valueOf(files.length) + ".json";

			// Create the new file object
			File wiFile = new File(dir, dataPointName);


			// Create the JSON file object mapper
			ObjectMapper mapper = new ObjectMapper();

			// Write to disk
			try {
				mapper.writeValue(wiFile, fingerprint);
			} catch (Exception e) {
				// Catch exception
				e.printStackTrace();
			} // end try/catch

			showToast("Wrote to " + wiFile.getName());
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
	protected void togglePeriodicLocationUpdates() {
		if (!mRequestingLocationUpdates) {
			mRequestingLocationUpdates = true;

			// Starting the location updates
			startLocationUpdates();

			Log.d(TAG, "Periodic location updates started!");
		} else {
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
		//updateUI();

		//showToast("onLocationChanged() fired");
	} // end onLocationChanged()

	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this).addConnectionCallbacks(this).addOnConnectionFailedListener(this).addApi(LocationServices.API).build();

		createLocationRequest();

		//showToast("Built Google API Client!");
	} // end buildGoogleApiClient()

	private void disconnectGoogleApiClient() {
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		} // end if
	} // end disconnectGoogleApiClient()

	public void getLastKnownLocation() {
		mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

		if (mLastLocation != null) {
			//latitudeTextView.setText(String.valueOf("Latitude: " + mLastLocation.getLatitude()));
			//longitudeTextView.setText(String.valueOf("Longitude:" + mLastLocation.getLongitude()));
		} // end if
	} // end getLastKnownLocation()

	@Override
	public void onConnected(Bundle connectionHint) {
		getLastKnownLocation();


		if (mRequestingLocationUpdates) {
			startLocationUpdates();
		} // end if

		//showToast("Connected to location services");

		// Connect to Wifi broadcast receiver
		initWifiScan();
	} // end onConnected()

	@Override
	public void onConnectionSuspended(int arg0) {
		mGoogleApiClient.connect();
	} // end onConnectionSuspended()

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
	} // end onConnectionFailed()

} // end class
