package com.ergdyne.tasktimer;



import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.AlarmClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ergdyne.lib.AppConstants;
import com.ergdyne.lib.EpicDate;

/**
 * Created by j on 3/14/17.
 */

public class SwitchTaskFragment extends Fragment {
    //The top section on the home page. It lets someone switch tasks and create new events.

    /**********************/
    //Variable definitions
    /**********************/

    OnSwitchTaskListener mainCallback;
    OnEditCurrentListener currentCallback;

    private AutoCompleteTextView taskEdit;
    private TextView currentTask;
    private Button addTask;
    private Chronometer currentTimer;
    private Activity mainActivity;
    private DBHelper db;


    /**********************/
    //Fragment Lifecycle
    /**********************/

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception.
        try {
            mainCallback = (SwitchTaskFragment.OnSwitchTaskListener) context;
            currentCallback = (SwitchTaskFragment.OnEditCurrentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnSwitchTaskListener, OnEditCurrentListener");
        }
    }

    //Needed for pre Marshmallow support
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            try {
                mainCallback = (SwitchTaskFragment.OnSwitchTaskListener) activity;
                currentCallback = (SwitchTaskFragment.OnEditCurrentListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + " must implement OnSwitchTaskListener, OnEditCurrentListener");
            }

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_switch_task, container,false);
    }

    //todo consider moving most of this to onStart
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){
        //Connect to db
        mainActivity = getActivity();
        db = new DBHelper(mainActivity);

        //Link active components to view.
        taskEdit = (AutoCompleteTextView) view.findViewById(R.id.autoCompleteTextView_task);
        currentTask = (TextView) view.findViewById(R.id.textView_current);
        addTask = (Button) view.findViewById(R.id.button_addTask);
        currentTimer = (Chronometer) view.findViewById(R.id.chronometer_current);
        RelativeLayout currentItem = (RelativeLayout) view.findViewById(R.id.relativeLayout_current);

        //Set number of characters typed before suggestions.
        taskEdit.setThreshold(AppConstants.SUGGESTION_THRESHOLD);

        //Populate fields.
        currentTask.setText(db.getCurrentTaskName());
        currentItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                currentCallback.onEditCurrent();
            }
        });

        currentTimer.setBase(db.currentBaseTime());
        currentTimer.start();
        updateTaskList();

        {
            addTask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {switchTask(taskEdit.getText().toString());}
            });
        }



    }

    /**********************/
    //Functions used in lifecycle
    /**********************/

    private void switchTask(String newTask){

        //Check if there is an active reminder and close it... This does not actually work!
        //A note on setting reminders. Android as of writing this, doesn't make apps request permission to set alarms.
        SharedPreferences preferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        if(preferences.getBoolean(getString(R.string.saved_has_reminder),false)){
            int reminderHours = preferences.getInt(getString(R.string.saved_reminder_hours),0);
            int reminderMinutes = preferences.getInt(getString(R.string.saved_reminder_minutes),0);
            if(reminderMinutes + reminderHours >0){
              //TODO fix dismiss,
                long rightNow = db.rightNow();
                long reminderHack = preferences.getLong(getString(R.string.saved_alarm_futures),0);

                if(reminderHack>rightNow){
                    //dismissReminder(reminderHours,reminderMinutes);
                    Toast.makeText(getActivity(),getString(R.string.warn_alarm_not_dismissed),Toast.LENGTH_SHORT);
                }
            }
        }

        //Close the event and get the returned end time to use to start the next event.
        long now = db.closeEvent();

        //Update current event
        long taskID = db.findOrInsertTask(newTask);
        db.updateSettings(now,taskID);

        //If new taskID has a reminder, then we should set it.
        //This could be moved to separate method.
        long reminder = db.getReminderLength(taskID);
        SharedPreferences.Editor editor = preferences.edit();
        if(reminder >0){
            EpicDate reminderEpic = new EpicDate(db.rightNow()+reminder);

            editor.putBoolean(getString(R.string.saved_has_reminder),true);
            String reminderLabel = getString(R.string.cap_alarm_label) + db.getTaskName(taskID);
            editor.putInt(getString(R.string.saved_reminder_hours),reminderEpic.hourOfDay);
            editor.putInt(getString(R.string.saved_reminder_minutes),reminderEpic.minute);
            editor.putLong(getString(R.string.saved_alarm_futures),reminderEpic.sEpoch);

            setReminder(reminderEpic,reminderLabel);

        }else{
            editor.putBoolean(getString(R.string.saved_has_reminder),false);
            editor.putInt(getString(R.string.saved_reminder_hours),0);
            editor.putInt(getString(R.string.saved_reminder_minutes),0);
            editor.putLong(getString(R.string.saved_alarm_futures),0);
        }editor.commit();


        //Update display
        currentTask.setText(db.getCurrentTaskName());
        taskEdit.setText(null,true);
        mainCallback.onTaskSwitch();
        currentTimer.setBase(SystemClock.elapsedRealtime());
        currentTimer.start();

        updateTaskList();

    }

    public void updateTaskList(){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(mainActivity, android.R.layout.simple_list_item_1,db.getTaskList());
        taskEdit.setAdapter(adapter);
    }

    public void setReminder(EpicDate s, String label){
        if(checkAlarmPermission()) {
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            intent.putExtra(AlarmClock.EXTRA_HOUR, s.hourOfDay);
            intent.putExtra(AlarmClock.EXTRA_MINUTES,s.minute);
            intent.putExtra(AlarmClock.EXTRA_MESSAGE, label);
            intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            getActivity().startActivity(intent);
        }else{

            Toast.makeText(getActivity(),
                    getString(R.string.warn_set_alarm_permission),Toast.LENGTH_SHORT).show(); //haha
        }
    }

    //TODO this section is broken.
    public void dismissReminder(int h, int m){
        //Right now it opens the alarmclock even if skip is true, then it doesn't set the next one.
        if(checkAlarmPermission()){
            Intent intent = new Intent(AlarmClock.ACTION_DISMISS_ALARM);
            intent.putExtra(AlarmClock.EXTRA_ALARM_SEARCH_MODE,AlarmClock.ALARM_SEARCH_MODE_TIME);
            intent.putExtra(AlarmClock.EXTRA_HOUR,h);
            intent.putExtra(AlarmClock.EXTRA_MINUTES,m);
            //intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            getActivity().startActivity(intent);

        }else{
            Toast.makeText(getActivity(),
                    getString(R.string.warn_dismiss_alarm_permission),Toast.LENGTH_SHORT).show();
        }
    }


    //Well this is never used. It seems all apps have permission to set alarms in current Android.
    //Try to find the section of code in this project which sets an alarm for 1522563217.
    public boolean checkAlarmPermission(){
        if(ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.SET_ALARM)
                != PackageManager.PERMISSION_GRANTED){

            Toast.makeText(getActivity(),
                    "Asking for permission",Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.SET_ALARM},
                    AppConstants.PERMISSION_SET_ALARM
            );
        }
        return (ContextCompat.checkSelfPermission(getActivity(),
                Manifest.permission.SET_ALARM)
                == PackageManager.PERMISSION_GRANTED);
    }

    /**********************/
    //connecting to main activity
    /**********************/

    //These are where the magic happens.
    public interface OnSwitchTaskListener {
        public void onTaskSwitch();
    }

    public interface OnEditCurrentListener{
        public void onEditCurrent();
    }

    public void onListClicked(long id){
        switchTask(db.getEventTaskName(id));
    }




}
