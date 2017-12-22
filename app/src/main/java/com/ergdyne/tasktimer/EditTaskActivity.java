package com.ergdyne.tasktimer;

import android.app.AlertDialog;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.TimePicker;

import com.ergdyne.lib.AppConstants;
import com.ergdyne.lib.DBMap;
import com.ergdyne.lib.ErgAlert;
import com.ergdyne.lib.ErgFormats;

/**
 * Created by j on 4/3/17.
 */

//Similar to Edit Tag activity
public class EditTaskActivity extends AppCompatActivity {

    /**********************/
    //Variable definitions
    /**********************/
    public static final String TASK_ID ="taskID";

    private static final String TAG = "EditTaskActivity";

    private long taskID;
    private long newID;
    private long relationID;


    private TextView editedTaskName;
    private AutoCompleteTextView renameTask; //will be reusing a bit of code... Maybe a list provider is a better way?
    private CheckBox deleteTask;
    private AutoCompleteTextView tag;
    private ListView taskTagList;
    private TextView reminderDisplay;
    private RelativeLayout reminder;
    private Button addTag;

    private DBHelper dbHelper;

    private String currentTaskName;

    private long reminderLength;
    private long newReminderLength;


    /**********************/
    //Activity lifecycle
    /**********************/

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Cancel and confirm toolbar could be a separate class implemented here.
        {
            try{
                final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                        .getSystemService(LAYOUT_INFLATER_SERVICE);
                final View customActionBarView = inflater.inflate(
                        R.layout.actionbar_edit, null);

                customActionBarView.findViewById(R.id.actionbar_cancel).setOnClickListener(
                        new View.OnClickListener(){
                            @Override
                            public void onClick(View v) {

                                finish();
                            }});
                customActionBarView.findViewById(R.id.actionbar_confirm).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                inputProcessing(dbHelper,
                                        taskID,
                                        currentTaskName,
                                        renameTask.getText().toString(),
                                        deleteTask.isChecked(),reminderLength,newReminderLength);

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
            } catch(NullPointerException e){
                Log.e(TAG,"getThemedContext returned null");
            }
        }


        setContentView(R.layout.activity_two_pane);

        taskID = getIntent().getLongExtra(TASK_ID, DBMap.TaskTable.defaultID);

        //Bind the views.
        {
            FrameLayout editTaskFrame = (FrameLayout) findViewById(R.id.fragment_A);
            getLayoutInflater().inflate(R.layout.fragment_edit_task_or_tag, editTaskFrame);
            editedTaskName = (TextView) findViewById(R.id.textView_editedTaskName);
            renameTask = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView_renameTask);
            deleteTask = (CheckBox) findViewById(R.id.checkBox_deleteTask);
            reminder = (RelativeLayout) findViewById(R.id.relativeLayout_reminder);
            reminderDisplay = (TextView) findViewById(R.id.textView_reminder);

            //add tags section
            FrameLayout addTagsFrame = (FrameLayout) findViewById(R.id.fragment_B);
            getLayoutInflater().inflate(R.layout.fragment_tag_a_task, addTagsFrame);
            tag = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView_tag);
            addTag = (Button) findViewById(R.id.button_addTag);
            taskTagList = (ListView) findViewById(R.id.listView_tags);

            dbHelper = new DBHelper(EditTaskActivity.this);
        }
        //Push values to views.
        {
            currentTaskName = dbHelper.getTaskName(taskID);
            editedTaskName.setText(currentTaskName);
            ArrayAdapter<String> taskAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dbHelper.getTaskList());
            renameTask.setThreshold(AppConstants.SUGGESTION_THRESHOLD);
            renameTask.setAdapter(taskAdapter);

            //Get reminder value.
            reminderLength = dbHelper.getReminderLength(taskID);
            newReminderLength = reminderLength;
            reminderDisplay.setText(ErgFormats.durationHM(reminderLength));

            ArrayAdapter<String> tagSuggestions = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dbHelper.getTagList());
            tag.setThreshold(AppConstants.SUGGESTION_THRESHOLD);
            tag.setAdapter(tagSuggestions);


            addTag.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addTagToTask(dbHelper, tag.getText().toString(), taskID);
                    tag.setText(null);
                }
            });


            final int setHour = ErgFormats.hours(reminderLength);
            final int setMinute= ErgFormats.minutes(reminderLength);

            reminder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    TimePickerDialog picker;
                    picker = new TimePickerDialog(EditTaskActivity.this,
                            new TimePickerDialog.OnTimeSetListener(){

                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    newReminderLength = (hourOfDay*60 + minute)*60;
                                    reminderDisplay.setText(ErgFormats.durationHM(newReminderLength));
                                }
                            },setHour,setMinute,true);
                    picker.setTitle(getResources().getString(R.string.hint_set_alarm_title));
                    picker.show();
                }
            });

        }

        loadTaskTags();

        if (!TutorialToast.make(this, TutorialToast.TUT_ADD_TAG,
                getResources().getString(R.string.tut_add_tag), TutorialToast.ADD_TAG_LENGTH)) {
            if(!TutorialToast.make(this, TutorialToast.TUT_REMINDER,
                    getResources().getString(R.string.tut_reminder),TutorialToast.REMINDER_LENGTH)){
                //add new layer here if needed
            }

        }

    }


    private void loadTaskTags(){
        //the list of tags attached to the task
        Cursor cursor = dbHelper.getTaskTagNames(taskID);
        SimpleCursorAdapter taskTagsAdapter = new SimpleCursorAdapter(EditTaskActivity.this,
                R.layout.row_item,
                cursor,
                new String[]{DBMap.TagTable.name},
                new int[]{R.id.textView_row_item_item},0
        );

        taskTagList.setLongClickable(true);
        taskTagList.setAdapter(taskTagsAdapter);
        taskTagList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                relationID = id;

                AlertDialog.Builder b = new AlertDialog.Builder(EditTaskActivity.this);
                b.setPositiveButton(getResources().getString(R.string.but_delete),
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        dbHelper.deleteByID(DBMap.TaskTagTable.table,relationID);
                        loadTaskTags();
                    }
                });
                b.setNegativeButton(getResources().getString(R.string.but_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //not nothing
                            }
                        });

                b.show();

                return true;
            }
        });
    }

    /**********************/
    //functions used in Lifecycle
    /**********************/

    private void addTagToTask(DBHelper db, String tagName, long idTask){
        TutorialToast.remove(this, TutorialToast.TUT_ADD_TAG);
        db.findOrInsertTaskTag(tagName,idTask);

        //Refresh the display.
        loadTaskTags();
    }

    /**********************/
    //Input processing
    /**********************/
    //Similar to "functional" formatting from other input processing.
    private boolean inputProcessing(DBHelper db, long id, String currentName, String newName,
                                    boolean delete, long oldReminder, long newReminder){
        if(delete && id == DBMap.TaskTable.defaultID){
            ErgAlert.alert(EditTaskActivity.this, "Error cannot delete default Task");
            return false;
        }else{
            if(!delete){
                //Since confirm was press, we are going to go ahead and do a change to reminder if needed.
                if(oldReminder != newReminder){
                    db.deleteTaskReminder(id);
                    db.insertTaskReminder(id,newReminder);
                }

                //Did the name change?
                if(newName.length() == 0 || newName.equals(currentName)){

                    finish();
                    return true;
                }else{
                    //Process name change.
                    long existingTaskID = db.getTaskID(newName);
                    if(existingTaskID == 0){
                        //Task name does not exist, so just rename the task and exit.
                        db.updateTaskName(id,newName);
                        finish();
                        return true;
                    }else{
                        //Task name exists, so it is a little more complicated, ask about merging first.
                        newID = existingTaskID;
                        AlertDialog.Builder b = new AlertDialog.Builder(EditTaskActivity.this);
                        b.setMessage(getResources().getString(R.string.warn_task_name_merge));
                        b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                mergeTasks(dbHelper,taskID,newID);
                                finish();

                            }
                        });
                        b.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        });
                        b.setIcon(android.R.drawable.ic_dialog_alert);
                        b.show();

                        return true;
                    }
                }

            }else{
                //Confirm deleting
                newID = DBMap.TaskTable.defaultID;
                AlertDialog.Builder b = new AlertDialog.Builder(EditTaskActivity.this);
                b.setMessage(getResources().getString(R.string.warn_task_name_delete));
                b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //in events
                        mergeTasks(dbHelper,taskID,newID);
                        finish();

                    }
                });
                b.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                b.setIcon(android.R.drawable.ic_dialog_alert);
                b.show();
                return true;
            }
        }
    }

    private void mergeTasks(DBHelper db, long oldID, long updateID){

        db.updateTaskIDs(oldID,updateID);

        //Update the current task if needed.
        if(oldID == db.getCurrentTaskID()){
            db.updateSettings(db.getCurrentStart(),updateID);
        }

        //Delete the task unless it is default... before the default check was added, this caused an unrecoverable error.
        if(oldID != DBMap.TaskTable.defaultID){
            db.deleteByID(DBMap.TaskTable.table,oldID);
        }

        db.deleteTaskRelations(oldID);

        db.deleteTaskReminder(oldID);



    }

}
