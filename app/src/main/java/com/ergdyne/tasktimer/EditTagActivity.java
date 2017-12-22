package com.ergdyne.tasktimer;

import android.app.AlertDialog;
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
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.ergdyne.lib.AppConstants;
import com.ergdyne.lib.DBMap;
import com.ergdyne.lib.ErgAlert;

/**
 * Created by j on 4/3/17.
 */

//Maybe merge with EditTask.
    //Nope, edit task/tags have enough of their own things.
        //The only thing to do would be some common functions maybe.
public class EditTagActivity extends AppCompatActivity {

    /**********************/
    //Variable definitions
    /**********************/
    public static final String TAG_ID ="tagID";

    private long tagID;

    private static final String TAG = "EditTagActivity";

    private AutoCompleteTextView renameTag; //will be reusing a bit of code... List provider?
    private CheckBox deleteTag;

    private DBHelper dbHelper;

    private String currentTagName;


    /**********************/
    //Activity lifecycle
    /**********************/

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //The addition of the action bar is repeated elsewhere, so could be moved.
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
                                inputProcessing(dbHelper,
                                        tagID,
                                        currentTagName,
                                        renameTag.getText().toString(),
                                        deleteTag.isChecked());

                            }
                        }
                );

                //The full actionbar api is a bit of a mystery to me.
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

        //Process incoming data.
        tagID = getIntent().getLongExtra(TAG_ID, DBMap.TagTable.defaultID);

        //Link to views.
        {
            FrameLayout editTagFrame = (FrameLayout) findViewById(R.id.fragment_A);
            getLayoutInflater().inflate(R.layout.fragment_edit_task_or_tag, editTagFrame);
            TextView editedTagName = (TextView) findViewById(R.id.textView_editedTaskName);
            renameTag = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView_renameTask);
            deleteTag = (CheckBox) findViewById(R.id.checkBox_deleteTask);
            RelativeLayout reminder = (RelativeLayout) findViewById(R.id.relativeLayout_reminder);
            reminder.setVisibility(View.GONE);

            FrameLayout tasksListFrame = (FrameLayout) findViewById(R.id.fragment_B);
            ListView taskTagList = new ListView(EditTagActivity.this);
            tasksListFrame.addView(taskTagList, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView captionText = (TextView) findViewById(R.id.textView_edit_task_caption);

        //Assign values.

            dbHelper = new DBHelper(EditTagActivity.this);


            currentTagName = dbHelper.getTagName(tagID);
            editedTagName.setText(currentTagName);
            ArrayAdapter<String> tagAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dbHelper.getTagList());
            renameTag.setThreshold(AppConstants.SUGGESTION_THRESHOLD);
            renameTag.setAdapter(tagAdapter);
            captionText.setText(getResources().getString(R.string.cap_tag));


        //Bind the list of tasks.

            Cursor cursor = dbHelper.getTagTaskNames(tagID);
            SimpleCursorAdapter tagTasksAdapter = new SimpleCursorAdapter(EditTagActivity.this,
                    R.layout.row_item,
                    cursor,
                    new String[]{DBMap.TaskTable.name},
                    new int[]{R.id.textView_row_item_item},0
            );

            taskTagList.setAdapter(tagTasksAdapter);
        }

    }




    /**********************/
    //Input processing
    /**********************/
    private boolean inputProcessing(DBHelper db, long id, String currentName, String newName, boolean delete){


        if(!delete){
            //Is there a name change
            if(newName.length() == 0 || newName.equals(currentName)){

                finish();
                return true;
            }else{
                //Process name change.
                long existingTagID = db.getTagID(newName);
                if(existingTagID == 0){
                    //The tag name does not exist, so just rename the tag and exit.
                    db.updateTagName(id,newName);
                    finish();
                    return true;
                }else{
                    //Tag name exists, you can't change it. This could also just be a merge functionality similar to changing Task names.
                    ErgAlert.alert(EditTagActivity.this, getResources().getString(R.string.err_tag_exists));
                    return false;
                }
            }

        }else{
            //Confirmation dialog for deleting a tag.
            AlertDialog.Builder b = new AlertDialog.Builder(EditTagActivity.this);
            b.setMessage(getResources().getString(R.string.warn_tag_name_delete));
            b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    deleteTag(dbHelper,tagID);
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

    //Delete the tag and the relationships.
    private void deleteTag(DBHelper db, long id){

        db.deleteByID(DBMap.TagTable.table,id);


        db.deleteTagRelations(id);

    }

}
