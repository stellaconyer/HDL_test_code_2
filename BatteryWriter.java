package com.humdynlog;

import java.io.*;
import java.util.Date;
import android.content.*;

public class BatteryWriter extends StreamWriter
{
	private static final String STREAM_NAME = "hdl_batt";
	private static final int STREAM_FEATURES = 2;

	private DataOutputStream sensorStream = null;

	private BroadcastReceiver batteryReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if ((sensorStream != null) && isRecording)
			{
		    	double currentSecs = (double)System.currentTimeMillis()/1000.0d;
	        	double diffSecs = currentSecs - prevSecs;
	        	prevSecs = currentSecs;

	        	double[] battData = new double[2];
	        	battData[0] = diffSecs;
	        	battData[1] = intent.getIntExtra("level", 0);
	        	writeFeatureFrame(battData, sensorStream, OUTPUT_FORMAT_SHORT);
			}
		}
	};
	
	
    public BatteryWriter(Context ctx)
    {
    	localCtx = ctx;
		String rootPath = getStringPref(Globals.PREF_KEY_ROOT_PATH);

		// When the battery level changes
    	ctx.registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

    	allocateFrameFeatureBuffer(STREAM_FEATURES);
        
        openLogTextFile(STREAM_NAME, rootPath);
	    writeLogTextLine("Created " + this.getClass().getName() + " instance");
    }

    public void init(BatteryWriter sensorWriter)
    {
    }

    public void destroy()
    {
    	localCtx.unregisterReceiver(batteryReceiver);
    }
    
    public void start(Date startTime)
    {
//	    prevSecs = (double)System.currentTimeMillis()/1000.0d;
	    prevSecs = ((double)startTime.getTime())/1000.0d;
	    String timeStamp = timeString(startTime);
	    sensorStream = openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_BIN);
    	
    	isRecording = true;
	    writeLogTextLine("Battery recording started");
    }

    public void stop(Date stopTime)
    {
    	isRecording = false;
    	if (closeStreamFile(sensorStream))
    	{
		    writeLogTextLine("Battery recording successfully stopped");
		}
    }
    
    public void restart(Date time)
    {
    	DataOutputStream oldStream = sensorStream;
    	String timeStamp = timeString(time);
    	sensorStream = openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_BIN);
	    prevSecs = ((double)time.getTime())/1000.0d;
    	if (closeStreamFile(oldStream))
    	{
		    writeLogTextLine("Battery recording successfully restarted");
    	}
    }
    
}
