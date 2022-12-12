package com.metrohm.edu.ev3.remotecontrol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import lejos.hardware.Audio;
import lejos.remote.ev3.RemoteRequestEV3;
import lejos.robotics.RegulatedMotor;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener {

	private RemoteRequestEV3 ev3;
	private boolean connected = false;
	private RegulatedMotor left, right, canon;

	private MenuItem btnConnect;
	private Button btnLeft;
	private Button btnRight;
	private Button btnForward;
	private Audio audio;

	/**
	 * Called on App Startup
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		btnLeft = findViewById(R.id.left);
		btnRight = findViewById(R.id.right);
		btnForward = findViewById(R.id.forward);

		btnLeft.setOnTouchListener(this);
		btnRight.setOnTouchListener(this);
		btnForward.setOnTouchListener(this);

		if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[] { Manifest.permission.INTERNET }, 101);
		}
	}

	/**
	 * Add Main Menu to View
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_main, menu);
		return true;
	}

	/**
	 * Called when a Menu Item is Tapped
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.btnConnect:
				btnConnect = item;
				if (!connected) {
					new Control().execute(ControlCommand.CONNECT.toString(), "192.168.44.74");
					btnConnect.setVisible(false);
				} else {
					new Control().execute(ControlCommand.DISCONNECT.toString());
					btnConnect.setVisible(false);
				}
				break;
		}
		return true;
	}

	/**
	 * A View has been clicked
	 */
	@Override
	public void onClick(View v) {
		int id = v.getId();
	}

	/**
	 * Called when a Button was Pressed or Released
	 */
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (v.getId() == R.id.left) {
					new Control().execute(ControlCommand.FORWARD.toString());
				} else if (v.getId() == R.id.right) {
					new Control().execute(ControlCommand.ROTATE_LEFT.toString());
				} else if (v.getId() == R.id.forward) {
					new Control().execute(ControlCommand.ROTATE_RIGHT.toString());
				}
				return true;
			case MotionEvent.ACTION_UP:
				new Control().execute(ControlCommand.STOP.toString());
				return true;
		}
		return false;
	}

	/**
	 * Control the EV3
	 */
	private class Control extends AsyncTask<String, Integer, Long> {
		protected Long doInBackground(String... cmd) {
			if (cmd[0].equals(ControlCommand.CONNECT.toString())) {
				try {
//					ev3 = new RemoteRequestEV3(cmd[1]);
//					left = ev3.createRegulatedMotor("B", 'L');
//					right = ev3.createRegulatedMotor("C", 'L');
//					canon = ev3.createRegulatedMotor("D", 'L');
//					audio = ev3.getAudio();
//					audio.systemSound(3);
					Log.d(MainActivity.class.getName(), "Robot connected.");
					connected = true;
					runOnUiThread(() -> {
						btnConnect.setIcon(R.drawable.ic_link_off);
						btnConnect.setVisible(true);
						btnLeft.setEnabled(true);
						btnRight.setEnabled(true);
						btnForward.setEnabled(true);
					});
					return 0l;
				} catch (Exception e) {
					Log.e("EV3", "error on connecting", e);
					connected = false;
//					finishLeJos();
					runOnUiThread(() -> {
						btnConnect.setIcon(R.drawable.ic_link);
						btnConnect.setVisible(true);
					});
					return 1l;
				}
			} else if (cmd[0].equals(ControlCommand.DISCONNECT.toString())) {
//				finishLeJos();
//				audio.systemSound(2);
				Log.d(MainActivity.class.getName(), "Robot disconnected.");
				runOnUiThread(() -> {
					btnConnect.setIcon(R.drawable.ic_link);
					btnConnect.setVisible(true);
					btnLeft.setEnabled(false);
					btnRight.setEnabled(false);
					btnForward.setEnabled(false);
				});
				return 0l;
			}

//			if (ev3 == null) return 2l;

			if (cmd[0].equals(ControlCommand.STOP.toString())) {
//				left.stop(true);
//				right.stop(true);
				Log.d(MainActivity.class.getName(), "Robot stopped.");
			} else if (cmd[0].equals(ControlCommand.FORWARD.toString())) {
//				left.forward();
//				right.forward();
				Log.d(MainActivity.class.getName(), "Robot moves forward.");
			} else if (cmd[0].equals(ControlCommand.BACKWARD.toString())) {
//				left.backward();
//				right.backward();
				Log.d(MainActivity.class.getName(), "Robot moves backward.");
			} else if (cmd[0].equals(ControlCommand.ROTATE_LEFT.toString())) {
//				left.backward();
//				right.forward();
				Log.d(MainActivity.class.getName(), "Robot rotates left.");
			} else if (cmd[0].equals(ControlCommand.ROTATE_RIGHT.toString())) {
//				left.forward();
//				right.backward();
				Log.d(MainActivity.class.getName(), "Robot rotates right.");
			} else if (cmd[0].equals(ControlCommand.SHOOT.toString())) {
//				canon.rotate(1080);
				Log.d(MainActivity.class.getName(), "Robot shoots.");
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
