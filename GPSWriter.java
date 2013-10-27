package com.humdynlog;

import java.io.*;
import java.util.Date;
import java.util.List;

import android.content.*;
import android.location.*;
//import android.os.Handler;

public class GPSWriter extends StreamWriter
{
	private static final String STREAM_NAME = "hdl_gps";
	private static final int GPS_SAMPLERATE = 1;		// GPS update frequency in seconds
//	private static String LOC_PROVIDER = LocationManager.PASSIVE_PROVIDER;
//	private static String LOC_PROVIDER = LocationManager.NETWORK_PROVIDER;
	private static String LOC_PROVIDER = LocationManager.GPS_PROVIDER;

	private LocationManager locManager = null;
	private DataOutputStream locStream = null;
	
    public GPSWriter(Context ctx)
    {
    	localCtx = ctx;
		String rootPath = getStringPref(Globals.PREF_KEY_ROOT_PATH);
    	openLogTextFile(STREAM_NAME, rootPath);
	    writeLogTextLine("Created " + this.getClass().getName() + " instance");

    	locManager = (LocationManager)localCtx.getSystemService(Context.LOCATION_SERVICE);
    	
    	List<String> providers = locManager.getProviders(true);
        Location loc = null;
        for (int i = providers.size()-1; i >= 0; i--)
        {
        	String prov = providers.get(i);
            loc = locManager.getLastKnownLocation(prov);
            if (loc != null)
            {
            	break;
            }
        }
        if (loc != null)
        {
    	    writeLogTextLine("Initial position lat: " + loc.getLatitude() +
    	    		                         " lon: " + loc.getLongitude() + " alt: " + loc.getAltitude());
        }    	
    	
    }

    public void init(GPSWriter gpsWriter)
    {
//    	locManager.requestLocationUpdates(LOC_PROVIDER, 0, 0, gpsWriter);
    	locManager.requestLocationUpdates(LOC_PROVIDER, GPS_SAMPLERATE*1000, 0, gpsWriter);
    }

    public void destroy()
    {
    	locManager.removeUpdates(this);
    	locManager = null;
    }
    
    public void start(Date startTime)
    {
	    prevSecs = ((double)startTime.getTime())/1000.0d;
//	    prevSecs = (double)System.currentTimeMillis()/1000.0d;
	    String timeStamp = timeString(startTime);
	    locStream = openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_BIN);
	    
    	isRecording = true;
	    writeLogTextLine("GPS recording started");
    }

    public void stop(Date stopTime)
    {
    	isRecording = false;
    	if (closeStreamFile(locStream))
    	{
		    writeLogTextLine("GPS recording successfully stopped");
		}
    	locManager.removeUpdates(this);
    }
    
    public void restart(Date time)
    {
    	DataOutputStream oldStream = locStream;
    	String timeStamp = timeString(time);
    	locStream = openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_BIN);
	    prevSecs = ((double)time.getTime())/1000.0d;
    	if (closeStreamFile(oldStream))
    	{
		    writeLogTextLine("GPS recording successfully restarted");
    	}
    }
    
	@Override
	public void onLocationChanged(Location location)
	{
		if ((locStream != null) && isRecording)
		{
//        	double fixSecs = (double)locManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getTime()/1000.0d;
	    	double fixSecs = (double)System.currentTimeMillis()/1000.0d;
        	double diffSecs = fixSecs - prevSecs;
        	prevSecs = fixSecs;

        	double[] gpsData = new double[4];
        	gpsData[0] = diffSecs;
        	gpsData[1] = locManager.getLastKnownLocation(LOC_PROVIDER).getLatitude();
        	gpsData[2] = locManager.getLastKnownLocation(LOC_PROVIDER).getLongitude();
        	gpsData[3] = locManager.getLastKnownLocation(LOC_PROVIDER).getAltitude();
        	writeFeatureFrame(gpsData, locStream, OUTPUT_FORMAT_DOUBLE);
		}
	}

}
