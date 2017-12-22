package com.ergdyne.tasktimer;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.ergdyne.lib.DBMap;

/**
 * Created by j on 3/18/17.
 */

//I actually have very little idea what is going on with the Provider, but this is supposedly what makes the infinite scrolling work!

public class DBProvider extends ContentProvider{
    private DBHelper dbHelper;
    protected SQLiteDatabase db;

    //TODO centralize these settings type details.
    public  static final String AUTHORITY = "com.ergdyne.tasktimer.DBProvider";

    //DOESN'T HAVE TO BE TABLE_NAME
    public static final String TABLE_NAME = DBMap.EventTable.table;
    static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + TABLE_NAME);

    @Override
    public boolean onCreate() {
        open();
        return true;
    }

    private void open() throws SQLException{
        dbHelper = new DBHelper(getContext());
        db = dbHelper.getWritableDatabase(); //I don't think it has to be writable...
    }
    private void close(){dbHelper.close();}



    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {

        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
                builder.setTables(
                        DBMap.EventTable.table + " INNER JOIN " + DBMap.TaskTable.table +
                        " ON (" + DBMap.EventTable.taskID
                        + " = " + DBMap.TaskTable.table + "." +DBMap._ID + ")");

        return builder.query(db,projection,selection,selectionArgs,null,null,sortOrder);
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {

        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {

        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        return 0;
    }
}
