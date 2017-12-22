package com.ergdyne.lib;

/**
 * Created by j on 4/11/17.
 */


//Exists because I was too lazy to research a better way.
public final class AppConstants {


    public static final int SUGGESTION_THRESHOLD = 1;
    public static final int DAY_LENGTH = 24*60*60; //seconds
    public static final int WEEK_LENGTH = DAY_LENGTH *7; //seconds

    public static final int PERMISSION_WRITE_EXTERNAL = 10170;
    public static final int PERMISSION_SET_ALARM = 10171;

    public static final boolean HOUR_FORMAT = false;
    public static final String ABOUT_PAGE = "http://www.ergdyne.com/task-turner";

    public static final String DATE_FORMAT = "ddMMMyy";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String DUMB_TIME_FORMAT = "hh:mm a";
    public static final String EXPORT_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss z";
}
