package com.humdynlog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

public class ZipperService extends HumDynLogService
{
	private static final String LOGFILE_NAME = "hdl_zip";
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		logFileName = LOGFILE_NAME;
		
		// Open log file
		openLogTextFile();
        writeLogTextLine("Zip service started");
        
		Bundle params = intent.getExtras();
		
		String userID = getStringPref(Globals.PREF_KEY_USERID);
		String rootPath = getStringPref(Globals.PREF_KEY_ROOT_PATH);

		String lastStartTimeStamp = params.getString("lastStartTimeStamp");
		String lastStopTimeStamp = params.getString("lastStopTimeStamp");
		
		String uid = getStringPref(Globals.PREF_KEY_UNIQUE_PHONE_ID).replaceAll(" ", "_");
    	String sessionName = Globals.APP_NAME + "_" + userID + "_" + uid + "_" + lastStartTimeStamp + "_" + lastStopTimeStamp;

    	File[] listFiles = listStreamAndLogFiles(lastStartTimeStamp);
    	String zipFileName = rootPath + "/" + sessionName + ".zip";
    	zipFiles(listFiles, zipFileName);

    	writeLogTextLine("Zipping time stamp: " + lastStartTimeStamp);
		for (File lf: listFiles)
		{
			writeLogTextLine(lf.getName());
		}
    	
    	listFiles = listStreamFiles(lastStartTimeStamp);
    	deleteFiles(listFiles);
		
		Intent cast = new Intent(Globals.ZIPPER_DONE_ACTION);
		sendBroadcast(cast);
		
	    return START_STICKY;
	}
	
	@Override
    public void onDestroy()
	{
		
	}
	
    public File[] listStreamFiles(String uniqueTimeStamp)
    {
		File rootDir = new File(getStringPref(Globals.PREF_KEY_ROOT_PATH));
		final String uid = uniqueTimeStamp;
		FilenameFilter fnFilter = new FilenameFilter()
		{
		    public boolean accept(File dir, String name)
		    {
		        return name.contains(uid) &
		        		(name.contains(".bin") | name.contains(".csv") | name.contains(".raw") | name.contains(".txt"));
		    }
		};
		return rootDir.listFiles(fnFilter);
    }
    
    public File[] listStreamAndLogFiles(String uniqueTimeStamp)
    {
		File rootDir = new File(getStringPref(Globals.PREF_KEY_ROOT_PATH));
		final String uid = uniqueTimeStamp;
		FilenameFilter fnFilter = new FilenameFilter()
		{
		    public boolean accept(File dir, String name)
		    {
		        return (name.contains(uid) &
		        		(name.contains(".bin") | name.contains(".csv") | name.contains(".raw") | name.contains(".txt")))
		        	  | name.contains(".log");
		    }
		};
		return rootDir.listFiles(fnFilter);
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
    
    public void zipFiles(File[] listFiles, String zipFileName)
    {
    	final int bufferSize = 65536;
    	
    	try
    	{
    		BufferedInputStream origin = null;
    		FileOutputStream dest = new FileOutputStream(zipFileName);
    		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

    		byte data[] = new byte[bufferSize];

    		for(int i = 0; i < listFiles.length; i++)
    		{
    			FileInputStream fi = new FileInputStream(listFiles[i]);
    			String fileName = listFiles[i].getAbsolutePath();
    			origin = new BufferedInputStream(fi, bufferSize);
    			ZipEntry entry = new ZipEntry(fileName.substring(fileName.lastIndexOf("/") + 1));
    			out.putNextEntry(entry);
    			int count;
    			while ((count = origin.read(data, 0, bufferSize)) != -1)
    			{
    				out.write(data, 0, count);
    			}
    			out.closeEntry();
    			origin.close();
    		}
			out.flush();
			out.finish();
    		out.close();
    	}
    	catch (Exception e)
    	{
    		e.printStackTrace();
    	}
    }
    
    public void deleteFiles(File[] list)
	{
		for (File f: list)
			f.delete();
	}
	
}
