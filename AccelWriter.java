package com.humdynlog;

import android.content.*;
import android.hardware.*;
import java.io.*;
import java.util.Arrays;
import java.util.Date;

public class AccelWriter extends StreamWriter
{
	private static String STREAM_NAME = "hdl_accel";
	
	private static final int SENSOR_TYPE = Sensor.TYPE_ACCELEROMETER;
	private static final int SENSOR_RATE = SensorManager.SENSOR_DELAY_FASTEST;
	
	private static final int STREAM_FEATURES = 26;
	private static final double SENSOR_FRAME_DURATION = 1.0;			// Frame length in seconds
	private static final double SENSOR_MAX_RATE = 100.0;				// Assumed maximum accelerometer sampling rate
	private static int FFT_SIZE = 128;
	private static double[] FREQ_BANDEDGES = {0,1,3,6,10};

	private SensorManager sensorManager = null;
	private Sensor sensor = null;
	private DataOutputStream sensorStreamRaw = null;
    private DataOutputStream sensorStreamFeatures = null;
    
	private double prevSecs;
	private double prevFrameSecs;
	private double frameTimer = 0;
	private double[][] frameBuffer = null;
	private double[] fftBufferR = null;
	private double[] fftBufferI = null;
	private int frameSamples = 0;
	private int frameBufferSize = 0;

	private FFT featureFFT = null;
    private Window featureWin = null;
    private static int[] freqBandIdx = null;
    
    public AccelWriter(Context ctx)
    {
    	localCtx = ctx;

    	sensorManager = (SensorManager)localCtx.getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(SENSOR_TYPE);

        openLogTextFile(STREAM_NAME, getStringPref(Globals.PREF_KEY_ROOT_PATH));
	    writeLogTextLine("Created " + this.getClass().getName() + " instance");
	    writeLogTextLine("Raw streaming: " + getBooleanPref(Globals.PREF_KEY_RAW_STREAMS_ENABLED));
        
        // Allocate frame buffer, assuming a maximum sampling rate
        frameBufferSize = (int)Math.ceil(SENSOR_MAX_RATE/SENSOR_FRAME_DURATION);
        frameBuffer = new double[frameBufferSize][3];
        writeLogTextLine("Accelerometer maximum frame size (samples): " + frameBufferSize);
        writeLogTextLine("Accelerometer maximum frame duation (secs): " + SENSOR_FRAME_DURATION);
        
        allocateFrameFeatureBuffer(STREAM_FEATURES);

        featureFFT = new FFT(FFT_SIZE);
	    featureWin = new Window(frameBufferSize);

        fftBufferR = new double[FFT_SIZE];
    	fftBufferI = new double[FFT_SIZE];

    	freqBandIdx = new int[FREQ_BANDEDGES.length];
	    for (int i = 0; i < FREQ_BANDEDGES.length; i ++)
	    {
	    	freqBandIdx[i] = Math.round((float)FREQ_BANDEDGES[i]*((float)FFT_SIZE/(float)SENSOR_MAX_RATE));
	    	writeLogTextLine("Frequency band edge " + i + ": " + Integer.toString(freqBandIdx[i]));
	    }
    }
    
    public void init(AccelWriter sensorWriter)
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
	    prevSecs = ((double)startTime.getTime())/1000.0d;
//	    prevSecs = ((double)System.currentTimeMillis())/1000.0d;
	    writeLogTextLine("prevSecs: " + prevSecs);
	    
	    prevFrameSecs = prevSecs;
	    frameTimer = 0;
	    frameSamples = 0;

	    // Ensure frame buffer is cleared
		for (double[] row: frameBuffer)
			Arrays.fill(row, 0);
	    
	    // Create new stream file(s)
	    String timeStamp = timeString(startTime);
	    
	    if (getBooleanPref(Globals.PREF_KEY_RAW_STREAMS_ENABLED))
	    {
	    	sensorStreamRaw = openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_RAW);
	    }
	    sensorStreamFeatures = openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_BIN);

    	isRecording = true;
	    writeLogTextLine("Accelerometry recording started");
    }
    
    public void stop(Date stopTime)
    {
    	isRecording = false;
    	if (getBooleanPref(Globals.PREF_KEY_RAW_STREAMS_ENABLED))
    	{
	    	if (closeStreamFile(sensorStreamRaw))
	    	{
			    writeLogTextLine("Raw accelerometry recording successfully stopped");
			}
    	}
    	if (closeStreamFile(sensorStreamFeatures))
    	{
		    writeLogTextLine("Accelerometry feature recording successfully stopped");
		}
    }

    public void restart(Date time)
    {
    	DataOutputStream oldRaw = sensorStreamRaw;
        DataOutputStream oldFeatures = sensorStreamFeatures;
    	String timeStamp = timeString(time);
	    if (getBooleanPref(Globals.PREF_KEY_RAW_STREAMS_ENABLED))
	    {
	    	sensorStreamRaw = openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_RAW);
	    }
	    sensorStreamFeatures = openStreamFile(STREAM_NAME, timeStamp, Globals.STREAM_EXTENSION_BIN);
	    prevSecs = ((double)time.getTime())/1000.0d;
    	if (getBooleanPref(Globals.PREF_KEY_RAW_STREAMS_ENABLED))
    	{
	    	if (closeStreamFile(oldRaw))
	    	{
			    writeLogTextLine("Raw accelerometry recording successfully restarted");
			}
    	}
    	if (closeStreamFile(oldFeatures))
    	{
		    writeLogTextLine("Accelerometry feature recording successfully restarted");
		}
    }
    
	@Override
	public void onSensorChanged(SensorEvent event)
	{
		if (isRecording)
		{
//	    	double currentSecs = ((double)event.timestamp)/1000000000.0d;
//			if (prevSecs == 0)
//			{
//				prevSecs = currentSecs;
//			}
	    	double currentSecs = (double)(System.currentTimeMillis())/1000.0d;
			
        	double diffSecs = currentSecs - prevSecs;
        	prevSecs = currentSecs;

        	double X = event.values[0];
        	double Y = event.values[1];
        	double Z = event.values[2];
        	
        	// Write out raw accelerometry data, if enabled
        	if (getBooleanPref(Globals.PREF_KEY_RAW_STREAMS_ENABLED))
        	{
	        	double[] accData = new double[4];
	        	accData[0] = diffSecs;
	        	accData[1] = X;
	        	accData[2] = Y;
	        	accData[3] = Z;
	        	writeFeatureFrame(accData, sensorStreamRaw, OUTPUT_FORMAT_FLOAT);
        	}

	        // Store measurement in frame buffer
        	frameBuffer[frameSamples][0] = X;
        	frameBuffer[frameSamples][1] = Y;
        	frameBuffer[frameSamples][2] = Z;
        	frameSamples ++;
        	frameTimer += diffSecs;

        	// Frame complete?
        	if ((frameTimer >= SENSOR_FRAME_DURATION) || (frameSamples == (frameBufferSize - 1)))
        	{
        		clearFeatureFrame();
        		
                double fN = (double)frameSamples;
//                if (prevFrameSecs == 0)
//                {
//                	prevFrameSecs = currentSecs;
//                }
                double diffFrameSecs = currentSecs - prevFrameSecs;
                prevFrameSecs = currentSecs;
                pushFrameFeature(diffFrameSecs);
        		pushFrameFeature(fN);
        		
        		// Calculate accelerometry features for X,Y,Z
        		for (int i = 0; i < 3; i ++)
        		{
            		// Mean
        			double mean = 0;
        			for (int j = 0; j < frameSamples; j ++)
        				mean += frameBuffer[j][i];
        			mean /= fN;
        			pushFrameFeature(mean);
            		
        			double accum;

        			// Absolute central moment
        			accum = 0;
        			for (int j = 0; j < frameSamples; j ++)
        				accum += Math.abs(frameBuffer[j][i] - mean);
        			pushFrameFeature(accum/fN);
        			
        			// Standard deviation
        			accum = 0;
        			for (int j = 0; j < frameSamples; j ++)
        				accum += (frameBuffer[j][i] - mean)*(frameBuffer[j][i] - mean);
        			pushFrameFeature(Math.sqrt(accum/fN));

        			// Max deviation
        			accum = 0;
        			for (int j = 0; j < frameSamples; j ++)
        				accum = Math.max(Math.abs(frameBuffer[j][i] - mean),accum);
        			pushFrameFeature(accum);
        			
                    // Frequency analysis with zero-padding
    	        	Arrays.fill(fftBufferR, 0);
    	        	Arrays.fill(fftBufferI, 0);
    	        	
    	        	// Drop accel. values into FFT buffer
    	        	for (int j = 0; j < frameSamples; j++)
    	        	{
    	        		fftBufferR[j] = frameBuffer[j][i] - mean;
    	        	}

    	        	// In-place windowing
    	        	featureWin.applyWindow(fftBufferR);
    	        	
    	        	// In-place FFT
    	        	featureFFT.fft(fftBufferR, fftBufferI);

    	        	// Get PSD across frequency band ranges
    	        	for (int b = 0; b < (FREQ_BANDEDGES.length - 1); b ++)
    	        	{
    	        		int j = freqBandIdx[b];
    	        		int k = freqBandIdx[b+1];
    	        		accum = 0;
    	        		for (int h = j; h < k; h ++)
    	        		{
    	        			accum += fftBufferR[h]*fftBufferR[h] + fftBufferI[h]*fftBufferI[h];
    	        		}
    	        		pushFrameFeature(accum/((double)(k - j)));
    	        	}
        		}
        		
	        	// Write out features
	        	writeFeatureFrame(featureBuffer, sensorStreamFeatures, OUTPUT_FORMAT_FLOAT);
	        	
        		// Reset frame buffer counters
        		frameSamples = 0;
        		frameTimer = 0;

        		// Ensure buffer is zero-padded
        		for (double[] row: frameBuffer)
        			Arrays.fill(row, 0);
        	}

		}
	}

}
