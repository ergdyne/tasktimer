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
public class TagTotalsFragment extends ListFragment {
    //Pairs with a range fragment to provide totals.

    OnTagLongClickListener mainCallback;
    OnTagClickListener clickCallback;
    OnNoTags noTagsCallback;

    /**********************/
    //variable definitions
    /**********************/
    private DBHelper db;
    private int layout = R.layout.row_duration;

    private long toHack;
    private long fromHack;


    private static final String[] UI_BINDING_FROM = new String[] {
            DBMap.TagTable.name, DBMap.TagTable.totalDuration};
    private static final int[] UI_BINDING_TO = new int[] {
            R.id.textView_row_duration_caption, R.id.textView_row_duration_duration};

    /**********************/
    //Fragment lifecycle
    //not much needed
    /**********************/

    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        try{
            mainCallback = (OnTagLongClickListener) context;
            noTagsCallback = (OnNoTags) context;
            clickCallback = (OnTagClickListener) context;
        }catch(ClassCastException e){
            throw new ClassCastException(context.toString()
                    + " must implement OnTagLongClickListener, OnNoTags, OnTagClickListener");
        }


    }

    //Required as long as pre marshmallow is supported.
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            try {
                mainCallback = (OnTagLongClickListener) activity;
                noTagsCallback = (OnNoTags) activity;
                clickCallback = (OnTagClickListener) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + " must implement OnTagLongClickListener, OnNoTags, OnTagClickListener");
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
        displayTagTotals(from, to);
    }

    /**********************/
    //function used in lifecycle
    /**********************/

    private void displayTagTotals(long from, long to){

        Cursor cursor = db.getTagTotalsBetween(from,to);

       if(!cursor.moveToFirst()) {
           Cursor allTotals = db.getTagTotalsBetween(0,db.rightNow());
           if(!allTotals.moveToFirst()){
               noTagsCallback.onNoTags();
           }else{
               setAdapter(cursor, from, to);
           }

        }else{
            setAdapter(cursor, from, to);
            //Load tutorial items.
           if (!TutorialToast.make(getActivity(), TutorialToast.TUT_EDIT_TAG,
                   getResources().getString(R.string.tut_edit_tag), TutorialToast.EDIT_TAG_LENGTH)) {
               if (!TutorialToast.make(getActivity(), TutorialToast.TUT_VIEW_TASK,
                       getResources().getString(R.string.tut_view_tag), TutorialToast.VIEW_TAG_LENGTH)) {

               }
           }
        }
    }

    private void setAdapter(Cursor totals, long from, long to){
        //I don't remember exactly why this is here, but probably because the way the listeners work.
        fromHack = from;
        toHack = to;

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(getActivity(),
                layout,
                totals,
                UI_BINDING_FROM,
                UI_BINDING_TO, 0
        );
        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor c, int columnIndex) {
                if (columnIndex == 2) {
                    long durationSum = c.getLong(c.getColumnIndex(DBMap.TagTable.totalDuration));
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
                clickCallback.onTagClick(id,fromHack,toHack);
            }
        });
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mainCallback.onTagLongClick(id);

                return true;
            }
        });
    }


    /**********************/
    //Connection to mainactivity
    /**********************/

    public interface OnTagLongClickListener {
        public void onTagLongClick(long id);
    }

    public interface OnNoTags{
        public void onNoTags();
    }

    public interface OnTagClickListener{
        public void onTagClick(long id, long from, long to);
    }

    public void onTagRangeChange(long from, long to){
        displayTagTotals(from, to);
    }


}