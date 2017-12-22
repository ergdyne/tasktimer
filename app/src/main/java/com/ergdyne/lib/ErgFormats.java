package com.ergdyne.lib;


/**
 * Created by j on 3/14/17.
 */

public class ErgFormats {
    //TODO don't use any of this class. Calendar formats does all this.
    private ErgFormats(){}

    public static String durationHMS(long s){
        long seconds = s % 60;
        return durationHM(s) + ":" + minTwoDigit(seconds);
    }


    public static String durationHM(long s){
        return hours(s) + ":" + minTwoDigit(minutes(s));
    }

    public static int hours(long s){
        return (int) Math.floor(s/3600);
    }

    public static int minutes(long s){
        return (int) Math.floor((s % 3600)/60);
    }

    static String minTwoDigit(long x){
        String y = String.valueOf(x);
        if(x <10){
            y = "0" + x;
        }
        return y;
    }
}
