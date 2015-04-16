package com.googboog.wifiheatmap;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.location.LocationRequest;

import java.text.DateFormat;
import java.util.Date;

import pl.charmas.android.reactivelocation.ReactiveLocationProvider;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;

public class ScannerFragment extends Fragment {
	protected static final String TAG = ScannerFragment.class.getSimpleName();

	MainActivity m;

	OnScannerSelectedListener mCallback;

	private OnFragmentInteractionListener mListener;

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

	private boolean currentlyScanning = false;

	public ScannerFragment() {
		// Required empty public constructor
	} // end constructor

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	} // end onCreate()

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_scanner, container, false);

		assignUIElements(view);

		return view;
	} // end onCreateView()

	// TODO: Rename method, update argument and hook method into UI event
	public void onButtonPressed(Uri uri) {
		if (mListener != null) {
			mListener.onFragmentInteraction(uri);
		} // end if
	} // end onButtonPressed()

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			mCallback = (OnScannerSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnScannerSelectedListener");
		} // end try/catch

		// Grab the MainActivity
		this.m = (MainActivity) getActivity();
	} // end onAttach()

	@Override
	public void onDetach() {
		super.onDetach();
		mListener = null;
	} // end onDetach()

	protected void assignUIElements(View view) {
		startLocationUpdatesButton = (Button) view.findViewById(R.id.buttonLocationUpdates);
		getDataPointButton = (Button) view.findViewById(R.id.buttonGetData);

		wifiSSIDTextView = (TextView) view.findViewById(R.id.textViewWifiSSID);
		wifiStrengthTextView = (TextView) view.findViewById(R.id.textViewWifiStrength);
		wifiSpeedTextView = (TextView) view.findViewById(R.id.textViewWifiSpeed);

		latitudeTextView = (TextView) view.findViewById(R.id.textViewLatitude);
		longitudeTextView = (TextView) view.findViewById(R.id.textViewLongitude);
		locationAccuracyTextView = (TextView) view.findViewById(R.id.textViewLocationAccuracy);
		lastUpdateTimeTextView = (TextView) view.findViewById(R.id.textViewLastUpdateTime);

		timerToggleButton = (Button) view.findViewById(R.id.buttonTimerToggle);
		intervalEditText = (EditText) view.findViewById(R.id.editTextInterval);

		timerToggleButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				/*
				if (timerRunning) {
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

				*/
				if (!currentlyScanning) {
					currentlyScanning = true;
					m.currentlyScanning = true;

					m.startCollectingDataFingerprints();

					timerToggleButton.setText("Stop collecting data");
				} else {
					currentlyScanning = false;
					m.currentlyScanning = false;

					m.stopCollectingDataFingerprints();

					timerToggleButton.setText("Start collecting data");
				} // end else/if
			} // end onClick()
		});

		// Toggling the periodic location updates
		startLocationUpdatesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				m.togglePeriodicLocationUpdates();

				if (!m.mRequestingLocationUpdates) {
					// Changing the button text
					startLocationUpdatesButton.setText(getString(R.string.stop_location_updates_button));
				} else {
					// Changing the button text
					startLocationUpdatesButton.setText(getString(R.string.start_location_updates_button));
				} // end if/else
			} // end onClick()
		});

		getDataPointButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				m.showToast("Waiting for location before grabbing data...");

				// Create location request
				LocationRequest request = LocationRequest.create()
						.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
						.setInterval(100);

				ReactiveLocationProvider locationProvider = new ReactiveLocationProvider(m.getApplicationContext());
				Subscription subscription = locationProvider.getUpdatedLocation(request)
						.filter(new Func1<Location, Boolean>() {
							@Override
							public Boolean call(Location location) {
								locationAccuracyTextView.setText("Location Accuracy: " + location.getAccuracy() + "m");
								return location.getAccuracy() < 10.0f;
							} // end call()
						})
						.first()
						.subscribe(new Action1<Location>() {
							@Override
							public void call(Location location) {
								WifiManager wifi = (WifiManager) m.getSystemService(Context.WIFI_SERVICE);

								m.wifiSSID = WifiUtils.getWifiSSID(wifi);
								wifiSSIDTextView.setText("Wifi SSID: \"" + m.wifiSSID + "\"");

								m.wifiStrength = WifiUtils.getWifiStrength(wifi);
								wifiStrengthTextView.setText("Wifi Strength: " + m.wifiStrength);

								m.wifiSpeed = WifiUtils.getWifiLinkSpeed(wifi);
								wifiSpeedTextView.setText("Wifi Speed: " + m.wifiSpeed);

								latitudeTextView.setText(String.valueOf("Latitude: " + location.getLatitude()));
								longitudeTextView.setText(String.valueOf("Longitude: " + location.getLongitude()));
								locationAccuracyTextView.setText("Location Accuracy: " + location.getAccuracy() + "m");
								lastUpdateTimeTextView.setText("Last Update Time: " + DateFormat.getTimeInstance().format(new Date()));
							} // end call
						});

			} // end onClick()
		});
	} // end assignUIElements()

	protected void updateUI() {
		latitudeTextView.setText(String.valueOf("Latitude: " + m.mCurrentLocation.getLatitude()));
		longitudeTextView.setText(String.valueOf("Longitude: " + m.mCurrentLocation.getLongitude()));
		locationAccuracyTextView.setText("Location Accuracy: " + m.mCurrentLocation.getAccuracy() + "m");
		lastUpdateTimeTextView.setText("Last Update Time: " + m.mLastUpdateTime);

		//showToast("updateUI() fired");
	} // end updateUI()

	public interface OnScannerSelectedListener {
		public void onScannerSelected(MainActivity m);
	}

	/**
	 * This interface must be implemented by activities that contain this
	 * fragment to allow an interaction in this fragment to be communicated
	 * to the activity and potentially other fragments contained in that
	 * activity.
	 * <p/>
	 * See the Android Training lesson <a href=
	 * "http://developer.android.com/training/basics/fragments/communicating.html"
	 * >Communicating with Other Fragments</a> for more information.
	 */
	public interface OnFragmentInteractionListener {
		// TODO: Update argument type and name
		public void onFragmentInteraction(Uri uri);
	}

}
