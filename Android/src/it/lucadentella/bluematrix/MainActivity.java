package it.lucadentella.bluematrix;

import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.app.Activity;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SelectDeviceDialogListener {

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothSocket btSocket;
	private BtAsyncTask btAsyncTask;
	private Timer batteryUpdaterTimer;
	
	private MenuItem btOnOff;
	private ImageButton btSend;
	private ImageButton btToggleDisplay;
	private ImageView batteryIcon;
	private TextView tvBatteryStatus;
	private EditText etText;
	
	private boolean connected;
	
	private final UUID SPP_UUID = java.util.UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private final int REQUEST_ENABLE_BT = 1;
	
	/**
	 * APP INITIALIZATION
	 */
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}	
		
		// get the bluetooth adapter
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// check if the device has bluetooth capabilities
		// if not, display a toast message and close the app
		if (mBluetoothAdapter == null) {

			Toast.makeText(this, "This app requires a bluetooth capable phone",
					Toast.LENGTH_SHORT).show();
			finish();
		}
	}
		
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {

		// Import the AIPOINTE font and apply to the TextView
		String fontPath = "fonts/AIPOINTE.TTF";
		Typeface tf = Typeface.createFromAsset(getAssets(), fontPath);
		etText = (EditText)findViewById(R.id.etText);
		etText.setTypeface(tf);
		
		// check if bluetooth is enabled
		// if not, ask the user to enable it using an Intent
		if (!mBluetoothAdapter.isEnabled()) {

			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
		
		// init variables and GUI controls
		
		btSend = (ImageButton)findViewById(R.id.bt_send);
		btToggleDisplay = (ImageButton)findViewById(R.id.btToggleDisplay);
		tvBatteryStatus = (TextView)findViewById(R.id.tvBatteryStatus);
		batteryIcon = (ImageView)findViewById(R.id.battery_icon);		
		
		connected = false;
		btSend.setEnabled(false);
		btToggleDisplay.setEnabled(false);
		etText.setEnabled(false);
		
		super.onPostCreate(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_activity_actions, menu);
		
		// Get the button on the menu
		btOnOff = menu.findItem(R.id.bt_onoff);

		return super.onCreateOptionsMenu(menu);
	}


	/**
	 * GUI EVENTS
	 */

	// Respond to click on the BtOnOff button
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {

		case R.id.bt_onoff:

			// If we're not connected, create and show the dialog with the paired devices
			if(!connected) {

				Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

				String[] pairedDeviceNames = new String[pairedDevices.size()];
				int i = 0;
				for(BluetoothDevice pairedDevice : pairedDevices) {
					pairedDeviceNames[i] = pairedDevice.getName();
					i++;
				}

				SelectDeviceDialog selectDeviceDialog = SelectDeviceDialog.newInstance(pairedDeviceNames);
				selectDeviceDialog.show(getFragmentManager(), "selectDeviceDialog");
			}

			// if we're connected, disconnect
			else {
				disconnectFromDevice();
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}	
	
	// Respond to click on the btSend button
	public void btSendClicked(View view) {
		
		//Log.d("MainActivity", "Enter btSendClicked");
		
		btAsyncTask.setText(etText.getText().toString());
		
		//Log.d("MainActivity", "Exit btSendClicked");
	}
	
	// Respond to click on the btToggleDisplay button
	public void btToggleDisplayClicked(View view) {

		//Log.d("MainActivity", "Enter btToggleDisplayClicked");

		btAsyncTask.sendCommand("!S");
		btAsyncTask.getDisplayStatus();

		//Log.d("MainActivity", "Exit btToggleDisplayClicked");
	}
	
	// A device has been chosen in the SelectDeviceDialog
	@Override
	public void onChoosingPairedDevice(String deviceName) {

		connectToDevice(deviceName);
	}

	
	/**
	 * LOGIC
	 */

	// Bluetooth connection
	private void connectToDevice(String deviceName) {

		//Log.d("MainActivity", "Enter connectToDevice()");

		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		BluetoothDevice targetDevice = null;
		for(BluetoothDevice pairedDevice : pairedDevices) 
			if(pairedDevice.getName().equals(deviceName)) {
				targetDevice = pairedDevice;
				break;
			}

		// If the device was not found, toast an error and return
		if(targetDevice == null) {
			//Log.d("MainActivity", "No device found with name " + deviceName);
			Toast.makeText(this, "Device not found", Toast.LENGTH_SHORT).show();
			return;
		}

		// Create a connection to the device with the SPP UUID
		try {
			btSocket = targetDevice.createInsecureRfcommSocketToServiceRecord(SPP_UUID);
			//Log.d("MainActivity", "InsecureRfCommSocket created");
		} catch (IOException e) {
			//Log.d("MainActivity", "Unable to create InsecureRfCommSocket: " + e.getMessage());
			Toast.makeText(this, "Unable to open a serial socket with the device", Toast.LENGTH_SHORT).show();
			return;
		}

		// Connect to the device
		try {
			btSocket.connect();
			//Log.d("MainActivity", "Socket connected");
		} catch (IOException e) {
			//Log.d("MainActivity", "Unable to connect the socket: " + e.getMessage());
			Toast.makeText(this, "Unable to connect to the device", Toast.LENGTH_SHORT).show();
			return;
		}

		// Connection successful, start the async task
		btAsyncTask = new BtAsyncTask(this, btSocket);
		btAsyncTask.execute();
		//Log.d("MainActivity", "AsyncTask executed");

		// update the GUI
		connected = true;
		btSend.setEnabled(true);
		btToggleDisplay.setEnabled(true);
		etText.setEnabled(true);
		btOnOff.setIcon(R.drawable.bt_on);
		//Log.d("MainActivity", "GUI updated");
		
		// require displayed text from BlueMatrix
		btAsyncTask.getDisplayedText();
		
		// delay the request for the display status
		new Timer().schedule(new TimerTask() {
			
			@Override
			public void run() {
				btAsyncTask.getDisplayStatus();				
			}
		}, 1000);
						
		// start the task to periodically update the battery status
		batteryUpdaterTimer = new Timer();
		TimerTask batteryUpdaterTask = new BatteryUpdaterTask(btAsyncTask);
		batteryUpdaterTimer.schedule(batteryUpdaterTask, 2000, 60000);
		//Log.d("MainActivity", "BatteryUpdaterTask scheduled every 60s");

		//Log.d("MainActivity", "Exit connectToDevice()");
	}

	private void disconnectFromDevice() {

		//Log.d("MainActivity", "Enter disconnectFromDevice()");

		// cancel the batteryUpdaterTimer
		batteryUpdaterTimer.cancel();
		//Log.d("MainActivity", "BatteryUpdaterTask cancelled");
		
		// stop the async task
		btAsyncTask.cancel(true);
		//Log.d("MainActivity", "AsyncTask stopped");

		// close the socket
		try {
			btSocket.close();
			//Log.d("MainActivity", "Socket closed");
		} catch (IOException e) {
			//Log.d("MainActivity", "Unable to close socket: " + e.getMessage());
			Toast.makeText(this, "Unable to disconnect from the device", Toast.LENGTH_SHORT).show();
			return;
		}

		// Disconnection successful, update GUI
		connected = false;
		btSend.setEnabled(false);
		btToggleDisplay.setEnabled(false);
		etText.setEnabled(false);
		etText.setText("");
		btOnOff.setIcon(R.drawable.bt_off);
		btToggleDisplay.setImageResource(R.drawable.bt_display_off);
		batteryIcon.setImageResource(R.drawable.bt_1);
		tvBatteryStatus.setText(R.string.battery_status);
		//Log.d("MainActivity", "GUI updated");

		//Log.d("MainActivity", "Exit disconnectFromDevice()");
	}

	
	
	
	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}
