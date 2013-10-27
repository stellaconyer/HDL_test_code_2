package com.humdynlog;

import java.io.*;
import java.util.Date;

import android.content.*;
import android.hardware.*;

public class LightWriter extends StreamWriter
{
	private static final String STREAM_NAME = "hdl_light";

	private static final int SENSOR_TYPE = Sensor.TYPE_LIGHT;
	private static final int SENSOR_RATE = SensorManager.SENSOR_DELAY_FASTEST;
	
	private SensorManager sensorManager = null;
	private Sensor sensor = null;
	private DataOutputStream sensorStream = null;
		
    public LightWriter(Context ctx)
    {
    	localCtx = ctx;
		String rootPath = getStringPref(Globals.PREF_KEY_ROOT_PATH);
        
        sensorManager = (SensorManager)localCtx.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(SENSOR_TYPE);

        openLogTextFile(STREAM_NAME, rootPath);
	    writeLogTextLine("Created " + this.getClass().getName() + " instance");
    }

    public void init(LightWriter sensorWriter)
    {
        sensorManager.registerListener(sensorWriter, sensor, SENSOR_RATE);
    }

    public void destroy()
    {
    	sensorManager.unregisterListener(this);
    	sensorManager = null;
    	sensor = null;
    }
    
    public void start(Date startTime)
    {
//	    prevSecs = (double)System.currentTimeMillis()/1000.0d;
	    prevSecs = ((double)startTime.getTime())/1000.0d;
	    String timeStamp = timeString(startTime);
	    sensorStream = openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_BIN);
    	
    	isRecording = true;
	    writeLogTextLine("Light recording started");
    }

    public void stop(Date stopTime)
    {
    	isRecording = false;
    	if (closeStreamFile(sensorStream))
    	{
		    writeLogTextLine("Light recording successfully stopped");
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
		    writeLogTextLine("Light recording successfully restarted");
    	}
    }
    
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if ((sensorStream != null) && isRecording)
		{
//	    	double currentSecs = ((double)event.timestamp)/1000000000.0d;
	    	double currentSecs = (double)System.currentTimeMillis()/1000.0d;
        	double diffSecs = currentSecs - prevSecs;
        	prevSecs = currentSecs;

        	double[] proxData = new double[2];
        	proxData[0] = diffSecs;
        	proxData[1] = event.values[0];
        	writeFeatureFrame(proxData, sensorStream, OUTPUT_FORMAT_FLOAT);
		}
	}
    
}
