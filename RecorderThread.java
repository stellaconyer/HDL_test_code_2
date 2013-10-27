package com.humdynlog;

import java.util.*;

import android.content.*;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;

public class RecorderThread extends Thread
{
	public volatile Handler handler;

	private static final int WAKE_LOCK_ACQUIRE_PAUSE = 1000;
	private AccelWriter accelWriter = null;
	private ProxWriter proxWriter = null;
	private LightWriter lightWriter = null;
	private AudioWriter audioWriter = null;
	private GPSWriter gpsWriter = null;
	private BatteryWriter battWriter = null;
	private CompassWriter cmpssWriter = null;
	private MetaDataWriter metaWriter = null;
	
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;
	private int prevScreenTimeout = 0;
	private int prevScreenBrightness = 0;
	private Timer keepAliveTimer = null;

	public Context localCtx = null;
	
	public void run()
    {
        Looper.prepare();
        handler = new Handler()
        {
            public void handleMessage(Message msg)
            {
            	switch (msg.what)
            	{
            	case Globals.STREAM_INIT:
            		localCtx = (Context)msg.obj;
            		initStreams();
            		break;
            		
            	case Globals.STREAM_START:
            		startStreams((Date)msg.obj);
            		break;
            		
            	case Globals.STREAM_RESTART:
            		restartStreams((Date)msg.obj);
            		break;
            		
            	case Globals.STREAM_STOP:
            		stopStreams((Date)msg.obj);
            		break;
            		
            	case Globals.STREAM_DESTROY:
            		destroyStreams();
            		break;
            	}
            }
        };
        Looper.loop();
    }
	
	private void initStreams()
	{
		accelWriter = new AccelWriter(localCtx);
		audioWriter = new AudioWriter(localCtx);
		proxWriter = new ProxWriter(localCtx);
		lightWriter = new LightWriter(localCtx);
		gpsWriter = new GPSWriter(localCtx);
		battWriter = new BatteryWriter(localCtx);
		cmpssWriter = new CompassWriter(localCtx);
		metaWriter = new MetaDataWriter(localCtx);
		
		accelWriter.init(accelWriter);
		proxWriter.init(proxWriter);
		lightWriter.init(lightWriter);
		gpsWriter.init(gpsWriter);
		battWriter.init(battWriter);
		cmpssWriter.init(cmpssWriter);
		metaWriter.init(metaWriter);

		screenHold();
	}

	private void startStreams(Date now)
	{
    	// Start recording
    	accelWriter.start(now);
    	proxWriter.start(now);
    	lightWriter.start(now);
    	audioWriter.start(now);
    	gpsWriter.start(now);
    	battWriter.start(now);
    	cmpssWriter.start(now);
    	metaWriter.start(now);
	}
	
	private void restartStreams(Date now)
	{
		// Restart recording
		accelWriter.restart(now);
		proxWriter.restart(now);
		lightWriter.restart(now);
		audioWriter.restart(now);
		gpsWriter.restart(now);
		battWriter.restart(now);
		cmpssWriter.restart(now);
		metaWriter.restart(now);
	}
	
	private void stopStreams(Date now)
	{
        // Stop recording
    	accelWriter.stop(now);
    	proxWriter.stop(now);
    	lightWriter.stop(now);
    	audioWriter.stop(now);
    	gpsWriter.stop(now);
    	battWriter.stop(now);
    	cmpssWriter.stop(now);
    	metaWriter.stop(now);
	}
	
	private void destroyStreams()
	{
		screenRelease();
    	
    	// Delete StreamWriters
    	accelWriter.destroy();
    	proxWriter.destroy();
    	lightWriter.destroy();
    	audioWriter.destroy();
    	gpsWriter.destroy();
    	battWriter.destroy();
    	cmpssWriter.destroy();
    	metaWriter.destroy();
	}

    private void screenHold()
    {
		// Attempt to keep screen on (v3)
    	if (!getBooleanPref(Globals.PREF_KEY_ALLOW_SCREEN_BLANK))
    	{
			try
			{
				prevScreenTimeout = Settings.System.getInt(localCtx.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT);
				prevScreenBrightness = Settings.System.getInt(localCtx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
			}
			catch (SettingNotFoundException e)
			{
			}
			Settings.System.putInt(localCtx.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, -1);
			Settings.System.putInt(localCtx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 0);
    		
	    	powerManager = (PowerManager)localCtx.getSystemService(Context.POWER_SERVICE);
    		
	    	keepAliveTimer = new Timer();
			keepAliveTimer.schedule(new TimerTask()
			{
				@Override
				public void run()
				{
					wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, "AccelOn");
					wakeLock.acquire();
					
//					if (!powerManager.isScreenOn())
//					{
//						writeLogTextLine("Screen off during record", false);
//					}
				}
				
			}, 0, WAKE_LOCK_ACQUIRE_PAUSE);
    	}
    }
    
    private void screenRelease()
    {
    	// Return screen on/off state to previous (v3)
    	if (!getBooleanPref(Globals.PREF_KEY_ALLOW_SCREEN_BLANK))
    	{
    		if (wakeLock != null)
    		{
    			if (wakeLock.isHeld())
		    	{
		    		wakeLock.release();
		    	}
    		}
			Settings.System.putInt(localCtx.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, prevScreenBrightness);
			Settings.System.putInt(localCtx.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, prevScreenTimeout);
			keepAliveTimer.cancel();
    	}
    }
	
	private boolean getBooleanPref(String key)
    {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(localCtx);
    	return prefs.getBoolean(key, false);
    }
}
