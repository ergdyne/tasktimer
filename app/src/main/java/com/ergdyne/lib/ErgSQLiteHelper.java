package com.ergdyne.lib;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;

/**
 * Created by j on 3/6/17.
 *
 * Some more general queries. Really, this is just an exercise in inheritance.
 * Most of these queries are better built using other mechanisms.
 */



public abstract class ErgSQLiteHelper extends SQLiteOpenHelper{

    public ErgSQLiteHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version ) {
        super(context, name, factory, version);

    }

    //The ID field used by this helper is always _id.
    public static final String _ID = "_id";

    /**********************/
    //General Get Cursors
    /**********************/
    protected Cursor getAllData(String table){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.query(table,null,null,null,null,null,null,null);
    }

    public Cursor getByTextField(String table, String column, String name){
        SQLiteDatabase db = this.getReadableDatabase();
        String[] args = {name};
        return db.query(table,null, column + "=?" , args,null,null,null);
    }

    //Routes through TextField because it seems like numbers have to be looked up as "String.valueOf"
    public Cursor getByLongField(String table, String column, long x){
        return getByTextField(table,column,String.valueOf(x));
    }

    public Cursor getByID(String table, long id){
        return getByLongField(table, _ID, id);
    }

    public Cursor getInRange(String table, String[] projection, String column, long from, long to){
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = column + " BETWEEN " + from + " AND " + to;

        return db.query(table, projection, selection, null, null,null,null);
    }

    public Cursor getByBuilder(SQLiteQueryBuilder builder, String[] projection,
                               String selection, String[] args, String groupBy, String having,
                               String sortOrder){
        SQLiteDatabase db = this.getReadableDatabase();

        return builder.query(db,projection,selection,args,groupBy,having, sortOrder);
    }

    /**********************/
    //General Get values
    /**********************/

    public String getStringByID(String table, long id, String column){
        Cursor item = getByID(table,id);
        item.moveToFirst();
        String s = item.getString(item.getColumnIndex(column));
        item.close();
        return s;
    }
    public long getLongByID(String table, long id, String column){
        Cursor item = getByID(table,id);
        item.moveToFirst();
        long x = item.getLong(item.getColumnIndex(column));
        item.close();
        return x;
    }

    public boolean hasResult(String table, String selection, String[]args){
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(table,null,selection,args,null,null,null,String.valueOf(1));
        boolean b = (c.moveToFirst());
        c.close();
        return b;
    }


    /**********************/
    //Simple insert, update, replace, and delete
    /**********************/
    public long insert(String table, ContentValues values){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.insert(table,null,values);
    }

    public long update(String table, ContentValues values, String selection, String[] args){
        SQLiteDatabase db = this.getWritableDatabase();

        return db.update(table, values, selection,args);
    }

    public long replace(String table, ContentValues values){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.replace(table, null, values);
    }

    public int delete(String table, String selection, String[]args){
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(table,selection,args);
    }
}
