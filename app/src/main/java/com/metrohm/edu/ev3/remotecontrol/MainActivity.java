package com.metrohm.edu.ev3.remotecontrol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import lejos.hardware.Audio;
import lejos.remote.ev3.RemoteRequestEV3;
import lejos.remote.ev3.RemoteRequestSampleProvider;
import lejos.robotics.RegulatedMotor;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

	private RemoteRequestEV3 ev3;
	private RegulatedMotor left, right;
	private RemoteRequestSampleProvider distanceProvider;
	private float[] distanceSample;

	private MenuItem btnConnect;
	private Button btnLeft;
	private Button btnRight;
	private Button btnForward;
	private Button btnBackward;
	private Button btnGetDistance;
	private TextView txtDistance;
	private Audio audio;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnLeft = findViewById(R.id.left);
		btnRight = findViewById(R.id.right);
		btnForward = findViewById(R.id.forward);
		btnBackward = findViewById(R.id.backward);
		txtDistance = findViewById(R.id.txtDistance);
		btnGetDistance = findViewById(R.id.btnGetDistance);
		btnGetDistance.setOnClickListener(this);
		btnLeft.setOnTouchListener(this);
		btnRight.setOnTouchListener(this);
		btnForward.setOnTouchListener(this);
		btnBackward.setOnTouchListener(this);

		if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[] { Manifest.permission.INTERNET }, 101);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.btnConnect:
				btnConnect = item;
				if (ev3 == null) {
					new Control().execute("connect", "192.168.44.245");
					btnConnect.setEnabled(false);
				} else {
					new Control().execute("disconnect");
					btnConnect.setEnabled(false);
				}
				break;
		}
		return true;
	}

	private void updateDistanceValue() {
		new Thread(() -> {
			try {
				distanceProvider.fetchSample(distanceSample, 0);
				String distance = "Distance: " + distanceSample[0] + " cm";
				runOnUiThread(() -> txtDistance.setText(distance));
			} catch (Exception e) {
				Log.e("EV3", "exception on updateDistance", e);
				updateDistanceValue();
			}
		}).start();
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.btnGetDistance) {
			updateDistanceValue();
		}
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (v.getId() == R.id.left) new Control().execute("rotate left");
				else if (v.getId() == R.id.right) new Control().execute("rotate right");
				else if (v.getId() == R.id.forward) new Control().execute("forward");
				else if (v.getId() == R.id.backward) new Control().execute("backward");
				return true;
			case MotionEvent.ACTION_UP:
				new Control().execute("stop");
				return true;
		}
		return false;
	}

	private class Control extends AsyncTask<String, Integer, Long> {

		protected Long doInBackground(String... cmd) {
			if (cmd[0].equals("connect")) {
				try {
					ev3 = new RemoteRequestEV3(cmd[1]);
					left = ev3.createRegulatedMotor("B", 'L');
					right = ev3.createRegulatedMotor("C", 'L');
					distanceProvider = (RemoteRequestSampleProvider) ev3.createSampleProvider("S1", "lejos.hardware.sensor.EV3IRSensor", "Distance");
					distanceSample = new float[distanceProvider.sampleSize()];
					audio = ev3.getAudio();
					audio.systemSound(3);
					runOnUiThread(() -> {
						btnConnect.setIcon(R.drawable.ic_link_off);
						btnConnect.setEnabled(true);
						btnLeft.setEnabled(true);
						btnRight.setEnabled(true);
						btnForward.setEnabled(true);
						btnBackward.setEnabled(true);
						txtDistance.setVisibility(View.VISIBLE);
						btnGetDistance.setVisibility(View.VISIBLE);
					});
					return 0l;
				} catch (Exception e) {
					Log.e("EV3", "error on connecting", e);
					finishLeJos();
					runOnUiThread(() -> {
						btnConnect.setIcon(R.drawable.ic_link);
						btnConnect.setEnabled(true);
						txtDistance.setText("Distance: -- cm");
						txtDistance.setVisibility(View.INVISIBLE);
						btnGetDistance.setVisibility(View.INVISIBLE);
					});
					return 1l;
				}
			} else if (cmd[0].equals("disconnect") && ev3 != null) {
				finishLeJos();
				audio.systemSound(2);
				runOnUiThread(() -> {
					btnConnect.setIcon(R.drawable.ic_link);
					btnConnect.setEnabled(true);
					txtDistance.setText("Distance: -- cm");
					txtDistance.setVisibility(View.INVISIBLE);
					btnGetDistance.setVisibility(View.INVISIBLE);
					btnLeft.setEnabled(false);
					btnRight.setEnabled(false);
					btnForward.setEnabled(false);
					btnBackward.setEnabled(false);
				});
				return 0l;
			}

			if (ev3 == null) return 2l;

			if (cmd[0].equals("stop")) {
				left.stop(true);
				right.stop(true);
			} else if (cmd[0].equals("forward")) {
				left.forward();
				right.forward();
			} else if (cmd[0].equals("backward")) {
				left.backward();
				right.backward();
			} else if (cmd[0].equals("rotate left")) {
				left.backward();
				right.forward();
			} else if (cmd[0].equals("rotate right")) {
				left.forward();
				right.backward();
			}

			return 0l;
		}

		protected void onPostExecute(Long result) {
			if (result == 1l) Toast.makeText(MainActivity.this, "Could not connect to EV3", Toast.LENGTH_LONG).show();
			else if (result == 2l) Toast.makeText(MainActivity.this, "Not connected", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onDestroy() {
		finishLeJos();
		super.onDestroy();
	}

	private void finishLeJos() {
		try {
			distanceProvider.close();
		} catch (Exception e) {
			Log.e("EV3", "error on closing sensor", e);
		}
		try {
			left.close();
		} catch (Exception e) {
			Log.e("EV3", "error on closing left motor", e);
		}
		try {
			right.close();
		} catch (Exception e) {
			Log.e("EV3", "error on closing right motor", e);
		}
		new Timer().schedule(new TimerTask() {
			@Override
			public void run() {
				try {
					ev3.disConnect();
					ev3 = null;
				} catch (Exception e) {
					Log.e("EV3", "error on disconnecting EV3", e);
				}
			}
		}, 1000);
	}
}
