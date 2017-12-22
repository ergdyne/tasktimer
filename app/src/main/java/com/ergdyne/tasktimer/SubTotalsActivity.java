package com.ergdyne.tasktimer;

import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ergdyne.lib.AppConstants;
import com.ergdyne.lib.DBMap;
import com.ergdyne.lib.EpicDate;
import com.ergdyne.lib.ErgFormats;

/**
 * Created by j on 4/13/17.
 */

public class SubTotalsActivity  extends AppCompatActivity{
    //This is used to show a couple different total types.
    //It would be nice to add a range fragment to this as well.


    /**********************/
    //Variable definitions
    /**********************/

    public static final String ITEM_ID = "itemID";
    public static final String ITEM_TABLE = "itemTable";
    public static final String FROM = "from"; //uhh
    public static final String TO = "to"; //uhh
    public static final int TASK_TABLE = 0;
    public static final int TAG_TABLE = 1;

    private static final String[] UI_BINDING_FROM_TASK = new String[] {
            DBMap.EventTable.end, DBMap.EventTable.durationDisplay};
    private static final String[] UI_BINDING_FROM_TAG = new String[]{
            DBMap.TaskTable.name, DBMap.TaskTable.totalDuration
    };

    private static final int[] UI_BINDING_TO = new int[] {
            R.id.textView_row_duration_caption, R.id.textView_row_duration_duration};


    private static final String TAG = "SubTotalsActivity";

    private long itemID;
    private int itemTable;

    private long from;
    private long to;

    private ListView subTotalsList;

    private DBHelper dbHelper;

    /**********************/
    //Activity lifecycle
    /**********************/

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_pane);

        //Pull information from the intent to determine what kind of subtotals we are providing.
        dbHelper = new DBHelper(SubTotalsActivity.this);
        long rightNow = dbHelper.rightNow();
        itemID = getIntent().getLongExtra(ITEM_ID, DBMap.TaskTable.defaultID);
        itemTable = getIntent().getIntExtra(ITEM_TABLE,TASK_TABLE);
        from = getIntent().getLongExtra(FROM,0);
        to = getIntent().getLongExtra(TO,rightNow);

        //Link to views.
        FrameLayout headerFrame = (FrameLayout) findViewById(R.id.fragment_A);
        getLayoutInflater().inflate(R.layout.row_duration,headerFrame);
        TextView itemCaption = (TextView) findViewById(R.id.textView_row_duration_caption);
        TextView itemDuration = (TextView) findViewById(R.id.textView_row_duration_duration);

        FrameLayout listFrame = (FrameLayout) findViewById(R.id.fragment_B);
        subTotalsList = new ListView(SubTotalsActivity.this);
        listFrame.addView(subTotalsList, ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        itemCaption.setText((itemTable==TASK_TABLE)? dbHelper.getTaskName(itemID):dbHelper.getTagName(itemID));
        itemCaption.setTypeface(null, Typeface.BOLD);

        itemDuration.setText(ErgFormats.durationHMS( (itemTable==TASK_TABLE)?
                dbHelper.getTaskTotal(itemID,from,to):
                dbHelper.getTagTotal(itemID,from,to)
        ));

        loadList();

    }

//These loadList items that show up throughout the project would all probably work better as connection to some sort of data providers.
    private void loadList(){

        if(itemTable == TASK_TABLE) {
            Cursor cursor = dbHelper.getTasksEvents(itemID, from, to);

            SimpleCursorAdapter adapter = new SimpleCursorAdapter(SubTotalsActivity.this,
                    R.layout.row_duration, cursor, UI_BINDING_FROM_TASK, UI_BINDING_TO, 0);

            adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor c, int columnIndex) {
                    if (columnIndex == 1) {
                        long end = c.getLong(c.getColumnIndex(DBMap.EventTable.end));
                        TextView textView = (TextView) view;
                        textView.setText(EpicDate.toTimeString(end,
                                AppConstants.DATE_FORMAT + " " + AppConstants.TIME_FORMAT));
                        return true;
                    }
                    return false;
                }
            });
            subTotalsList.setAdapter(adapter);
        }else{

            Cursor cursor = dbHelper.getTagTasks(itemID, from, to);
            SimpleCursorAdapter adapter = new SimpleCursorAdapter(SubTotalsActivity.this,
                    R.layout.row_duration, cursor, UI_BINDING_FROM_TAG, UI_BINDING_TO, 0);

            adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
                @Override
                public boolean setViewValue(View view, Cursor c, int columnIndex) {
                    if (columnIndex == 2) {
                        long duration = c.getLong(c.getColumnIndex(DBMap.TaskTable.totalDuration));
                        TextView textView = (TextView) view;
                        textView.setText(ErgFormats.durationHMS(duration));
                        return true;
                    }
                    return false;
                }
            });
            subTotalsList.setAdapter(adapter);
        }




    }

    //Make the arrow in the menu to back to the right location.
    //Keeping the range the same would also be a good thing, but that would be a TODO.
    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case android.R.id.home:
                finish();
                break;
        }

        return true;
    }

}
