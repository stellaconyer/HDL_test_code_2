package com.humdynlog;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

public class HumDynLogService extends Service
{
	BufferedWriter logTextStream = null;
	public String logFileName = "";

	public void openLogTextFile()
    {
		String userID = getStringPref(Globals.PREF_KEY_USERID);
		String rootPath = getStringPref(Globals.PREF_KEY_ROOT_PATH);
		String logTextFileName = rootPath + "/" + logFileName + "_" + userID + ".log";
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
//        	Toast.makeText(this, message, Toast.LENGTH_LONG).show();
		}
        catch (IOException e)
		{
			e.printStackTrace();
		}
    }

	public String prettyDateString(Date time)
	{
    	return DateFormat.format("yyyy/MM/dd kk:mm:ss", time).toString();
	}
	
    public String getStringPref(String key)
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	return prefs.getString(key, "");
    }

    public boolean getBooleanPref(String key)
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	return prefs.getBoolean(key, false);
    }

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}
}
