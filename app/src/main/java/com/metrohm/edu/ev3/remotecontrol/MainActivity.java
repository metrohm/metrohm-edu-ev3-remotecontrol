package com.metrohm.edu.ev3.remotecontrol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import lejos.hardware.Audio;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.port.SensorPort;
import lejos.hardware.port.UARTPort;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.remote.ev3.RMISampleProvider;
import lejos.remote.ev3.RemoteEV3;
import lejos.remote.ev3.RemoteRequestEV3;
import lejos.remote.ev3.RemoteRequestSampleProvider;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.SampleProvider;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

	private RemoteRequestEV3 ev3;
	private RegulatedMotor left, right;
	private RemoteRequestSampleProvider distanceProvider;
	private float[] distanceSample;

	private Thread updateDistanceThread;
	private boolean updateDistanceEnabled = false;

	private Button connect;
	private TextView txtDistance;
	private Audio audio;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button left = findViewById(R.id.left);
		Button right = findViewById(R.id.right);
		Button forward = findViewById(R.id.forward);
		Button backward = findViewById(R.id.backward);
		txtDistance = findViewById(R.id.txtDistance);
		connect = findViewById(R.id.connect);
		connect.setOnClickListener(this);
		left.setOnTouchListener(this);
		right.setOnTouchListener(this);
		forward.setOnTouchListener(this);
		backward.setOnTouchListener(this);

		if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[] { Manifest.permission.INTERNET }, 101);
		}
	}

	private void updateDistance() {
		if (updateDistanceEnabled) {
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					try {
						distanceProvider.fetchSample(distanceSample, 0);
						runOnUiThread(()->txtDistance.setText("Distance: " + distanceSample[0] + " cm"));
					} catch (Exception e) {
						Log.e("EV3", "exception on updateDistance", e);
					}
					updateDistance();
				}
			}, 250);
		}
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.connect) {
			if (ev3 == null) {
				new Control().execute("connect", "192.168.44.245");
				connect.setText("Disconnect");
				txtDistance.setVisibility(View.VISIBLE);
			} else {
				new Control().execute("disconnect");
				connect.setText("Connect");
				txtDistance.setText("Distance: -- cm");
				txtDistance.setVisibility(View.INVISIBLE);
			}
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
					updateDistanceThread = new Thread() {
						@Override
						public void run() {
							updateDistance();
						}
					};
					updateDistanceEnabled = true;
					new Timer().schedule(new TimerTask() {
						@Override
						public void run() {
							updateDistanceThread.start();
						}
					}, 2000);
					audio = ev3.getAudio();
					audio.systemSound(3);
					return 0l;
				} catch (Exception e) {
					Log.e("EV3", "error on connecting", e);
					finishLeJos();
					return 1l;
				}
			} else if (cmd[0].equals("disconnect") && ev3 != null) {
				audio.systemSound(2);
				finishLeJos();
				return 0l;
			}

			if (ev3 == null) return 2l;

//			ev3.getAudio().systemSound(1);

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
			updateDistanceEnabled = false;
			updateDistanceThread.interrupt();
		} catch (Exception e) {
			Log.e("EV3", "error on interrupt distance thread", e);
		}
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
