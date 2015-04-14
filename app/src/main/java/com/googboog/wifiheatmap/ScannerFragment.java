package com.googboog.wifiheatmap;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
				if (!m.currentlyScanning) {
					m.currentlyScanning = true;
					m.wifiManager.startScan();

					timerToggleButton.setText("Stop Timer");
				} else {
					m.currentlyScanning = false;

					timerToggleButton.setText("Start Timer");
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
				WifiManager wifi = (WifiManager) m.getSystemService(Context.WIFI_SERVICE);

				/*
					The level of 100% is equivalent to the signal level of -35 dBm and higher, e.g.
					both -25 dBm and -15 dBm will be shown as 100%, because this level of signal is very high.
					The level of 1% is equivalent to the signal level of -95 dBm.
					Between -95 dBm and -35 dBm, the percentage scale is linear, i.e. 50% is equivalent to -65 dBm
				*/
				m.wifiSSID = WifiUtils.getWifiSSID(wifi);
				wifiSSIDTextView.setText("Wifi SSID: \"" + m.wifiSSID + "\"");

				m.wifiStrength = WifiUtils.getWifiStrength(wifi);
				wifiStrengthTextView.setText("Wifi Strength: " + m.wifiStrength);

				m.wifiSpeed = WifiUtils.getWifiLinkSpeed(wifi);
				wifiSpeedTextView.setText("Wifi Speed: " + m. wifiSpeed);

				m.createLocationRequest();
				m.getLastKnownLocation();

				updateUI();
				// Write the collected data to JSON file on SD card
				//writeDataToSDCard();
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
