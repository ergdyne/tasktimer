package com.ergdyne.lib;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by j on 3/31/17.
 *
 * This library is meant for making it easier to deal with dates stored as seconds since 1970 epoch.
 * It exists largely because of a miss-understanding of how SQLite stores dates.
 * This acts as a layer on top of Calendar to interact with dates stored as seconds with some display functions
 * If one were to decide to change the way dates were handled, they should only have to change code here.
 * This whole class is a real mess. class "Epically Bad Date"
 */

public class EpicDate {
    //The ints are for the local timezone it seems.
    public long sEpoch;

    //These items are meant to simplify display and picker interaction, they can be changed without committing a change to the epoch.
    public int year;
    public int month;
    public int dayOfMonth;
    public int hourOfDay;
    public int minute;


    /**********************/
    //Constructors
    /**********************/
    public EpicDate(long s){
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(new Date(s*1000));

        sEpoch=s;
        year =calendar.get(Calendar.YEAR);
        month =calendar.get(Calendar.MONTH);
        dayOfMonth =calendar.get(Calendar.DAY_OF_MONTH);
        hourOfDay =calendar.get(Calendar.HOUR_OF_DAY);
        minute =calendar.get(Calendar.MINUTE);
    }

    public EpicDate(){
        Calendar calendar = GregorianCalendar.getInstance();

        sEpoch=calendar.getTimeInMillis()/1000;
        year =calendar.get(Calendar.YEAR);
        month =calendar.get(Calendar.MONTH);
        dayOfMonth =calendar.get(Calendar.DAY_OF_MONTH);
        hourOfDay =calendar.get(Calendar.HOUR_OF_DAY);
        minute =calendar.get(Calendar.MINUTE);
    }

    /**********************/
    //Setters
    //Some of these are kind of odd things that were a "quick solution" to date/time selection widgets being separate.
    //A better solution might be to build a custom combined widget.
    /**********************/

    //Set the seconds since epoch value to value and also override sub-components.
    public void setSEpoch(long s){
        setSEpoch(s, true);
    }

    //Set epoch value with option on resetting all sub-components.
    public void setSEpoch(long s,boolean all){
        this.sEpoch=s;
        if(all) {
            setComponents(s);
        }
    }

    //Reset epoch to match sub-components.
    public void setSEpoch(){
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(this.year,this.month,this.dayOfMonth,this.hourOfDay,this.minute);
        this.sEpoch = calendar.getTimeInMillis()/1000;
    }

    //Reset sub-components to match the provided epoch.
    private void setComponents(long s){
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(new Date(s * 1000));
        this.year = calendar.get(Calendar.YEAR);
        this.month = calendar.get(Calendar.MONTH);
        this.dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        this.hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        this.minute = calendar.get(Calendar.MINUTE);
    }

    //Sub Component Setters
    public void setYear(int year) {
        this.year = year;
    }
    public void setMonth(int month){
        this.month = month;
    }
    public void setDayOfMonth(int dayOfMonth){
        this.dayOfMonth = dayOfMonth;
    }
    public void setHourOfDay(int hourOfDay){
        this.hourOfDay =hourOfDay;
    }
    public void setMinute(int minute){
        this.minute = minute;
    }


    /**********************/
    //Gets
    /**********************/
    public long getDayStart(){
        return getDayStart(this.sEpoch);
    }

    public static long getDayStart(long s){
        EpicDate e = new EpicDate(s);
        e.setHourOfDay(0);
        e.setMinute(0);
        e.setSEpoch();
        return e.sEpoch;
    }

    public long getMondayStart(){
        return getMondayStart(this.sEpoch);
    }

    public static long getMondayStart(long s){
        Calendar c = GregorianCalendar.getInstance();
        c.setTime(new Date(s * 1000));
        return getDayStart(s -
                dayValue(c.get(Calendar.DAY_OF_WEEK))*
                        (24*60*60)
        );

    }

    //This might already be built into calendar. The switch case doesn't seem right.
    // You can judge lazy or dumb!
    private static int dayValue(int dayOfWeek){
        switch (dayOfWeek){
            case Calendar.TUESDAY: return 1;
            case Calendar.WEDNESDAY: return 2;
            case Calendar.THURSDAY: return 3;
            case Calendar.FRIDAY: return 4;
            case Calendar.SATURDAY: return 5;
            case Calendar.SUNDAY: return 6;
            default: return 0;
        }

    }

    public long getMonthStart(){return getMonthStart(this.sEpoch);}

    public static long getMonthStart(long s){
        EpicDate e = new EpicDate(s);
        e.setDayOfMonth(1);
        e.setSEpoch();
        return e.getDayStart();
    }

    public long getNextMonthStart(){return getNextMonthStart(this.sEpoch);}

    public static long getNextMonthStart(long s){
        EpicDate e = new EpicDate(s);
        e.setMonth(e.month + 1);
        e.setDayOfMonth(1);
        e.setSEpoch();
        return e.getDayStart();
    }


    /**********************/
    //Comparators
    /**********************/

    //For full comparison of EpicDates
    public boolean isEqual(EpicDate epicDate){
        return(this.sEpoch == epicDate.sEpoch &&
                this.minute == epicDate.minute &&
                this.hourOfDay == epicDate.hourOfDay &&
                this.dayOfMonth == epicDate.dayOfMonth &&
                this.month == epicDate.month &&
                this.year == epicDate.year);
    }


    /**********************/
    //Formatting items. These all show a general lack of knowledge of the Calendar class
    /**********************/

//TODO add local functionality for dates instead of this way.
    public String toDateString(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yy");
        return sdf.format(new Date(this.sEpoch*1000)).toString();
    }

    public static String toDateString(int year, int month, int dayOfMonth){
        EpicDate x = new EpicDate();
        x.setYear(year);
        x.setMonth(month);
        x.setDayOfMonth(dayOfMonth);
        x.setSEpoch();

        return x.toDateString();
    }

    public static String toDateTimeString(long s){
        return toTimeString(s,AppConstants.EXPORT_DATE_TIME_FORMAT);
    }


    public String toTimeString(boolean usesEpoch, boolean isProper){
        if(!usesEpoch){
            return toTimeString(this.hourOfDay,this.minute,isProper);
        }else{
            return toTimeString(this.sEpoch,isProper);
        }
    }

    public static String toTimeString(long s, boolean isProper){
        return toTimeString(s,(isProper)?AppConstants.TIME_FORMAT:AppConstants.DUMB_TIME_FORMAT);
    }

    public static String toTimeString(long s, String format){
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(s*1000)).toString();
    }

    public static String toTimeString(int h, int m, boolean isProper){
        if(isProper){
            return minTwoDigit(h)+":"+ minTwoDigit(m);
        }else{
            String xm = " AM";

            if(h>12){
                xm = " PM";
                h = h-12;
            }
            return h + ":" + minTwoDigit(m) + xm;
        }
    }


    //Uhhh? This was a weird thing to do.
    static String minTwoDigit(long x){
        String y = String.valueOf(x);
        if(x <10){
            y = "0" + x;
        }
        return y;
    }

}
