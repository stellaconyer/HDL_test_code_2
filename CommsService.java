package com.humdynlog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

public class CommsService extends HumDynLogService
{
	private static final String LOGFILE_NAME = "hdl_comms";
	private static final int EMAIL_RETRY_PAUSE = 10000;
	private String localUserID = "";
	private boolean localUseMobileInternet = false;
	private boolean dispatchEmails = false;
	
	private Thread emailWorker = new Thread()
	{
		public void run()
		{
			int retryDelay = 0;
			int lastZipListLength = -1;
			boolean lastHaveInternet = false;
			while (dispatchEmails)
//			while (true)
			{
				// If there are any session Zip files, email them, then delete them
    	        boolean haveInternet = false;
    	        
    			// Check that we have the right kind of connection, if at all
    	        boolean wifi = haveConnectionType(ConnectivityManager.TYPE_WIFI);
    	        boolean mobile = haveConnectionType(ConnectivityManager.TYPE_MOBILE);
				
    			if (wifi || (!wifi && mobile && localUseMobileInternet))
    			{
    				// OK to upload any zipped sessions
    				haveInternet = true;
    			}
    			else
    			{
    				haveInternet = false;
    				retryDelay = EMAIL_RETRY_PAUSE;
    			}
    			
    			// If we acquired or lost an acceptable internet connection, notify user
    			if (lastHaveInternet != haveInternet)
    			{
    				if (haveInternet)
    				{
	    				if (wifi)
	    				{
	    					sendServiceMessage("Using WiFi internet");
	    				}
	    				else
	    				{
	    					sendServiceMessage("Using mobile internet");
	    				}
    				}
    				else
    				{
	    				sendServiceMessage("No available internet connection");
    				}
    			}
		    	lastHaveInternet = haveInternet;
		    	
		    	// If a new session zip has been created, or uploaded, notify user
		    	File[] listZipFiles = listAllZipFiles();
		    	int zipListLength = listZipFiles.length;
		    	if (zipListLength != lastZipListLength)
		    	{
		    		if (zipListLength > 0)
		    		{
		    			sendServiceMessage(zipListLength + " session(s) now pending upload");
		    		}
		    		else
		    		{
		    			sendServiceMessage("No sessions pending upload");
		    		}
		    	}
		    	
		    	if (zipListLength > 0)
		    	{
		    		String zipFileName = listZipFiles[0].getName();
	    			if (haveInternet)
	    			{
			            Mail email = new Mail();
				    	String emailResult = email.sendEmailSubmission(Globals.APP_NAME + ": " + localUserID + " " +
				    			getStringPref(Globals.PREF_KEY_UNIQUE_PHONE_ID), "", listZipFiles[0]);
				    	
			            if (emailResult.equals(""))
			            {
			            	sendServiceMessage("Session successfully uploaded");
				            listZipFiles[0].delete();
				            writeLogTextLine("Email succeeded: " + zipFileName);
				            retryDelay = 0;
			            }
			            else
			            {
				        	writeLogTextLine("Email failed: " + zipFileName + ", " + emailResult);
				        	retryDelay = EMAIL_RETRY_PAUSE;
			            }
	    			}
		    	}
		    	lastZipListLength = zipListLength;
		    	
		    	// Might want to wait a bit before trying again
		    	try
		    	{
					Thread.sleep(retryDelay);
				}
		    	catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	};
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		logFileName = LOGFILE_NAME;
		localUserID = getStringPref(Globals.PREF_KEY_USERID);
		localUseMobileInternet = getBooleanPref(Globals.PREF_KEY_USE_MOBILE_INTERNET);

		// Open log file
		openLogTextFile();
        writeLogTextLine("Comms service started");

        dispatchEmails = true;
        emailWorker.start();
		
	    return START_STICKY;
	}
	
	@Override
    public void onDestroy()
	{
		dispatchEmails = false;
	}

	public File[] listAllZipFiles()
    {
		File rootDir = new File(getStringPref(Globals.PREF_KEY_ROOT_PATH));
		FilenameFilter fnFilter = new FilenameFilter()
		{
		    public boolean accept(File dir, String name)
		    {
		        return name.contains(".zip");
		    }
		};
		return rootDir.listFiles(fnFilter);
    }
	
	private boolean haveConnectionType(int connectType)
	{
		ConnectivityManager connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo netInfo = connMgr.getNetworkInfo(connectType);
		if (netInfo != null)
			return netInfo.isAvailable();
		else
			return false;
	}
	
	private void sendServiceMessage(String message)
	{
		Intent cast = new Intent(Globals.SERVICE_MSG_ACTION);
		cast.putExtra("stringMsg", message);
		sendBroadcast(cast);
		writeLogTextLine(message);
	}
	
}
