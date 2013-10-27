package com.humdynlog;

import java.io.*;
import java.util.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.*;
import android.location.*;
import android.os.*;
import android.preference.PreferenceManager;
import android.text.format.*;

public class StreamWriter implements LocationListener, SensorEventListener
{
	public static final int OUTPUT_FORMAT_TXT = 0;
	public static final int OUTPUT_FORMAT_SHORT = 1;
	public static final int OUTPUT_FORMAT_FLOAT = 2;
	public static final int OUTPUT_FORMAT_DOUBLE = 3;
	
	public Context localCtx;
	BufferedWriter logTextStream = null;
	public String logTextFileName = null;
    public double prevSecs = 0;
	public boolean isRecording = false;
	public double[] featureBuffer = null;
	public double[] featureMult = null;
	public int featureCount = 0;
	public int featureSize = 0;
	
	public String timeString(Date time)
	{
    	return DateFormat.format("yyyyMMdd_kkmmss", time).toString();
	}
	
	public DataOutputStream openStreamFile(String streamName, String timeStamp, String streamExt)
	{
		String userID = getStringPref(Globals.PREF_KEY_USERID);
		String rootPath = getStringPref(Globals.PREF_KEY_ROOT_PATH);
		String fileName = rootPath + "/" + streamName + "_" + userID + "_" + timeStamp + "." + streamExt;
		DataOutputStream dos = null;
	    try
	    {
	    	dos = new DataOutputStream(new FileOutputStream(fileName));
	    }
	    catch (FileNotFoundException e)
	    {
	        e.printStackTrace();
	    }
	    return dos;
	}
	
	public boolean closeStreamFile(DataOutputStream stream)
	{
		boolean closed = false;
		if (stream != null)
		{
	        try
	        {
	        	stream.flush();
	        	stream.close();
	        	closed = true;
	        }
	        catch (IOException e)
	        {
	            e.printStackTrace();
	        }
		}
		return closed;
	}
	
	public boolean deleteStreamFile(String streamName, String timeStamp, String streamExt)
	{
		String userID = getStringPref(Globals.PREF_KEY_USERID);
		String rootPath = getStringPref(Globals.PREF_KEY_ROOT_PATH);
		String fileName = rootPath + "/" + streamName + "_" + userID + "_" + timeStamp + "." + streamExt;
    	File streamFile = new File(fileName);
	    return streamFile.delete();
	}

	public void allocateFrameFeatureBuffer(int features)
	{
		featureBuffer = new double[features];
		featureMult = new double[features];
		Arrays.fill(featureBuffer, 0);
		Arrays.fill(featureMult, 1.0);
		featureCount = 0;
		featureSize = features;
	}
	
	public boolean pushFrameFeature(double value)
	{
		if (featureCount < featureSize)
		{
			featureBuffer[featureCount] = value;
			featureCount ++;
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public void clearFeatureFrame()
	{
		Arrays.fill(featureBuffer, 0);
		featureCount = 0;
	}

	public void writeTextLine(String[] items, DataOutputStream stream)
	{
		if (stream != null)
		{
			try
			{
				for (int i = 0; i < items.length; i ++)
				{
					// Text strings in CSV format
					if (i < (items.length - 1))
						stream.writeBytes(items[i] + ",");
					else
						stream.writeBytes(items[i]);
				}
				
				// New line for CSV files
				stream.writeByte(10);
				stream.flush();
			}
	        catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
	}
	
	public void writeFeatureFrame(double[] features, DataOutputStream stream, int outputFormat)
	{
		if (stream != null)
		{
			try
			{
				for (int i = 0; i < features.length; i ++)
				{
					switch (outputFormat)
					{
					// Text strings in CSV format
					case OUTPUT_FORMAT_TXT:
						if (i < (features.length - 1))
							stream.writeBytes(Double.toString(features[i]) + ",");
						else
							stream.writeBytes(Double.toString(features[i]));
						break;
						
					// Raw 64-bit, double big-endian format
					case OUTPUT_FORMAT_DOUBLE:
						stream.writeDouble(features[i]);
						break;
						
				    // Raw 32-bit, float big-endian format
					case OUTPUT_FORMAT_FLOAT:
						stream.writeFloat((float)features[i]);
						break;
					
						// Compact 16-bit, big-endian with variable precision multiplier
					case OUTPUT_FORMAT_SHORT:
						stream.writeShort((short)Math.round(features[i]*featureMult[i]));
						break;
					}
				}
				
				// New line for CSV files
				if (outputFormat == OUTPUT_FORMAT_TXT)
				{
					stream.writeByte(10);
				}
				
				stream.flush();
			}
	        catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void openLogTextFile(String streamName, String streamPath)
    {
		String userID = getStringPref(Globals.PREF_KEY_USERID);
		String rootPath = getStringPref(Globals.PREF_KEY_ROOT_PATH);
		logTextFileName = rootPath + "/" + streamName + "_" + userID + ".log";
	    try
	    {
			logTextStream = new BufferedWriter(new FileWriter(logTextFileName, true));
		}
	    catch (IOException e)
		{
			e.printStackTrace();
		}
    }
    
    public void writeLogTextLine(String message)
    {
        try
        {
        	Date now = new Date();
        	String prettyDate = prettyDateString(now);
			logTextStream.write(prettyDate + ": " + message);
	        logTextStream.newLine();
	        logTextStream.flush();
		}
        catch (IOException e)
		{
			e.printStackTrace();
		}
    }

    public String getStringPref(String key)
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.localCtx);
    	return prefs.getString(key, "");
    }

    public boolean getBooleanPref(String key)
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.localCtx);
    	return prefs.getBoolean(key, false);
    }
    
    
	public String prettyDateString(Date time)
	{
		return DateFormat.format("yyyy/MM/dd kk:mm:ss", time).toString();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
	}

	@Override
	public void onLocationChanged(Location location) {
	}

	@Override
	public void onProviderDisabled(String provider) {
	}

	@Override
	public void onProviderEnabled(String provider) {
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
	}
}
