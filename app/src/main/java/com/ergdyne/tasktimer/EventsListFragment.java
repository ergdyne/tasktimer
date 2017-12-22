/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ergdyne.tasktimer;


import android.app.Activity;
import android.app.ListFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;

import com.ergdyne.lib.DBMap;

public class EventsListFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor>
{
    //Paired with switchtaskfragment on main page
    /**********************/
    //Variable definitions
    /**********************/

    OnEventSelectedListener clickCallback;
    OnEventLongClicked longClickedCallback;

    //Loader and adapter items.
    private static final int LOADER_ID   = 1;
    private LoaderManager.LoaderCallbacks<Cursor> loaderCallbacks;
    private SimpleCursorAdapter mAdapter;
    private static final int layout = R.layout.row_duration;

    //Which columns are used in the view.
    private static final String[] projection = new String[]{
      DBMap.EventTable.table + "." +DBMap._ID + " AS " + DBMap._ID,
            DBMap.TaskTable.name,
            DBMap.EventTable.durationDisplay};

    private static final String[] UI_BINDING_FROM = new String[] {
            DBMap.TaskTable.name, DBMap.EventTable.durationDisplay};

    private static final int[] UI_BINDING_TO = new int[] {
            R.id.textView_row_duration_caption, R.id.textView_row_duration_duration};

    private Activity mainActivity;


    /**********************/
    //Fragment Lifecycle
    /**********************/
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            clickCallback = (OnEventSelectedListener) context;
            longClickedCallback = (OnEventLongClicked) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnEventSelectedListener or OnEventLongClicked");
        }

    }

    //Required as long as pre marshmallow is supported.
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            try {
                clickCallback = (OnEventSelectedListener) activity;
                longClickedCallback = (OnEventLongClicked) activity;
            } catch (ClassCastException e) {
                throw new ClassCastException(activity.toString()
                        + " must implement OnEventSelectedListener or OnEventLongClicked");
            }
        }}


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainActivity = getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();
        mAdapter = new SimpleCursorAdapter(mainActivity,layout, null, UI_BINDING_FROM, UI_BINDING_TO, 0);
        setListAdapter(mAdapter);
        loaderCallbacks = this;
        getLoaderManager().initLoader(LOADER_ID,null,loaderCallbacks);

        getListView().setClickable(true);
        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                clickCallback.onEventSelected(id);
            }
        });
        getListView().setLongClickable(true);
        getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                longClickedCallback.onEventLongClicked(id);
                return true;
            }
        });

    }


    /**********************/
    //connecting to mainActivity and other fragments
    /**********************/

    // The container Activity must implement this interface so the fragment can deliver messages.
    public interface OnEventSelectedListener {
        // Called by EventsListFragment when a list item is selected.
        public void onEventSelected(long id);
    }

    public interface OnEventLongClicked{
        public void onEventLongClicked(long id);
    }

    public void onTaskSwitch(){
        getLoaderManager().restartLoader(LOADER_ID,null,loaderCallbacks);
    }



    /**********************/
    //Loader related stuff
    /**********************/

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        //todo understand what is going on here more than I do
        return new CursorLoader(mainActivity, DBProvider.CONTENT_URI,projection,null,null,DBMap.EventTable.sortLatest);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()){
            case LOADER_ID:
                mAdapter.swapCursor(data);
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }


}