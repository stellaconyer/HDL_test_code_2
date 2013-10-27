package com.humdynlog;

public class Globals
{
	public static String APP_NAME = "HumDynLog";
	public static String SERVICE_MSG_ACTION = "com.humdynlog.SERVICE_MESSAGE";
	public static String ZIPPER_DONE_ACTION = "com.humdynlog.ZIPPER_DONE_ACTION";
	public static String STREAM_EXTENSION_BIN = "bin";
	public static String STREAM_EXTENSION_RAW = "raw";
	public static String STREAM_EXTENSION_CSV = "csv";
	public static String PREF_KEY_UNIQUE_PHONE_ID = "uniquePhoneID";
	public static String PREF_KEY_USERID = "userIDPref";
	public static String PREF_KEY_USE_MOBILE_INTERNET = "useMobileInternetPref";
	public static String PREF_KEY_ROOT_PATH = "rootPathPref";
	public static String PREF_KEY_RAW_STREAMS_ENABLED = "rawStreamsEnabledPref";
	public static String PREF_KEY_UPLOAD_INTERVAL_MINS = "uploadIntervalMinsPref";
	public static String PREF_KEY_ALLOW_SCREEN_BLANK = "allowScreenBlankPref";
	public static String PREF_KEY_BATTERY_MINIMUM_RUN = "batteryMinimumRunPref";
	public static String[] UPLOAD_INTERVAL_CHOICES = {"2", "5", "10", "60"};
	public static String[] BATTERY_MIN_LEVEL_CHOICES = {"10","20","30","40","50","60","70","80","90"};
	public static int MENU_ITEM_SETTINGS = 1;
	public static int MENU_ITEM_EXIT = 2;
	public static int MENU_ITEM_CLEAR = 3;
	
	public static final int STREAM_INIT = 0;
	public static final int STREAM_START = 1;
	public static final int STREAM_RESTART = 2;
	public static final int STREAM_STOP = 3;
	public static final int STREAM_DESTROY = 4;
}
