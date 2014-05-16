package it.lucadentella.bluematrix;

import java.util.TimerTask;

public class BatteryUpdaterTask extends TimerTask {

	private BtAsyncTask btAsyncTask;
	
	public BatteryUpdaterTask(BtAsyncTask btAsyncTask) {
		
		// save the BtAsyncTask instance
		this.btAsyncTask = btAsyncTask;
	}
	
	@Override
	public void run() {
		
		//Log.d("BatteryUpdaterTask", "Enter run()");
		
		// update battery status
		btAsyncTask.getBatteryStatus();
		
		//Log.d("BatteryUpdaterTask", "Exit run()");
	}
}
