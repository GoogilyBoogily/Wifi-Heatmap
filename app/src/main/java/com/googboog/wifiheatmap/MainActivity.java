package com.googboog.wifiheatmap;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
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
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.WeightedLatLng;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;


public class MainActivity
		extends ActionBarActivity
		implements NavigationDrawerCallbacks, ScannerFragment.OnScannerSelectedListener, MapFragment.OnMapSelectedListener,
		OnMapReadyCallback {

	// Logcat tag
	protected static final String TAG = MainActivity.class.getSimpleName();

	private Location mLastLocation;
	protected Location mCurrentLocation;

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

	String currentFloor;

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

	GoogleMap mGoogleMap;


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
		if (!checkPlayServices()) {
			showToast("This device doesn't have Google Play Services...");
		} // end if
	} // end onCreate()

	@Override
	protected void onStart() {
		super.onStart();
	} // end onStart()

	protected void onPause() {
		super.onPause();
	} // end onPause()

	protected void onResume() {
		super.onResume();
	} // end onResume()

	@Override
	protected void onStop() {
		super.onStop();
	} // end onStop()

	@Override
	protected void onDestroy() {
		super.onDestroy();
	} // end onDestroy()

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		// Update the main content by replacing fragments
		switch (position) {
			case 0: // Scanner fragment
				ScannerFragment scanFragment = (ScannerFragment) getFragmentManager().findFragmentByTag(ScannerFragment.TAG);

				if (scanFragment == null) {
					scanFragment = new ScannerFragment();
				} // end if

				getFragmentManager().beginTransaction().replace(R.id.container, scanFragment, ScannerFragment.TAG).commit();

				break;
			case 1: // Map fragment
				com.google.android.gms.maps.MapFragment mapFragment = (com.google.android.gms.maps.MapFragment) getFragmentManager().findFragmentById(R.id.map);

				if (mapFragment == null) {
					mapFragment = MapFragment.newInstance();
				} // end if

				mapFragment.getMapAsync(this);

				getFragmentManager().beginTransaction().replace(R.id.container, mapFragment, MapFragment.TAG).commit();

				break;
		} // end switch()
	} // end onNavigationDrawerItemSelected()

	@Override
	public void onMapReady(GoogleMap map) {
		mGoogleMap = map;

		map.setMyLocationEnabled(true);

		// Zoom the camera into the user's current position
		LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();

		Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
		if (location != null) {
			map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));

			CameraPosition cameraPosition = new CameraPosition.Builder().target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
					.zoom(20)                   // Sets the zoom
					//.bearing(90)                // Sets the orientation of the camera to east
					//.tilt(40)                   // Sets the tilt of the camera to 30 degrees
					.build();                   // Creates a CameraPosition from the builder
			map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

		} // end if

		addHeatmap();
	} // end onMapReady()

	public void addHeatmap() {
		//ArrayList<WeightedLatLng> wifiData = getWeightedLatLngList();


		ArrayList<com.googboog.wifiheatmap.WeightedLatLng> list = new ArrayList<>();


		// For all collected data fingerprints
		for (DataFingerprint dataFingerprint : getDataFingerprints()) {

			ArrayList<WifiFingerprint> wifis = dataFingerprint.getDetectedWifis();

			Collections.sort(wifis, new Comparator<WifiFingerprint>() {
				@Override
				public int compare(WifiFingerprint finger1, WifiFingerprint finger2) {

					return Integer.compare(finger2.getLevel(), finger1.getLevel());
				} // end compare()
			});

			// For all detected wifis at the data fingerprint
			for (WifiFingerprint wifi : wifis) {

				if (wifi.getSSID().equals("ToTheMoon")) {
					//Log.d(TAG, wifi.getBSSID() + ": " + wifi.getLevel());

					float normalizedLevel = getNormalizedWiFiLevel(wifi.getLevel());
					int colorIntensity = generateColorIntensity(normalizedLevel);


					com.googboog.wifiheatmap.WeightedLatLng latLng = new com.googboog.wifiheatmap.WeightedLatLng(new LatLng(
							dataFingerprint.getLatitude(),
							dataFingerprint.getLongitude()),
							normalizedLevel);
					list.add(latLng);

					// Instantiates a new CircleOptions object and defines the center and radius
					CircleOptions circleOptions = new CircleOptions()
							.center(new LatLng(dataFingerprint.getLatitude(), dataFingerprint.getLongitude()))
							.radius(2) // In meters
							.fillColor(colorIntensity).strokeColor(Color.TRANSPARENT); // Don't show the border to the circle
					// Get back the mutable Circle
					//Circle circle = mGoogleMap.addCircle(circleOptions);

					break;
				} // end if
			} // end for
		} // end for


		HeatmapTile tile = new HeatmapTile.Builder().radius(40).weightedData(list).build();

		TileOverlay mOverlay = mGoogleMap.addTileOverlay(new TileOverlayOptions().tileProvider(tile));


		/*
		// Create a heat map tile provider, passing it the latlngs
		HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder().weightedData(wifiData).build();
		// Add a tile overlay to the map, using the heat map tile provider.
		TileOverlay mOverlay = mGoogleMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
		*/
	} // end addHeatMap()


	public void generateHeatmap() {

	} // end generateHeatmap()

	public int generateColorIntensity(float intensity) {
		int color1 = Color.rgb(102, 225, 0);
		int color2 = Color.rgb(255, 0, 0);

		//int alpha = (int) ((Color.alpha(color2) - Color.alpha(color1)) * intensity + Color.alpha(color1));
		int alpha = 50;

		float[] hsv1 = new float[3];
		Color.RGBToHSV(Color.red(color1), Color.green(color1), Color.blue(color1), hsv1);
		float[] hsv2 = new float[3];
		Color.RGBToHSV(Color.red(color2), Color.green(color2), Color.blue(color2), hsv2);

		// Adjust so that the shortest path on the color wheel will be taken
		if (hsv1[0] - hsv2[0] > 180) {
			hsv2[0] += 360;
		} else if (hsv2[0] - hsv1[0] > 180) {
			hsv1[0] += 360;
		} // end if/else

		// Interpolate using calculated ratio
		float[] result = new float[3];
		for (int i = 0; i < 3; i++) {
			result[i] = (hsv2[i] - hsv1[i]) * (intensity) + hsv1[i];
		} // end for

		return Color.HSVToColor(alpha, result);
	} // end generateColorIntensity()

	public ArrayList<DataFingerprint> getDataFingerprints() {
		ArrayList<DataFingerprint> data = new ArrayList<>();

		// Get SD card path
		File sdCard = Environment.getExternalStorageDirectory();
		// Grab the app directory
		File dir = new File(sdCard.getAbsolutePath() + "/WiFiHeatMap/DataPoints");

		// Get a list of the files in the current dir
		File files[] = dir.listFiles();

		ObjectMapper mapper = new ObjectMapper();

		// For each data fingerprint we have in the directory
		for (File file : files) {
			try {
				// Grab the data point from the file
				DataFingerprint dataPoint = mapper.readValue(file, DataFingerprint.class);

				data.add(dataPoint);
			} catch (Exception e) {
				e.printStackTrace();
			} // end try/catch
		} // end for

		return data;
	} // end getDataFingerprints()

	public ArrayList<WeightedLatLng> getWeightedLatLngList() {
		ArrayList<WeightedLatLng> data = new ArrayList<>();

		// Get SD card path
		File sdCard = Environment.getExternalStorageDirectory();
		// Grab the app directory
		File dir = new File(sdCard.getAbsolutePath() + "/WiFiHeatMap/DataPoints");

		// Get a list of the files in the current dir
		File files[] = dir.listFiles();

		ObjectMapper mapper = new ObjectMapper();

		// For each data fingerprint we have in the directory
		for (File file : files) {
			try {
				// Grab the data point from the file
				DataFingerprint dataPoint = mapper.readValue(file, DataFingerprint.class);

				// Create the new latlng object with the latitude and longitude
				LatLng latLng = new LatLng(dataPoint.getLatitude(), dataPoint.getLongitude());

				// Grab each wifi that was detected at this latlng
				ArrayList<WifiFingerprint> detectedWifis = dataPoint.getDetectedWifis();

				for (WifiFingerprint wifi : detectedWifis) {
					if (wifi.getSSID().equals("UMD-Wireless")) {
						// Create the weighted data point with the normalized WiFi level
						double normalizedWifiLevel = getNormalizedWiFiLevel(wifi.getLevel());
						WeightedLatLng weightedLatLng = new WeightedLatLng(latLng, normalizedWifiLevel);

						// Add the generated weighted latlng to the list
						data.add(weightedLatLng);
					} // end if
				} // end for

			} catch (Exception e) {
				e.printStackTrace();
			} // end try/catch
		} // end for

		return data;
	} // end getWeightedLatLngList()

	public float getNormalizedWiFiLevel(int wifiLevel) {
		float normalizedLevel;
		float choppedLevel = wifiLevel;

		// Anything bigger than -35 is still 100% and anything under -95 is still 0%
		if (choppedLevel > -35) {
			choppedLevel = -35.0f;
		} else if (choppedLevel < -95) {
			choppedLevel = -95.0f;
		} // end else/if

		// Normalize the level with (x - min)/(max - min)
		normalizedLevel = (choppedLevel - (-95.0f)) / ((-35.0f) - (-95.0f));

		Log.d(TAG, "Normalized level: " + String.valueOf(normalizedLevel));

		return normalizedLevel;
	} // end getNormalizedWiFiLevel()

	public Canvas generateHeatmap(GoogleMap map, DataFingerprint dataPoint) {
		int width = 100;
		int height = 100;

		Bitmap backbuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas myCanvas = new Canvas(backbuffer);
		Paint p = new Paint();
		p.setStyle(Paint.Style.FILL);
		p.setColor(Color.TRANSPARENT);
		myCanvas.drawRect(0, 0, width, height, p);


		ArrayList<WifiFingerprint> detectedWifis = dataPoint.getDetectedWifis();

		for (WifiFingerprint wifi : detectedWifis) {
			float x = (float) dataPoint.getLatitude();
			float y = (float) dataPoint.getLongitude();
			float radius = dataPoint.getAccuracy();

			int intensity = (int) getNormalizedWiFiLevel(wifi.getLevel()) * 10;

			RadialGradient g = new RadialGradient(x, y, radius, Color.argb(Math.max(10 * intensity, 255), 0, 0, 0), Color.TRANSPARENT, Shader.TileMode.CLAMP);
			Paint gp = new Paint();
			gp.setShader(g);
			myCanvas.drawCircle(x, y, radius, gp);
		} // end for


		return myCanvas;
	} // end generateHeatmap()


	@Override
	public void onBackPressed() {
		if (mNavigationDrawerFragment.isDrawerOpen()) {
			mNavigationDrawerFragment.closeDrawer();
		} else {
			super.onBackPressed();
		} // end if/else
	} // end onBackPressed()


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
		// Create the toast
		final Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);

		// Show the toast
		toast.show();

		// Cancel the toast after a custom amount of time
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				toast.cancel();
			} // end run
		}, 500);
	} // end showToast()

	Subscription fingerprintCollectionSub;
	public void startCollectingDataFingerprints() {
		showToast("Waiting for location before grabbing first point...");

		// Connect to Wifi broadcast receiver
		initWifiScan();

		// Start scanning for Wifis
		wifiManager.startScan();

		// Create location request
		LocationRequest request = LocationRequest.create()
				.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
				.setInterval(100);

		ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(getApplicationContext());
		fingerprintCollectionSub = locationProvider.getUpdatedLocation(request).filter(new Func1<Location, Boolean>() {
			@Override
			public Boolean call(Location location) {
				return location.getAccuracy() < 20.0f;
			} // end call()
		}).subscribe(new Action1<Location>() {
			@Override
			public void call(Location location) {
					mCurrentLocation = location;
					mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
				} // end call
			});
	} // end startCollectingDataFingerprints()

	public void stopCollectingDataFingerprints() {
		fingerprintCollectionSub.unsubscribe();

		this.unregisterReceiver(wifiBroadcastReceiver);
	} // end stopCollectingDataFingerprints()


	private void initWifiScan() {
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		wifiBroadcastReceiver = new WifiBroadcastReceiver(this);
		wifiIntentFilter = new IntentFilter();
		wifiIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		this.registerReceiver(wifiBroadcastReceiver, wifiIntentFilter);
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

} // end class
