package it.lucadentella.bluematrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.Activity;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

public class BtAsyncTask extends AsyncTask<Void, String, Void> {

	private TextView tvBatteryStatus;
	private ImageView batteryIcon;
	private EditText etText;	
	private String batteryStatusString;
	private ImageButton btToggleDisplay;

	private BufferedReader reader;
	private BufferedWriter writer;

	// FSM variables
	private int waiting_status;
	private final int WAITING_NOTHING = 0;
	private final int WAITING_BATTERY_STATUS = 1;
	private final int WAITING_DISPLAY_STATUS = 2;
	private final int WAITING_DISPLAYED_TEXT = 3;


	public BtAsyncTask(Activity activity, BluetoothSocket socket) {

		// save the calling activity
		tvBatteryStatus = (TextView)activity.findViewById(R.id.tvBatteryStatus);
		batteryIcon = (ImageView)activity.findViewById(R.id.battery_icon);
		etText = (EditText)activity.findViewById(R.id.etText);
		batteryStatusString = activity.getResources().getString(R.string.battery_status) + " ";
		btToggleDisplay = (ImageButton)activity.findViewById(R.id.btToggleDisplay);

		// prepare stream reader and writer
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// init FSM
		waiting_status = WAITING_NOTHING;
	}

	@Override
	protected Void doInBackground(Void... params) {

		// start the main loop
		receiveLoop();
		return null;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		
		//Log.d("BtAsyncTask", "Enter onProgressUpdate()");
		
		String incomingMessage = values[0];
		//Log.d("BtAsyncTask", "Incoming message: " + incomingMessage);
		
		// switch FSM status for what we're expecting from BlueMatrix
		switch(waiting_status) {
		
		// I wasn't waiting for anything
		case WAITING_NOTHING:
			
			Log.w("BtAsyncTask", "I wasn't expecting a message from BlueMatrix");
			break;
		
		// I was waiting for the battery status
		case WAITING_BATTERY_STATUS:
			
			//Log.d("BtAsyncTask", "I was waiting for the battery status");
			
			// try converting the string into float
			float batteryStatus;
			try {
				batteryStatus = Float.parseFloat(incomingMessage);				
			} catch (NumberFormatException e) {
				Log.e("BtAsyncTask", "Unable to convert the incoming message to float");
				break;
			}
			
			// update label and icon
			
			tvBatteryStatus.setText(batteryStatusString + batteryStatus + "%");
			if(batteryStatus < 10) batteryIcon.setImageResource(R.drawable.bt_1);
			else if(batteryStatus < 20) batteryIcon.setImageResource(R.drawable.bt_2);
			else if(batteryStatus < 35) batteryIcon.setImageResource(R.drawable.bt_3);
			else if(batteryStatus < 50) batteryIcon.setImageResource(R.drawable.bt_4);
			else if(batteryStatus < 80) batteryIcon.setImageResource(R.drawable.bt_5);
			else batteryIcon.setImageResource(R.drawable.bt_6);
			//Log.d("BtAsyncTask", "TextView and icon updated");
			break;			

			// I was waiting for the display status
			case WAITING_DISPLAY_STATUS:
				
				//Log.d("BtAsyncTask", "I was waiting for the display status");
										
				// update button's icon
				if("ON".equals(incomingMessage)) btToggleDisplay.setImageResource(R.drawable.bt_display_on);
				else if("OFF".equals(incomingMessage)) btToggleDisplay.setImageResource(R.drawable.bt_display_off);
				//Log.d("BtAsyncTask", "ImageButton updated");
				break;			
			
			
		// I was waiting for the text actually displayed
		case WAITING_DISPLAYED_TEXT:
			
			//Log.d("BtAsyncTask", "I was waiting for the text actually displayed");
			
			// update EditText
			etText.setText(incomingMessage);
			//Log.d("BtAsyncTask", "EditText updated");
			break;
		}
		
		// reset FSM
		waiting_status = WAITING_NOTHING;
		
		//Log.d("BtAsyncTask", "Exit onProgressUpdate()");
	}

	private void receiveLoop() {

		// loop until an error occurs or the Task is stopped
		while(true) {

			try {
				String inputLine = reader.readLine();
				publishProgress(inputLine);
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}			
		}
	}

	// Set new text to be displayed, syntax !T<text>
	public void setText(String text) {
	
		//Log.d("BtAsyncTask", "Enter setText()");
		
		String command = "!T" + text + "\n";
		sendCommand(command);
		
		//Log.d("BtAsyncTask", "Exit setText()");
	}
	
	// Get the battery status, syntax ?B
	public void getBatteryStatus() {

		//Log.d("BtAsyncTask", "Enter getBatteryStatus()");
		
		String command = "?B\n";
		sendCommand(command);
		waiting_status = WAITING_BATTERY_STATUS;
		
		//Log.d("BtAsyncTask", "Exit getBatteryStatus()");	
	}

	// Get the display status, syntax ?S
	public void getDisplayStatus() {

		//Log.d("BtAsyncTask", "Enter getDisplayStatus()");
		
		String command = "?S\n";
		sendCommand(command);
		waiting_status = WAITING_DISPLAY_STATUS;
		
		//Log.d("BtAsyncTask", "Exit getDisplayStatus()");	
	}	
	
	// Get the text displayed, syntax ?T
	public void getDisplayedText() {

		//Log.d("BtAsyncTask", "Enter getDisplayedText()");
		
		String command = "?T\n";
		sendCommand(command);
		waiting_status = WAITING_DISPLAYED_TEXT;
		
		//Log.d("BtAsyncTask", "Exit getDisplayedText()");	
	}	
	
	// Method to check if a new command can be sent
	public boolean isIdle() {
		
		return waiting_status == WAITING_NOTHING;
	}
	
	// Generic function to send commands to BlueMatrix
	public void sendCommand(String command) {

		//Log.d("BtAsyncTask", "Enter sendCommand()");
		//Log.d("BtAsyncTask", "Sending " + command);
		
		try {
			writer.write(command + "\n");
			writer.flush();
			//Log.d("BtAsyncTask", "Command sent");
		} catch (IOException e) {
			Log.e("BtAsyncTask", "Unable to send command to BlueMatrix: " + e.getMessage());
		}
		
		//Log.d("BtAsyncTask", "Exit sendCommand()");
	}
}
