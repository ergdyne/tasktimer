package com.ergdyne.tasktimer;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.ergdyne.lib.DBMap;
import com.ergdyne.lib.EpicDate;
import com.ergdyne.lib.ErgFormats;

/**
 * Created by j on 3/22/17.
 */

public class TaskTotalsFragment extends ListFragment {
    //Used with a range fragment.

    OnTaskLongClickListener mainCallback;
    OnTaskClickListener clickCallback;

    /**********************/
    //variable definitions
    /**********************/
    private DBHelper db;
    private int layout = R.layout.row_duration;


    private static final String[] UI_BINDING_FROM = new String[] {
            DBMap.TaskTable.name, DBMap.TaskTable.totalDuration};
    private static final int[] UI_BINDING_TO = new int[] {
            R.id.textView_row_duration_caption, R.id.textView_row_duration_duration};

    private long fromHack;
    private long toHack;

    /**********************/
    //Fragment lifecycle
    //not much needed
    /**********************/

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        try{
            mainCallback = (OnTaskLongClickListener) context;
            clickCallback = (OnTaskClickListener) context;
        }catch(ClassCastException e){
            throw new ClassCastException(context.toString()
                    + " must implement OnTaskLongClickListener and OnTaskClickListener");
        }
    }

    //Required as long as pre marshmallow is supported.
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                mainCallback = (OnTaskLongClickListener) activity;
                clickCallback = (OnTaskClickListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + " must implement OnTaskLongClickListener and OnTaskClickListener");
            }
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = new DBHelper(getActivity());

    }

    @Override
    public void onStart(){
        super.onStart();

        long to = db.rightNow();
        long from = EpicDate.getDayStart(to);
        displayTaskTotals(from, to);

    }

    /**********************/
    //function used in lifecycle
    /**********************/

    private void displayTaskTotals(long from, long to){

        fromHack = from;
        toHack = to;
        Cursor cursor = db.getTaskTotalsBetween(from,to);

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                layout,
                cursor,
                UI_BINDING_FROM,
                UI_BINDING_TO,0
        );
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor c, int columnIndex) {
                if(columnIndex == 2){ //uhh 2?
                    long durationSum = c.getLong(c.getColumnIndex(DBMap.TaskTable.totalDuration));
                    TextView textView = (TextView) view;
                    textView.setText(ErgFormats.durationHMS(durationSum));
                    return true;
                }
                return false;
            }
        });

        setListAdapter(adapter);
        getListView().setClickable(true);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clickCallback.onTaskClick(id,fromHack,toHack);
            }
        });
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mainCallback.onTaskLongClick(id);
                return true;
            }
        });

    }


    /**********************/
    //Connection to mainactivity
    /**********************/

    public interface OnTaskLongClickListener {
        public void onTaskLongClick(long id);
    }

    public interface OnTaskClickListener{
        public void onTaskClick(long id, long from, long to);
    }

    public void onTaskRangeChange(long from, long to){
        displayTaskTotals(from, to);
    }



}