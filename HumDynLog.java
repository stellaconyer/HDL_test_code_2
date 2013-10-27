package com.humdynlog;

import java.io.*;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import android.app.*;
import android.content.*;
import android.content.SharedPreferences.Editor;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.TelephonyManager;
import android.text.format.*;
import android.view.*;
import android.widget.*;


public class HumDynLog extends Activity
{
	private static final String LOGFILE_NAME = "hdl_main";
	private static final int RECORDER_THREAD_PRIORITY = 8;
	
    private Button recordButton = null;
	private TextView timeText = null;
	private TextView counterText = null;
	private TextView bannerText = null;
	private TextView promptText = null;
	private boolean recording = false;
	private Handler timerHandler = null;
	private Intent commsIntent = null;
	private Intent zipperIntent = null;
	private boolean sdCardOK = false;
	BufferedWriter logTextStream = null;
	private Date lastStart = null;
	private Date lastStop = null;
	private Date lastRestart = null;
	private static boolean zipperActive = false;
	private static Date lastTimerDate = null;
	private static boolean timerRunning = false;
	private static int batteryLevel = 100;
	private static float batteryTemp = 0.0f;
	
	private RecorderThread recorder = null;
	
	private Runnable updateTimeTask = new Runnable()
	{
		public void run()
		{
			Date thisTimerDate = new Date();
			String prettyDate = prettyDateString(thisTimerDate);
			counterText.setText("Current time\n" + prettyDate);

			if (recording)
			{
				int uploadInterval = Integer.valueOf(getStringPref(Globals.PREF_KEY_UPLOAD_INTERVAL_MINS));
				int batteryMinLevel = Integer.valueOf(getStringPref(Globals.PREF_KEY_BATTERY_MINIMUM_RUN));
				if (batteryLevel <= batteryMinLevel)
				{
					writeLogTextLine("Minium battery reached. Recording stopped", true);
					clickRecord(recordButton);
				}
				else if ( ((lastTimerDate.getMinutes() % uploadInterval) != 0) &&
							((thisTimerDate.getMinutes() % uploadInterval) == 0))
				{
					writeLogTextLine("Session upload queued", true);
					Date prevRestart = lastRestart;
					restartRecording(thisTimerDate);
			    	writeLogTextLine("Session restarted", true);
					startZip(prevRestart, lastRestart);
				}
			}

			if (timerRunning)
			{
				timerHandler.postDelayed(HumDynLog.this.updateTimeTask, 500);
				lastTimerDate = thisTimerDate;
			}
		}
	};
	
	private BroadcastReceiver serviceCastRcvr = new BroadcastReceiver()
	{
		@Override
	    public void onReceive(Context context, Intent intent)
		{
			if (intent.getAction().equals(Globals.SERVICE_MSG_ACTION))
			{
				String stringMsg = intent.getStringExtra("stringMsg");
				writeLogTextLine(stringMsg, true);
			}
			if (intent.getAction().equals(Globals.ZIPPER_DONE_ACTION))
			{
				stopService(zipperIntent);
				zipperActive = false;
				writeLogTextLine("Session zipped", true);
			}
	    }
	};
	
	private BroadcastReceiver batteryReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
//			batteryLevel = intent.getIntExtra("level", 0);
			batteryLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
			writeLogTextLine("Battery level now " + batteryLevel + "%", true);
			batteryTemp = ((float)intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0))/10.0f;
			writeLogTextLine("Battery temperature now " + batteryTemp, true);
		}
	};
	
    public void clickRecord(View view)
    {
    	int batteryMinLevel = Integer.valueOf(getStringPref(Globals.PREF_KEY_BATTERY_MINIMUM_RUN));
    	if (sdCardOK && !zipperActive)
    	{
	    	Date now = new Date();
	    	String prettyDate = DateFormat.format("yyyy/MM/dd kk:mm:ss", now).toString();

	        recording = !recording;
	        if (recording)
	        {
	        	if (batteryLevel <= batteryMinLevel)
				{
			    	writeLogTextLine("Battery level insufficient to start recording", true);
			    	return;
				}
	            startRecording(now);
		    	writeLogTextLine("Session started", true);
	        	recordButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.exit, 0, 0, 0);
	            recordButton.setText("Stop");
		    	promptText.setText("Recording.\nPress Stop when finished.");
		    	timeText.setText("Session last started\n" + prettyDate);
	        }
	        else
	        {
	            stopRecording(now);
				startZip(lastRestart, lastStop);
		    	writeLogTextLine("Session stopped", true);
	        	recordButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.playgreen, 0, 0, 0);
	            recordButton.setText("Start");
		    	promptText.setText("Not recording.\nPress Start to begin.");
		    	timeText.setText("Session last stopped\n" + prettyDate);
	        }
    	}
    }

    
	private void startZip(Date begin, Date end)
	{
		Bundle params = new Bundle();
		params.putString("lastStartTimeStamp", timeString(begin));
		params.putString("lastStopTimeStamp", timeString(end));
		zipperIntent = new Intent(this, ZipperService.class);
		zipperIntent.putExtras(params);
		startService(zipperIntent);
		zipperActive = true;
	}

	private void startRecording(Date time)
	{
        sendRecorderThreadMsg(Globals.STREAM_START, time);
		lastStart = time;
		lastRestart = time;
	}

	private void stopRecording(Date time)
	{
        sendRecorderThreadMsg(Globals.STREAM_STOP, time);
		lastStop = time;
	}

	private void restartRecording(Date time)
	{
        sendRecorderThreadMsg(Globals.STREAM_RESTART, time);
        lastRestart = time;
	}
	
    public void exit()
    {
        timerHandler.removeCallbacks(updateTimeTask);
		timerRunning = false;
    	if (sdCardOK)
    	{
    		stopService(commsIntent);
    		if (zipperActive)
    		{
    			stopService(zipperIntent);
    		}
            sendRecorderThreadMsg(Globals.STREAM_DESTROY, null);
			unregisterReceiver(serviceCastRcvr);
    	}
		finish();
		
//		android.os.Process.killProcess(android.os.Process.myPid());
    }
    
    public void clear()
    {
		stopService(commsIntent);
		closeLogTextFile();
		File rootDir = new File(getStringPref(Globals.PREF_KEY_ROOT_PATH));
		File[] files = rootDir.listFiles();
		for (int i = 0; i < files.length; i++)
		{
			files[i].delete();
		}
		openLogTextFile();
    	startService(commsIntent);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
    	menu.add(0, Globals.MENU_ITEM_SETTINGS, 0, "Settings");
    	menu.add(0, Globals.MENU_ITEM_EXIT, 0, "Exit");
    	menu.add(0, Globals.MENU_ITEM_CLEAR, 0, "Clear");
		return true;
    }
    
    @Override
    public void onBackPressed()
    {
    	return;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
    	if (requestCode == Globals.MENU_ITEM_SETTINGS)
    	{
    		if (sdCardOK)
    		{
    			closeLogTextFile();
    			stopService(commsIntent);
    			openLogTextFile();
    			startService(commsIntent);
    		}
			writeLogTextLine("Changes will take effect when application restarted", true);
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
    	if (item.getItemId() == Globals.MENU_ITEM_SETTINGS)
    	{
    		if (zipperActive || recording)
    		{
    			writeLogTextLine("Cannot change preferences whilst recording", true);
    			return true;
    		}
        	Intent prefsActivity = new Intent(getBaseContext(), MainPrefs.class);
        	startActivityForResult(prefsActivity, Globals.MENU_ITEM_SETTINGS);
    		return true;
    	}
    	if (item.getItemId() == Globals.MENU_ITEM_EXIT)
    	{
    		if (zipperActive || recording)
    		{
    			writeLogTextLine("Cannot exit whilst recording", true);
    			return true;
    		}
    		exit();
    		return true;
    	}
    	if (item.getItemId() == Globals.MENU_ITEM_CLEAR)
    	{
    		if (zipperActive || recording)
    		{
    			writeLogTextLine("Cannot clear data whilst recording", true);
    			return true;
    		}
    		if (!sdCardOK)
    		{
    	        Toast.makeText(this, Globals.APP_NAME + ": Cannot clear data whilst SD card unavailable", Toast.LENGTH_LONG).show();
    			return true;
    		}
    		final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
    		dialog.setIcon(android.R.drawable.ic_dialog_alert);
    		dialog.setTitle("Clear data");
    		dialog.setMessage("This will clear all recorded data on the phone, are you sure?");
    		dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
    		{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					clear();
				}
			});
    		dialog.setNegativeButton(android.R.string.cancel, null);
    		dialog.show();
    	}
    	return true;
    }

    @Override
    public void onAttachedToWindow()
    {
		 super.onAttachedToWindow();
		 android.view.Window window = getWindow();
		 // Eliminates colour banding
		 window.setFormat(PixelFormat.RGBA_8888);
    }
    
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        setContentView(R.layout.main);
        Typeface boldFont = Typeface.createFromAsset(getAssets(), "gill_sans_mt_bold.ttf");
        Typeface lightFont = Typeface.createFromAsset(getAssets(), "gill_sans_mt.ttf");
        
        PreferenceManager.setDefaultValues(this, R.xml.mainprefs, false);

        Date now = new Date();
        lastStart = lastStop = lastRestart = now;
        
        recordButton = (Button)findViewById(R.id.logbutton);
        timeText = (TextView)findViewById(R.id.timertext);
        counterText = (TextView)findViewById(R.id.messagetext);
        bannerText = (TextView)findViewById(R.id.bannertext);
        promptText = (TextView)findViewById(R.id.prompttext);
        
        counterText.setText("Current time\n" + prettyDateString(now));
        
        timeText.setTypeface(lightFont);
        counterText.setTypeface(lightFont);
        recordButton.setTypeface(boldFont);
        bannerText.setTypeface(boldFont);
        promptText.setTypeface(boldFont);
    	promptText.setText("Initializing ...");

        lastTimerDate = now;
        timerRunning = true;
        timerHandler = new Handler();
        timerHandler.removeCallbacks(updateTimeTask);
        timerHandler.postDelayed(updateTimeTask, 100);
        
		// Check SD card is available to write on
		String sdCardState = Environment.getExternalStorageState();
        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + Globals.APP_NAME;
        
        // Store path in shared preferences accessible to all code
        setStringPref(Globals.PREF_KEY_ROOT_PATH, rootPath);
        
		if (sdCardState.equals(Environment.MEDIA_MOUNTED))
		{
			// Make output directory
	        String sdCardPath = Environment.getExternalStorageDirectory().toString();
	        File subDir = new File(sdCardPath + "/" + Globals.APP_NAME);
	        subDir.mkdir();

	        openLogTextFile();
	        writeLogTextLine("Application started", false);
	        writeLogTextLine("Preferences:", false);
	        writeLogTextLine(Globals.PREF_KEY_RAW_STREAMS_ENABLED + ": " + getBooleanPref(Globals.PREF_KEY_RAW_STREAMS_ENABLED), false);
	        writeLogTextLine(Globals.PREF_KEY_ROOT_PATH + ": " + getStringPref(Globals.PREF_KEY_ROOT_PATH), false);
	        writeLogTextLine(Globals.PREF_KEY_UPLOAD_INTERVAL_MINS + ": " + getStringPref(Globals.PREF_KEY_UPLOAD_INTERVAL_MINS), false);
	        writeLogTextLine(Globals.PREF_KEY_USE_MOBILE_INTERNET + ": " + getBooleanPref(Globals.PREF_KEY_USE_MOBILE_INTERNET), false);
	        writeLogTextLine(Globals.PREF_KEY_USERID + ": " + getStringPref(Globals.PREF_KEY_USERID), false);
	        writeLogTextLine(Globals.PREF_KEY_ALLOW_SCREEN_BLANK + ": " + getBooleanPref(Globals.PREF_KEY_ALLOW_SCREEN_BLANK), false);
	        
	        // Get unique phone hardware details
	        TelephonyManager tManager = (TelephonyManager)this.getSystemService(Context.TELEPHONY_SERVICE);
	        String uniquePhoneID = Build.MANUFACTURER + " " + Build.PRODUCT + " " + tManager.getDeviceId();
	        setStringPref(Globals.PREF_KEY_UNIQUE_PHONE_ID, uniquePhoneID);
	        writeLogTextLine("uniqueID: " + uniquePhoneID, false);
	        
	        // Check available space
	        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
			double sdAvailBytes = (double)stat.getAvailableBlocks() * (double)stat.getBlockSize();
			double sdAvailableGb = sdAvailBytes / 1073741824;
	        DecimalFormat df = new DecimalFormat("#.##");
	        writeLogTextLine("SD card ready with " + df.format(sdAvailableGb) + "Gb free", true);

	        // Instantiate and start recorder thread
	        recorder = new RecorderThread();
	        recorder.setPriority(RECORDER_THREAD_PRIORITY);
	        recorder.start();
	        while (recorder.handler == null)
	        {
	        }
	        
	        // Initialize streams
	        sendRecorderThreadMsg(Globals.STREAM_INIT, this);
	        
	        // Instantiate and start comms service
	        commsIntent = new Intent(this, CommsService.class);
	        startService(commsIntent);
	        
	        sdCardOK = true;
	    	promptText.setText("Not recording.\nPress Start to begin.");
	    	
	    	// Use this broadcast action to send messages back to the main class
	    	registerReceiver(serviceCastRcvr, new IntentFilter(Globals.SERVICE_MSG_ACTION));

	    	// This one comes back from the Zip service to say it's done zipping
	    	registerReceiver(serviceCastRcvr, new IntentFilter(Globals.ZIPPER_DONE_ACTION));
	    	
	    	// When the battery level changes
	    	registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		}
		else
		{
			sdCardOK = false;
	        Toast.makeText(this, Globals.APP_NAME + ": SD card not ready, prepare SD card and restart", Toast.LENGTH_LONG).show();
	    	promptText.setText("SD card not ready\nPrepare card and restart.");
		}
    }
    
    private void sendRecorderThreadMsg(int what, Object obj)
    {
        Message msg = new Message();
        msg.what = what;
        msg.obj = obj;
        recorder.handler.sendMessage(msg);
    }

    private void setStringPref(String key, String value)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	Editor ed = preferences.edit();
    	ed.putString(key, value);
    	ed.commit();
    }
    
    private String getStringPref(String key)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	return preferences.getString(key, "");
    }

    private void setIntPref(String key, int value)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	Editor ed = preferences.edit();
    	ed.putInt(key, value);
    	ed.commit();
    }
    
    private int getIntPref(String key)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	return preferences.getInt(key, 0);
    }

    private void setBooleanPref(String key, boolean value)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	Editor ed = preferences.edit();
    	ed.putBoolean(key, value);
    	ed.commit();
    }

    private boolean getBooleanPref(String key)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	return preferences.getBoolean(key, false);
    }

	public void openLogTextFile()
    {
		String userID = getStringPref(Globals.PREF_KEY_USERID);
		String rootPath = getStringPref(Globals.PREF_KEY_ROOT_PATH);
		String logTextFileName = rootPath + "/" + LOGFILE_NAME + "_" + userID + ".log";
	    try
	    {
			logTextStream = new BufferedWriter(new FileWriter(logTextFileName, true));
		}
	    catch (IOException e)
		{
			e.printStackTrace();
		}
    }
    
	public void closeLogTextFile()
	{
		try
		{
			logTextStream.close();
		}
	    catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
    public void writeLogTextLine(String message, boolean toast)
    {
        try
        {
        	Date now = new Date();
        	String prettyDate = prettyDateString(now);
			logTextStream.write(prettyDate + ": " + message);
	        logTextStream.newLine();
	        logTextStream.flush();
	        if (toast)
	        {
	        	Toast.makeText(this, Globals.APP_NAME + ": " + message, Toast.LENGTH_LONG).show();
	        }
		}
        catch (IOException e)
		{
			e.printStackTrace();
		}
    }

	private String prettyDateString(Date time)
	{
		return DateFormat.format("yyyy/MM/dd kk:mm:ss", time).toString();
	}
    
	public String timeString(Date time)
	{
    	return DateFormat.format("yyyyMMdd_kkmmss", time).toString();
	}
    
}
