package com.ergdyne.tasktimer;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.widget.Toast;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by j on 4/29/17.
 */


//This was all kind of thrown together quickly. It probably requires more thought in order to have
    //a better implementation. Probably as something like tuples or a simple class.
class TutorialToast {

    static final String TUT_FIRST_EVENT = "tut_first_task";
    static final  String TUT_REPEAT_TASK = "tut_repeat_task";
    static final  String TUT_STOP_TRACKING = "tut_stop_tracking";
    static final  String TUT_EDIT_EVENT = "tut_edit_event";
    static final  String TUT_TASK_TOTALS = "tut_task_totals";
    static final  String TUT_EDIT_TASK = "tut_edit_task";
    static final  String TUT_VIEW_TASK = "tut_view_task";
    static final  String TUT_EDIT_TAG = "tut_edit_tag";
    static final  String TUT_VIEW_TAG = "tut_view_tag";
    static final  String TUT_ADD_TAG = "tut_add_tag";
    static final String TUT_OFF = "tut_off";
    static final String TUT_REMINDER = "tut_reminder";

    static final int FIRST_EVENT_LENGTH = 5;
    static final int REPEAT_TASK_LENGTH = 5;
    static final int STOP_TRACKING_LENGTH = 5;
    static final int EDIT_EVENT_LENGTH = 5;
    static final int TASK_TOTALS_LENGTH = 5;
    static final int EDIT_TASK_LENGTH = 5;
    static final int VIEW_TASK_LENGTH = 5;
    static final int EDIT_TAG_LENGTH = 5;
    static final int VIEW_TAG_LENGTH = 5;
    static final int ADD_TAG_LENGTH = 5;
    static final int REMINDER_LENGTH = 5;

    static final Map<String, Integer> maxValues = Collections.unmodifiableMap(
            new HashMap<String, Integer>(){{
                put(TUT_OFF,1);
                put(TUT_FIRST_EVENT,1);
                put(TUT_ADD_TAG,2);
                put(TUT_REPEAT_TASK,2);
                put(TUT_STOP_TRACKING,2);
                put(TUT_EDIT_EVENT,2);
                put(TUT_TASK_TOTALS,2);
                put(TUT_EDIT_TASK,2);
                put(TUT_EDIT_TAG,2);
                put(TUT_VIEW_TASK,2);
                put(TUT_VIEW_TAG,2);
                put(TUT_REMINDER,2);
            }}
    );


    private TutorialToast(){
    }

    static void init(Activity activity){
        SharedPreferences sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);

        //If we have never given any tutorial, then set all the key values
        int firstTask = sharedPreferences.getInt(TUT_FIRST_EVENT,-1);
        if(firstTask ==  -1){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for(Map.Entry<String, Integer> entry : maxValues.entrySet()){
                editor.putInt(entry.getKey(),0);
            }
            editor.commit();

        }else if(sharedPreferences.getInt(TUT_OFF,0)==0){
            //if tutorial is not off, check if any tutorials still need to run.
            for(Map.Entry<String, Integer> entry : maxValues.entrySet()){
                if( sharedPreferences.getInt(entry.getKey(),0)< entry.getValue()){
                    return;
                }
            }
            //If all the tutorial items have displayed, then switch them off for good.
            turnOff(activity);
        }
    }


    static boolean make(Activity activity, String tutorial, String text, int length){
        //bypass tutorials
        if(getCount(activity,TUT_OFF) ==1 ){return true;}

        //otherwise tutorial
        SharedPreferences sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        int count = sharedPreferences.getInt(tutorial,0);
        if(count < maxValues.get(tutorial)){

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(tutorial,count+1);
            editor.commit();

            final Toast message = Toast.makeText(activity,text,Toast.LENGTH_SHORT);
            new CountDownTimer(1000*length,1000){

                @Override
                public void onTick(long millisUntilFinished) {
                    message.show();
                }

                @Override
                public void onFinish() {
                    message.show();
                }
            }.start();


            return true;


        }
        return false;
    }

    static void remove(Activity activity, String tutorial){
        SharedPreferences sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        int max = maxValues.get(tutorial);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(tutorial,max);
        editor.commit();
    }

    static int getCount(Activity activity, String tutorial){
        SharedPreferences sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        return sharedPreferences.getInt(tutorial,0);
    }

    static void turnOff(Activity activity){
        if(getCount(activity,TUT_OFF) ==1 ){return;}
        SharedPreferences sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(TUT_OFF,1);
        editor.commit();
    }
}
