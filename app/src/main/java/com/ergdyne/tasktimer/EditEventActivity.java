package com.ergdyne.tasktimer;


import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import com.ergdyne.lib.AppConstants;
import com.ergdyne.lib.DBMap;
import com.ergdyne.lib.EpicDate;
import com.ergdyne.lib.ErgAlert;
import com.ergdyne.lib.ErgFormats;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by j on 3/30/17.
 */

public class EditEventActivity extends AppCompatActivity {

    /**********************/
    //Variable definitions
    /**********************/
    public static final String EVENT_ID = "eventID";
    public static final String IS_CURRENT = "isRunning";

    private static final String TAG = "EditEventActivity";

    private long eventID;
    private boolean isRunning; //is currently running...

    private TextView eventName;
    private TextView durationDisplay;
    private AutoCompleteTextView reviseEvent;
    private TextView setStartTime;
    private TextView setEndTime;
    private TextView setStartDate;
    private TextView setEndDate;

    private String currentTaskName;
    private EpicDate currentStart;
    private EpicDate currentEnd;
    private String currentDurationDisplay;

    private EpicDate newStart;
    private EpicDate newEnd;

    private DBHelper dbHelper;


    /**********************/
    //Activity lifecycle
    /**********************/

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Change the toolbar to cancel/confirm.
        {
            try {
                final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                        .getSystemService(LAYOUT_INFLATER_SERVICE);
                final View customActionBarView = inflater.inflate(
                        R.layout.actionbar_edit, null);

                customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {

                                finish();
                            }
                        });
                customActionBarView.findViewById(R.id.actionbar_confirm).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                inputProcessing(dbHelper, isRunning, eventID, reviseEvent.getText().toString(), currentTaskName, newStart, currentStart, newEnd, currentEnd);
                            }
                        }
                );


                final ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayOptions(
                        ActionBar.DISPLAY_SHOW_CUSTOM,
                        ActionBar.DISPLAY_SHOW_CUSTOM | ActionBar.DISPLAY_SHOW_HOME
                                | ActionBar.DISPLAY_HOME_AS_UP);
                actionBar.setCustomView(customActionBarView,
                        new ActionBar.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT));
            }catch(NullPointerException e){
                Log.e(TAG,"getThemedContext returned null");
            }
        }

        setContentView(R.layout.activity_edit_event);

        //Link variables to views.
        {
            eventName = (TextView) this.findViewById(R.id.textView_edit_event);
            durationDisplay = (TextView) this.findViewById(R.id.textView_edit_duration);
            reviseEvent = (AutoCompleteTextView) this.findViewById(R.id.autoCompleteTextView_revise_event);
            setStartTime = (TextView) this.findViewById(R.id.textView_set_start_time);
            setEndTime = (TextView) this.findViewById(R.id.textView_set_end_time);
            setStartDate = (TextView) this.findViewById(R.id.textView_set_start_date);
            setEndDate = (TextView) this.findViewById(R.id.textView_set_end_date);

        }

        dbHelper = new DBHelper(this);
        isRunning = getIntent().getBooleanExtra(IS_CURRENT,false);
        eventID = getIntent().getLongExtra(EVENT_ID, DBMap.SettingsTable.defaultID);

        //Load the major variables needed to display and edit.
        {
            if(isRunning){
                //If it is the current event, the end is not yet set. We go to the Settings table for information.
                newEnd = new EpicDate(dbHelper.rightNow());
                currentEnd = new EpicDate(newEnd.sEpoch);

                Cursor settings = dbHelper.getSettings();
                settings.moveToFirst();
                currentTaskName = dbHelper.getCurrentTaskName();
                currentStart = new EpicDate(dbHelper.getCurrentStart());
                long currentDuration = newEnd.sEpoch - currentStart.sEpoch;
                currentDurationDisplay = ErgFormats.durationHMS(currentDuration);
                settings.close();


            }else {
                //Normal event is being edited, so information comes from the event table.
                Cursor event = dbHelper.getByID(DBMap.EventTable.table, eventID);
                event.moveToFirst();
                long taskID = event.getLong(event.getColumnIndex(DBMap.EventTable.taskID));

                currentTaskName = dbHelper.getTaskName(taskID);
                currentStart = new EpicDate(event.getLong(event.getColumnIndex(DBMap.EventTable.start)));
                currentEnd = new EpicDate(event.getLong(event.getColumnIndex(DBMap.EventTable.end)));
                currentDurationDisplay = event.getString(event.getColumnIndex(DBMap.EventTable.durationDisplay));
                event.close();
                newEnd = new EpicDate(currentEnd.sEpoch);
            }

            newStart = new EpicDate(currentStart.sEpoch);

        }


        //Push values to the views.
        {
            //I don't like the way this whole section flows. Like so much similar code that there should be a better way.

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,dbHelper.getTaskList());

            //Same information is filled for current as others except for the End Time/Date
            eventName.setText(currentTaskName);
            durationDisplay.setText(currentDurationDisplay);
            setStartTime.setText(currentStart.toTimeString(true,AppConstants.HOUR_FORMAT));
            setEndTime.setText((isRunning)?"N/A":currentEnd.toTimeString(true,AppConstants.HOUR_FORMAT));
            setStartDate.setText(currentStart.toDateString());
            setEndDate.setText((isRunning)?"":currentEnd.toDateString());
            reviseEvent.setThreshold(AppConstants.SUGGESTION_THRESHOLD);
            reviseEvent.setAdapter(adapter);

            /**********************/
            //Set on click listeners. Date and times are separate.
            /**********************/

            setStartTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TimePickerDialog startPicker;
                    startPicker = new TimePickerDialog(EditEventActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                            if(minute != currentStart.minute || hourOfDay != currentStart.hourOfDay ) {

                                newStart.setHourOfDay(hourOfDay);
                                newStart.setMinute(minute);
                                setStartTime.setText(EpicDate.toTimeString(hourOfDay, minute, AppConstants.HOUR_FORMAT));


                            }
                        }
                    },currentStart.hourOfDay,currentStart.minute,false); //true 24hour time
                    startPicker.setTitle(getResources().getString(R.string.hint_set_start_time));
                    startPicker.show();
                }
            });

            setStartDate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DatePickerDialog startPicker;
                    startPicker = new DatePickerDialog(EditEventActivity.this, new DatePickerDialog.OnDateSetListener(){
                        @Override
                        public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth){
                            if(dayOfMonth != currentStart.dayOfMonth ||
                                    month != currentStart.month || year != currentStart.year){
                                newStart.setYear(year);
                                newStart.setMonth(month);
                                newStart.setDayOfMonth(dayOfMonth);
                                setStartDate.setText(EpicDate.toDateString(year,month,dayOfMonth));
                            }
                        }
                    },currentStart.year,currentStart.month,currentStart.dayOfMonth);
                    startPicker.setTitle(getResources().getString(R.string.hint_set_start_date));
                    startPicker.show();

                }
            });


            if(!isRunning) {
                setEndTime.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TimePickerDialog startPicker;
                        startPicker = new TimePickerDialog(EditEventActivity.this, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                if (minute != currentEnd.minute || hourOfDay != currentEnd.hourOfDay) {

                                    newEnd.setHourOfDay(hourOfDay);
                                    newEnd.setMinute(minute);
                                    setEndTime.setText(EpicDate.toTimeString(hourOfDay, minute,AppConstants.HOUR_FORMAT));


                                }
                            }
                        }, currentEnd.hourOfDay, currentEnd.minute, false); //true 24hour time
                        startPicker.setTitle(getResources().getString(R.string.hint_set_end_time));
                        startPicker.show();
                    }
                });

                setEndDate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        DatePickerDialog startPicker;
                        startPicker = new DatePickerDialog(EditEventActivity.this, new DatePickerDialog.OnDateSetListener() {
                            @Override
                            public void onDateSet(DatePicker datePicker, int year, int month, int dayOfMonth) {
                                if (dayOfMonth != currentEnd.dayOfMonth ||
                                        month != currentEnd.month || year != currentEnd.year) {

                                    newEnd.setYear(year);
                                    newEnd.setMonth(month);
                                    newEnd.setDayOfMonth(dayOfMonth);
                                    setEndDate.setText(EpicDate.toDateString(year, month, dayOfMonth));
                                }
                            }
                        }, currentEnd.year, currentEnd.month, currentEnd.dayOfMonth);
                        startPicker.setTitle(getResources().getString(R.string.hint_set_end_date));
                        startPicker.show();

                    }
                });
            }

        }
    }




    /**********************/
    //Input processing
    /**********************/

    //This is probably the most complex section of code in the app

    //Whats with all the inputs?
    //I was trying to make this look more functional.
    //i.e. this does not follow good Object Oriented Programming Principles.
    //The dialogs get in the way of some of the functional nature, so it is all a bit more messy than required.


    private void inputProcessing(DBHelper db, Boolean running, long id,
                                 String newName, String currentName,
                                 EpicDate nStart, EpicDate cStart,
                                 EpicDate nEnd, EpicDate cEnd){
        List<String> errors = new ArrayList<>();
        boolean changeStart = false;
        boolean changeEnd = false;

        //If we have any changes then reprocess the new epochs to work in seconds since epoch.
        if(!cStart.isEqual(nStart)){
            nStart.setSEpoch();
            changeStart = true;
        }
        if(!cEnd.isEqual(nEnd)){
            nEnd.setSEpoch();
            changeEnd = true;
        }
        if(changeStart || changeEnd){
            //Check for errors...
            if(!running && nEnd.sEpoch <= nStart.sEpoch){
                errors.add(getResources().getString(R.string.err_end_before_start));
            }
            if(!running && nEnd.sEpoch > db.rightNow()){
                errors.add(getResources().getString(R.string.err_end_future));
            }
            if(nStart.sEpoch > db.rightNow()){
                errors.add(getResources().getString(R.string.err_start_future));
            }
            if(errors.size()==0){
                //Inputs look good; we have a change of some sort, so run the eventAdjustments.
                eventAdjustments(db, running,id, newName, currentName, nStart.sEpoch,cStart.sEpoch,nEnd.sEpoch,cEnd.sEpoch);
            }else{
                //We have errors, so show them.
                if(errors.size()>0){
                    ErgAlert.alert(EditEventActivity.this,errors); }
            }
        }else{
            //No events to edit, just check if name has changed and go.
            updateEvent(db, running, newName,currentName, id, cStart.sEpoch, cEnd.sEpoch);
        }
    }


    private void updateEvent(DBHelper db, boolean running, String newName, String currentName, long id, long newStartEpoch, long newEndEpoch){
        //This function updates the Event or current Event.
        String name = (!newName.equals(currentName) && newName.length() >0)? newName:currentName;

        if(!running){
            db.updateEvent(id,
                    newStartEpoch,
                    newEndEpoch,
                    db.findOrInsertTask(name),
                    newEndEpoch-newStartEpoch,
                    ErgFormats.durationHMS(newEndEpoch-newStartEpoch)
            );
        }else{
            db.updateSettings(newStartEpoch, db.findOrInsertTask(name));
        }

        finish();
    }

    private void eventAdjustments(DBHelper db, boolean running, long id, String newName, String currentName, long newStartEpoch, long currentStartEpoch, long newEndEpoch, long currentEndEpoch){
        //Some changes are being made and we may need confirmation before deleting something.
        int numberDeleted = getEndDeleted(db, newEndEpoch,currentEndEpoch).size() + getStartDeleted(db, newStartEpoch,currentStartEpoch).size();

        if(numberDeleted > 0){
            AlertDialog.Builder b = new AlertDialog.Builder(EditEventActivity.this);
            b.setTitle(getResources().getString(R.string.warn_events_deleted_title));
            b.setMessage(getResources().getString(R.string.warn_events_deleted) + numberDeleted);
            b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    adjustAndDelete(dbHelper, newStart.sEpoch, currentStart.sEpoch,newEnd.sEpoch, currentEnd.sEpoch);
                    updateEvent(dbHelper, isRunning,reviseEvent.getText().toString(),currentTaskName,eventID,newStart.sEpoch,newEnd.sEpoch);
                }
            });
            b.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            b.setIcon(android.R.drawable.ic_dialog_alert);
            b.show();

        }else{
            //Only need to adjust so no confirmation.
            adjustStartSide(db, currentStartEpoch,newStartEpoch);
            adjustEndSide(db, currentEndEpoch,newEndEpoch);

            updateEvent(db,running,newName,currentName,id,newStartEpoch,newEndEpoch);
        }

    }

    //The Adjusted events are eating entire events.
    private void adjustAndDelete(DBHelper db, long newStartEpoch, long currentStartEpoch, long newEndEpoch, long currentEndEpoch){
         //Check for changes to impacted on start side.
        List<Long> startIds = getStartDeleted(db,newStartEpoch,currentStartEpoch);

        if(startIds.size()==0){
            adjustStartSide(db,currentStartEpoch,newStartEpoch);
        }else{
            adjustStartSide(db,startIds,newStartEpoch,currentEndEpoch);
        }

        //Check for changes to impacted on end side.
        List<Long> endIds = getEndDeleted(db,newEndEpoch,currentEndEpoch);

        if(endIds.size()==0){
            adjustEndSide(db,currentEndEpoch,newEndEpoch);
        }else{
            adjustEndSide(db,endIds,currentStartEpoch,newEndEpoch);
        }

    }

    private boolean adjustStartSide(DBHelper db, long currentStartEpoch, long newStartEpoch){//ok
        //Exactly one impacted. The event that has an end that matches the the start.
        //Update the previous event.
        return updateStartSide(db, db.getByLongField(DBMap.EventTable.table,DBMap.EventTable.end,currentStartEpoch),newStartEpoch);

    }
    private boolean adjustEndSide(DBHelper db, long currentEndEpoch, long newEndEpoch){//ok
        //Exactly one impacted and easy to find because it is the one with a start that matches the end.
        //Update the next event.
        return updateEndSide(db, db.getByLongField(DBMap.EventTable.table,DBMap.EventTable.start,currentEndEpoch),newEndEpoch);

    }

    //Then overloaded versions that include deletes.
    private boolean adjustStartSide(DBHelper db, List<Long> impactedIDs, long newStartEpoch, long currentEndEpoch){
        db.deleteByID(DBMap.EventTable.table,impactedIDs);

        return updateStartSide(db, db.getInRange(
                DBMap.EventTable.table,
                null,
                DBMap.EventTable.end,
                newStartEpoch,
                currentEndEpoch-1
            ),newStartEpoch);
    }

    private boolean adjustEndSide(DBHelper db, List<Long> impactedIDs, long currentStartEpoch, long newEndEpoch){
        db.deleteByID(DBMap.EventTable.table,impactedIDs);

        //We don't know exactly where this one starts, but we know the range in which its start lies.
        //It is the only one in range because we deleted the others.
        return updateEndSide(db, db.getInRange(
                DBMap.EventTable.table,
                null,
                DBMap.EventTable.start,
                currentStartEpoch+1,
                newEndEpoch
            ),newEndEpoch);

    }


    private boolean updateStartSide(DBHelper db, Cursor impacted, long newStartEpoch){
        if(impacted.moveToFirst()){
            long impactedStart = impacted.getLong(impacted.getColumnIndex(DBMap.EventTable.start));

            long adjustedStart = impactedStart;
            long adjustedEnd = newStartEpoch;

            long adjustedDuration = adjustedEnd - adjustedStart;

            db.updateEvent(impacted.getLong(impacted.getColumnIndex(DBMap._ID)),
                    adjustedStart,
                    adjustedEnd,
                    impacted.getLong(impacted.getColumnIndex(DBMap.EventTable.taskID)),
                    adjustedDuration,
                    ErgFormats.durationHMS(adjustedDuration));
            return true;
        }else{
            //This is probably not an issue as it would only happen if adjusting the first event or extending behind the first event.
            //So it is not an issue if it happens.

            Log.w(getResources().getString(R.string.app_name),"EditEventActivity - Start cursor not found");
            return false;
        }
    }

    private boolean updateEndSide(DBHelper db, Cursor impacted, long newEndEpoch){
        long adjustedStart = newEndEpoch;

        if(impacted.moveToFirst()){

        long impactedEnd = impacted.getLong(impacted.getColumnIndex(DBMap.EventTable.end));


            long adjustedEnd = impactedEnd;

            long adjustedDuration = adjustedEnd - adjustedStart;

        db.updateEvent(impacted.getLong(impacted.getColumnIndex(DBMap._ID)),
                adjustedStart,
                adjustedEnd,
                impacted.getLong(impacted.getColumnIndex(DBMap.EventTable.taskID)),
                adjustedDuration,
                ErgFormats.durationHMS(adjustedDuration));
            return true;

        }else{
            db.updateSettings(adjustedStart,db.getCurrentTaskID());
            return true;
        }
    }

//Find which events will be deleted by an expansion of the edited event
    private List<Long> getStartDeleted(DBHelper db, long newStartEpoch, long currentStartEpoch){
        //the deleted items have newStart < itemStart < currentStart-1
        return db.getImpactedEvents(DBMap.EventTable.start,newStartEpoch,(currentStartEpoch-1));
    }

    private List<Long> getEndDeleted(DBHelper db, long newEndEpoch, long currentEndEpoch){
        return db.getImpactedEvents(DBMap.EventTable.end, (currentEndEpoch + 1),newEndEpoch);
    }





}