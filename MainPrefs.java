package com.humdynlog;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.*;

public class MainPrefs extends PreferenceActivity implements OnSharedPreferenceChangeListener
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.mainprefs);

		populatePrefs();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

		populatePrefs();
	}
	
	@Override
    protected void onPause()
	{
        super.onPause();
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}
		
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		Preference pref = findPreference(key);
		validatePrefs(pref);
		populatePrefs();
	}
	
	private void validatePrefs(Preference pref)
	{
		PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
		if (pref.getKey().equals(Globals.PREF_KEY_USERID))
		{
	    	EditTextPreference textPref = (EditTextPreference)pref;
			String text = textPref.getText().toUpperCase().trim().replaceAll("[^A-Z]", "");
			((EditTextPreference) pref).setText(text);
		}
		PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}
	
	private void populatePrefs()
	{
		// Populate preference items from default shared preferences
		String key = Globals.PREF_KEY_UPLOAD_INTERVAL_MINS;
		ListPreference listPref = (ListPreference)findPreference(key);

		int entries = Globals.UPLOAD_INTERVAL_CHOICES.length;
		String entryStrings[] = new String[entries];
		String entryValues[] = new String[entries];
		for (int i = 0; i < entries; i++)
		{
			entryStrings[i] = Globals.UPLOAD_INTERVAL_CHOICES[i] + " mins";
			entryValues[i] = Globals.UPLOAD_INTERVAL_CHOICES[i] + "";
		}
		listPref.setEntries(entryStrings);
		listPref.setEntryValues(entryValues);
		listPref.setSummary(getStringPref(key) + " mins");
		
		key = Globals.PREF_KEY_BATTERY_MINIMUM_RUN;
		listPref = (ListPreference)findPreference(key);
		
		entries = Globals.BATTERY_MIN_LEVEL_CHOICES.length;
		entryStrings = new String[entries];
		entryValues = new String[entries];
		for (int i = 0; i < entries; i++)
		{
			entryStrings[i] = Globals.BATTERY_MIN_LEVEL_CHOICES[i] + "%";
			entryValues[i] = Globals.BATTERY_MIN_LEVEL_CHOICES[i] + "";
		}
		listPref.setEntries(entryStrings);
		listPref.setEntryValues(entryValues);
		listPref.setSummary(getStringPref(key) + "%");

		
		key = Globals.PREF_KEY_USE_MOBILE_INTERNET;
		CheckBoxPreference boxPref = (CheckBoxPreference)findPreference(key);
		boxPref.setChecked(getBooleanPref(key));
		
		key = Globals.PREF_KEY_RAW_STREAMS_ENABLED;
		boxPref = (CheckBoxPreference)findPreference(key);
		boxPref.setChecked(getBooleanPref(key));

		key = Globals.PREF_KEY_ALLOW_SCREEN_BLANK;
		boxPref = (CheckBoxPreference)findPreference(key);
		boxPref.setChecked(getBooleanPref(key));
		
		key = Globals.PREF_KEY_USERID;
		EditTextPreference textPref = (EditTextPreference)findPreference(key);
		textPref.setSummary(getStringPref(key));
	}
	
    private void setStringPref(String key, String value)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	Editor ed = preferences.edit();
    	ed.putString(key, value);
    	ed.commit();
    }
    
    private String getStringPref(String key)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	return preferences.getString(key, "");
    }

    private void setIntPref(String key, int value)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	Editor ed = preferences.edit();
    	ed.putInt(key, value);
    	ed.commit();
    }
    
    private int getIntPref(String key)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	return preferences.getInt(key, 0);
    }

    private void setBooleanPref(String key, boolean value)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	Editor ed = preferences.edit();
    	ed.putBoolean(key, value);
    	ed.commit();
    }

    private boolean getBooleanPref(String key)
    {
    	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    	return preferences.getBoolean(key, false);
    }
	

}
