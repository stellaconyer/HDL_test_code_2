package com.humdynlog;

import java.io.*;
import java.util.Date;
import android.content.*;
import android.os.Build;
import android.telephony.TelephonyManager;

public class MetaDataWriter extends StreamWriter
{
	private static final String STREAM_NAME = "hdl_meta";
	private static final String[] metaHeader = {"Android OS", "Manufacturer", "Brand", "Device", "Model", "Product", "Unique ID (IMEI/MEID/ESN)", "Session start", "Session end"};
	
	private DataOutputStream metaStream = null;
	private Date lastStartTime = null;
		
    public MetaDataWriter(Context ctx)
    {
    	localCtx = ctx;
		String rootPath = getStringPref(Globals.PREF_KEY_ROOT_PATH);
        
        openLogTextFile(STREAM_NAME, rootPath);
	    writeLogTextLine("Created " + this.getClass().getName() + " instance");
    }

    public void init(MetaDataWriter sensorWriter)
    {
    }

    public void destroy()
    {
    }
    
    public void start(Date startTime)
    {
    	lastStartTime = startTime;
	    String timeStamp = timeString(startTime);
	    metaStream = openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_CSV);
    	
    	isRecording = true;
	    writeLogTextLine("Metadata recording started");
    }

    public void stop(Date stopTime)
    {
    	isRecording = false;
    	
    	writeMetadata(lastStartTime, stopTime);
    	if (closeStreamFile(metaStream))
    	{
		    writeLogTextLine("Metadata recording successfully stopped");
		}
    }
    
    public void restart(Date time)
    {
    	writeMetadata(lastStartTime, time);
    	closeStreamFile(metaStream);
	    metaStream = openStreamFile(STREAM_NAME, timeString(time), Globals.STREAM_EXTENSION_CSV);
    	lastStartTime = time;
	    writeLogTextLine("Metadata recording successfully restarted");
    }
    
    public void writeMetadata(Date sessionStart, Date sessionEnd)
    {
	    // Pull together phone details and other metadata
        TelephonyManager tManager = (TelephonyManager)localCtx.getSystemService(Context.TELEPHONY_SERVICE);
	    String[] metaData = new String[9];
	    metaData[0] = Build.VERSION.RELEASE;
	    metaData[1] = Build.MANUFACTURER;
	    metaData[2] = Build.BRAND;
	    metaData[3] = Build.DEVICE;
	    metaData[4] = Build.MODEL;
	    metaData[5] = Build.PRODUCT;
	    metaData[6] = tManager.getDeviceId();
	    metaData[7] = timeString(sessionStart);
	    metaData[8] = timeString(sessionEnd);

	    // Write to file
	    writeTextLine(metaHeader, metaStream);
	    writeTextLine(metaData, metaStream);
    }
}
