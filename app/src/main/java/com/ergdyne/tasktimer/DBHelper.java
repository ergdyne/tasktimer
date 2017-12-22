package com.ergdyne.tasktimer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.os.SystemClock;

import com.ergdyne.lib.DBMap;
import com.ergdyne.lib.ErgFormats;
import com.ergdyne.lib.ErgSQLiteHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by j on 4/5/17.
 */

public class DBHelper extends ErgSQLiteHelper {
    public DBHelper(Context context){
        super(context, DBMap.name,null,DBMap.version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(DBMap.SettingsTable.createTable);
        db.execSQL(DBMap.EventTable.createTable);
        db.execSQL(DBMap.TaskTable.createTable);
        db.execSQL(DBMap.TagTable.createTable);
        db.execSQL(DBMap.TaskTagTable.createTable);
        db.execSQL(DBMap.TaskReminderTable.createTable);

        setDefaultValues(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        if(oldVersion == 1) {
            db.execSQL(DBMap.DELETE_TAG_TAG_TABLE);
            db.execSQL(DBMap.TaskReminderTable.createTable);
        }
    }

    private void setDefaultValues(SQLiteDatabase db){

        ContentValues defaultTask = new ContentValues();
        defaultTask.put(DBMap.TaskTable.name,DBMap.TaskTable.defaultTask);
        defaultTask.put(DBMap.TaskTable.active,DBMap.TaskTable.defaultActivity);
        defaultTask.put(DBMap.TaskTable.priority,DBMap.TaskTable.defaultPriority);

        ContentValues defaultSettings = new ContentValues();
        defaultSettings.put(DBMap._ID,DBMap.SettingsTable.defaultID);
        defaultSettings.put(DBMap.SettingsTable.taskID,
                db.insert(DBMap.TaskTable.table, null, defaultTask));
        defaultSettings.put(DBMap.SettingsTable.start,rightNow());

        db.replace(DBMap.SettingsTable.table,null,defaultSettings);
    }

    public void resetDB() {
        SQLiteDatabase db = this.getReadableDatabase();

        db.execSQL(DBMap.SettingsTable.deleteTable);
        db.execSQL(DBMap.EventTable.deleteTable);
        db.execSQL(DBMap.TaskTable.deleteTable);
        db.execSQL(DBMap.TagTable.deleteTable);
        db.execSQL(DBMap.TaskTagTable.deleteTable);
        db.execSQL(DBMap.TaskReminderTable.deleteTable);

        db.execSQL(DBMap.SettingsTable.createTable);
        db.execSQL(DBMap.EventTable.createTable);
        db.execSQL(DBMap.TaskTable.createTable);
        db.execSQL(DBMap.TagTable.createTable);
        db.execSQL(DBMap.TaskTagTable.createTable);
        db.execSQL(DBMap.TaskReminderTable.createTable);

        setDefaultValues(db);

    }

    /**********************/
    //Time management, This time stuff because it is only here because of my early confusion about SQLite and laziness since.
    /**********************/

    //Returns seconds.
    long rightNow(){
        long s = new Date().getTime()/1000; //in s

        return s;
    }

    //Used for the chronometer, returns milliseconds
    long currentBaseTime(){
        long duration = ( rightNow() - getCurrentStart())*1000;
        long start = SystemClock.elapsedRealtime() - duration;

        return start;
    }


    /**********************/
    //Get Current Items
    /**********************/
    long getCurrentStart(){
        return getLongByID(DBMap.SettingsTable.table,DBMap.SettingsTable.defaultID,DBMap.SettingsTable.start);
    }
    String getCurrentTaskName(){
        return getTaskName(getCurrentTaskID());
    }

    long getCurrentTaskID(){
       long id = getLongByID(DBMap.SettingsTable.table,DBMap.SettingsTable.defaultID,DBMap.SettingsTable.taskID);

        //This is a fix just for my copy of the App which is glitched and sans a default task!
        if(id == DBMap.TagTable.defaultID){
            id = findOrInsertTask(DBMap.TaskTable.defaultTask);
            updateSettings(getCurrentStart(),id);
        }
        return id;
    }

    Cursor getSettings(){
        return getByID(DBMap.SettingsTable.table,DBMap.SettingsTable.defaultID);
    }

    /**********************/
    //Get Task items
    /**********************/

    String getTaskName(long id){
        return getStringByID(DBMap.TaskTable.table,id,DBMap.TaskTable.name);
    }

    long getTaskID(String n){
        Cursor task = getByTextField(DBMap.TaskTable.table, DBMap.TaskTable.name, n);

        if(task.moveToFirst()){
            long x = task.getLong(task.getColumnIndex(DBMap._ID));
            task.close();
            return x;
        }else{
            task.close();
            return 0;
        }
    }

    //Populate Task suggestions.
    List<String> getTaskList(){
        Cursor allData = getAllData(DBMap.TaskTable.table);
        List<String> l = new ArrayList<>();
        while(allData.moveToNext()){
            l.add(allData.getString(allData.getColumnIndex(DBMap.TaskTable.name)));
        }
        allData.close();
        return l;
    }

    long getTaskTotal(long id, long from, long to){
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

        builder.setTables(DBMap.EventTable.table);

        String[] projection = new String[]{
                "SUM(" + DBMap.EventTable.duration + ")"
        };

        String selection = DBMap.EventTable.taskID + "=? AND " + DBMap.EventTable.end + " BETWEEN " + from + " AND " +  to;

        String[] args = new String[]{String.valueOf(id)};

        Cursor cursor = getByBuilder(builder,projection,selection,args,null,null,null);

        if(cursor.moveToFirst()){
            long total = cursor.getLong(0);
            cursor.close();
            return total;
        }else{
            return 0;
        }
    }

    /**********************/
    //Get Tag items
    /**********************/

    String getTagName(long id){
        return getStringByID(DBMap.TagTable.table,id,DBMap.TagTable.name);
    }

    long getTagID(String n){
        Cursor tag = getByTextField(DBMap.TagTable.table, DBMap.TagTable.name,n);
        if(tag.moveToFirst()){
            long x = tag.getLong(tag.getColumnIndex(DBMap._ID));
            tag.close();
            return x;
        }else{
            tag.close();
            return 0;
        }
    }

    long getTagTotal(long id, long from, long to){
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DBMap.EventTable.table + " INNER JOIN " + DBMap.TaskTagTable.table +
                " ON (" + DBMap.EventTable.table + "." + DBMap.EventTable.taskID + " = " +
                DBMap.TaskTagTable.table + "." + DBMap.EventTable.taskID + ")" +
                " INNER JOIN " + DBMap.TagTable.table +
                " ON (" + DBMap.TaskTagTable.table +"." + DBMap.TaskTagTable.tagID + " = " +
                DBMap.TagTable.table + "." + DBMap._ID + ")"

        );
        String[] projection = new String[]{"SUM(" + DBMap.EventTable.duration +")" };

        String selection = DBMap.TagTable.table + "." + DBMap._ID + "=? AND " +
                DBMap.EventTable.end + " BETWEEN " + from + " AND " +  to;

        String[] args = new String[]{String.valueOf(id)};


        Cursor cursor = getByBuilder(builder, projection,selection,args,null,null,null);
        if(cursor.moveToFirst()){
            return cursor.getLong(0);
        }else{
            return 0;
        }
    }
    //For populating Tag suggestions.
    List<String> getTagList(){
        Cursor allData = getAllData(DBMap.TagTable.table);
        List<String> l = new ArrayList<>();
        while(allData.moveToNext()){
            l.add(allData.getString(allData.getColumnIndex(DBMap.TagTable.name)));
        }
        allData.close();
        return l;
    }

    /**********************/
    //Get Reminder Items
    /**********************/

    long getReminderLength(long taskID){
        Cursor c = getByLongField(DBMap.TaskReminderTable.table,
                DBMap.TaskReminderTable.taskID, taskID);
        if(c.moveToFirst()){

            return c.getLong(c.getColumnIndex(DBMap.TaskReminderTable.length));
        }
        return 0;
    }

    /**********************/
    //Get Event items
    /**********************/

    List<Long> getImpactedEvents(String column,long x, long y){
        List<Long> l = new ArrayList<>();
        String[] columns = {DBMap._ID};
        Cursor cursor = getInRange(DBMap.EventTable.table, columns, column, x, y);

        while(cursor.moveToNext()){
            l.add(cursor.getLong(cursor.getColumnIndex(DBMap._ID)));
        }
        return l;
    }

    long getLogStart(){
        SQLiteDatabase db = this.getReadableDatabase();
        long start;

        String[] columns = {"MIN("+DBMap.EventTable.start+")"};
        Cursor cursor = db.query(DBMap.EventTable.table,columns,null,null,null,null,null);

        if(cursor.moveToFirst()){
            start = cursor.getLong(0);
        }else {
            start = getCurrentStart();
        }
        cursor.close();
        return start;
    }

    String getEventTaskName(long id){
        Cursor cursor = getByID(DBMap.EventTable.table,id);
        cursor.moveToFirst();
        long taskID = cursor.getLong(cursor.getColumnIndex(DBMap.EventTable.taskID));
        cursor.close();

        return getTaskName(taskID);
    }

    /**********************/
    //Get Cursor for listView
    /**********************/

    Cursor getTaskTotalsBetween(long from, long to){
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DBMap.EventTable.table + " INNER JOIN " + DBMap.TaskTable.table +
                " ON (" + DBMap.EventTable.taskID + " = "
                + DBMap.TaskTable.table + "." + DBMap._ID + ")"
        );

        String[] projection = new String[]{
                DBMap.TaskTable.table + "." +DBMap._ID + " AS " + DBMap._ID,
                DBMap.TaskTable.name,
                "SUM(" + DBMap.EventTable.duration +") AS " + DBMap.TaskTable.totalDuration
        };

        String selection = DBMap.EventTable.end + " BETWEEN " + from + " AND " +  to;

        String groupBy = DBMap.TaskTable.name;

        String sortOrder = DBMap.TaskTable.totalDuration + " DESC";

        return getByBuilder(builder, projection,selection,null,groupBy,null,sortOrder);
    }

    Cursor getTagTotalsBetween(long from, long to){
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DBMap.EventTable.table + " INNER JOIN " + DBMap.TaskTagTable.table +
                " ON (" + DBMap.EventTable.table + "." + DBMap.EventTable.taskID + " = " +
                DBMap.TaskTagTable.table + "." + DBMap.EventTable.taskID + ")" +
                " INNER JOIN " + DBMap.TagTable.table +
                " ON (" + DBMap.TaskTagTable.table +"." + DBMap.TaskTagTable.tagID + " = " +
                DBMap.TagTable.table + "." + DBMap._ID + ")"

        );
        String[] projection = new String[]{
                DBMap.TagTable.table + "." +DBMap._ID + " AS " + DBMap._ID,
                DBMap.TagTable.table + "." + DBMap.TagTable.name + " AS " + DBMap.TagTable.name,
                "SUM(" + DBMap.EventTable.duration +") AS " + DBMap.TagTable.totalDuration
        };

        String selection = DBMap.EventTable.end + " BETWEEN " + from + " AND " +  to;
        String groupBy = DBMap.TagTable.name;
        String sortOrder = DBMap.TagTable.totalDuration + " DESC";

        return getByBuilder(builder, projection,selection,null,groupBy,null,sortOrder);
    }

    //Not to be confused with getTagTaskNames.
    Cursor getTaskTagNames(long taskID){
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DBMap.TaskTagTable.table + " INNER JOIN " + DBMap.TagTable.table +
                " ON (" + DBMap.TaskTagTable.tagID + " = "
                + DBMap.TagTable.table + "." + DBMap._ID + ")"
        );

        String[] projection = new String[]{
                DBMap.TaskTagTable.table + "." + DBMap._ID + " AS " + DBMap._ID,
                DBMap.TagTable.name
        };

        String selection = DBMap.TaskTagTable.taskID + " =?";
        String[] args = {String.valueOf(taskID)};

        String sortOrder = DBMap.TaskTagTable.table + "." + DBMap._ID + " ASC";

        return getByBuilder(builder, projection,selection,args,null,null,sortOrder);
    }

    //Not to be confused with getTaskTagNames.
    Cursor getTagTaskNames(long tagID){
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DBMap.TaskTagTable.table + " INNER JOIN " + DBMap.TaskTable.table +
                " ON (" + DBMap.TaskTagTable.taskID + " = "
                + DBMap.TaskTable.table + "." + DBMap._ID + ")"
        );

        String[] projection = new String[]{
                DBMap.TaskTagTable.table + "." + DBMap._ID + " AS " + DBMap._ID,
                DBMap.TaskTable.name
        };

        String selection = DBMap.TaskTagTable.tagID + " =?";
        String[] args = {String.valueOf(tagID)};

        String sortOrder = DBMap.TaskTagTable.table + "." + DBMap._ID + " ASC";

        return getByBuilder(builder, projection,selection,args,null,null,sortOrder);
    }

    Cursor getAllEvents(){
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(
                DBMap.EventTable.table + " INNER JOIN " + DBMap.TaskTable.table +
                        " ON (" + DBMap.EventTable.taskID
                        + " = " + DBMap.TaskTable.table + "." +DBMap._ID + ")");

        return getByBuilder(builder,null,null,null,null,null,DBMap.EventTable.start + " DESC");
    }

    Cursor getTasksEvents(long id, long from, long to){
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DBMap.EventTable.table);

        String[] projection = new String[]{DBMap._ID,DBMap.EventTable.end,DBMap.EventTable.durationDisplay};

        String selection = DBMap.EventTable.taskID + "=? AND " + DBMap.EventTable.end + " BETWEEN " + from + " AND " +  to;

        String[] args = new String[]{String.valueOf(id)};

        String orderBy = DBMap.EventTable.end + " DESC";

        return getByBuilder(builder,projection,selection,args, null, null, orderBy);

    }

    Cursor getTagTasks(long id, long from, long to){
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(DBMap.EventTable.table + " INNER JOIN " + DBMap.TaskTagTable.table +
                " ON (" + DBMap.EventTable.table + "." + DBMap.EventTable.taskID + " = " +
                DBMap.TaskTagTable.table + "." + DBMap.EventTable.taskID + ")" +
                " INNER JOIN " + DBMap.TaskTable.table +
                " ON (" + DBMap.TaskTagTable.table +"." + DBMap.TaskTagTable.taskID + " = " +
                DBMap.TaskTable.table + "." + DBMap._ID + ")"
        );

        String[] projection = new String[]{
                DBMap.TaskTable.table + "." +DBMap._ID + " AS " + DBMap._ID,
                DBMap.TaskTable.table + "." + DBMap.TaskTable.name + " AS " + DBMap.TaskTable.name,
                "SUM(" + DBMap.EventTable.duration +") AS " + DBMap.TaskTable.totalDuration
        };

        String selection = DBMap.TaskTagTable.tagID + "=? AND " + DBMap.EventTable.end + " BETWEEN " + from + " AND " +  to;
        String[] args = new String[]{String.valueOf(id)};
        String groupBy = DBMap.TaskTable.name;
        String sortOrder = DBMap.TaskTable.totalDuration + " DESC";

        return getByBuilder(builder, projection,selection,args,groupBy,null,sortOrder);

    }

    /**********************/
    //Specific Updates
    /**********************/
    long updateSettings (long start, long taskID){

        ContentValues values = new ContentValues();
        values.put(DBMap._ID,DBMap.SettingsTable.defaultID);
        values.put(DBMap.SettingsTable.taskID,taskID);
        values.put(DBMap.SettingsTable.start,start);

        return replace(DBMap.SettingsTable.table, values);
    }

    long updateEvent(long id, long start, long end, long taskID, long duration, String durationDisplay){
        ContentValues values = new ContentValues();

        values.put(DBMap._ID,id);
        values.put(DBMap.EventTable.start,start);
        values.put(DBMap.EventTable.end,end);
        values.put(DBMap.EventTable.taskID,taskID);
        values.put(DBMap.EventTable.duration,duration);
        values.put(DBMap.EventTable.durationDisplay,durationDisplay);

        return replace(DBMap.EventTable.table, values);
    }

    long updateTaskIDs(long oldID, long newID){
        String selection = DBMap.EventTable.taskID + "=?";
        String[] args = {String.valueOf(oldID)};

        ContentValues values = new ContentValues();
        values.put(DBMap.EventTable.taskID,newID);

        return update(DBMap.EventTable.table,values,selection,args);
    }

    long updateTaskName(long id,String name){

        ContentValues values = new ContentValues();
        values.put(DBMap.TaskTable.name,name);

        String selection = DBMap._ID + "=?";
        String[] args = {String.valueOf(id)};

        return update(DBMap.TaskTable.table,values,selection,args);
    }

    long updateTagName(long id, String name){
        ContentValues values = new ContentValues();
        values.put(DBMap.TagTable.name, name);

        String selection = DBMap._ID + "=?";
        String[] args = {String.valueOf(id)};

        return update(DBMap.TagTable.table,values,selection,args);
    }

    /**********************/
    //Specific inserts
    /**********************/
    long insertTask(String name, int activity, int priority){


        ContentValues values = new ContentValues();
        values.put(DBMap.TaskTable.name,name);
        values.put(DBMap.TaskTable.active,activity);
        values.put(DBMap.TaskTable.priority,priority);

        return insert(DBMap.TaskTable.table,values);
    }

    //TODO get rid of durationDisplay.
    long insertEvent(long start, long end, long taskID, long duration, String durationDisplay){
        ContentValues values = new ContentValues();

        values.put(DBMap.EventTable.start,start);
        values.put(DBMap.EventTable.end,end);
        values.put(DBMap.EventTable.taskID,taskID);
        values.put(DBMap.EventTable.duration,duration);
        values.put(DBMap.EventTable.durationDisplay,durationDisplay);

        return insert(DBMap.EventTable.table,values);
    }

    long insertTag(String name, int activity, int prime){
        ContentValues values = new ContentValues();

        values.put(DBMap.TagTable.name,name);
        values.put(DBMap.TagTable.active,activity);
        values.put(DBMap.TagTable.prime,prime);

        return insert(DBMap.TagTable.table,values);
    }

    long insertTaskTag(long taskID, long tagID){
        ContentValues values = new ContentValues();

        values.put(DBMap.TaskTagTable.taskID,taskID);
        values.put(DBMap.TaskTagTable.tagID,tagID);

        return insert(DBMap.TaskTagTable.table,values);
    }

    long insertTaskReminder(long taskID, long length){
        ContentValues values = new ContentValues();

        values.put(DBMap.TaskReminderTable.taskID,taskID);
        values.put(DBMap.TaskReminderTable.length,length);

        return insert(DBMap.TaskReminderTable.table,values);
    }

    /**********************/
    //Deletes... ooo dangerous!
    //And I did kind of go about it in the wrong way.
    /**********************/

    List<Integer> deleteByID(String table,List<Long> ids){
        List<Integer> results=new ArrayList<>();

        for(int i=0;i<ids.size();i++){
            results.add(deleteByID(table, ids.get(i)));
        }
        return results;
    }

    int deleteByID(String table, long id){
        String selection = DBMap._ID + "=?";
        String[] args = {String.valueOf(id)};
        return delete(table,selection,args);
    }

    int deleteTaskRelations(long id){
        String selection = DBMap.TaskTagTable.taskID + "=?";
        String[] args = {String.valueOf(id)};
        return delete(DBMap.TaskTagTable.table,selection,args);

    }

    int deleteTagRelations(long id){
        String selection = DBMap.TaskTagTable.tagID + "=?";
        String[] args = {String.valueOf(id)};
        return delete(DBMap.TaskTagTable.table,selection,args);
    }

    int deleteTaskReminder(long taskID){
        String selection = DBMap.TaskReminderTable.taskID + "=?";
        String[] args = {String.valueOf(taskID)};
        return delete(DBMap.TaskReminderTable.table,selection,args);
    }


    /**********************/
    //Very specific Action/Queries. Can likely be in a different class.
    /**********************/


    //Return the end to use in opening the next event.
    long closeEvent(){
        //Get current event information.
        Cursor settings = getSettings();
        settings.moveToFirst();
        long start = settings.getLong(settings.getColumnIndex(DBMap.SettingsTable.start));
        long end = rightNow();
        long taskID = settings.getLong(settings.getColumnIndex(DBMap.SettingsTable.taskID));
        settings.close();

        long duration = end - start;

        if(duration>0) {
            String durationDisplay = ErgFormats.durationHMS(duration);
            insertEvent(start, end, taskID, duration, durationDisplay);
        }
        return end;
    }


    long findOrInsertTask(String n){

        //Nothing entered, so return the default.
        if(n.length() == 0){
            return DBMap.TaskTable.defaultID;
        }

        long id = getTaskID(n);

        if(id!=0){
            //Task Exists.
            return id;
        }else{
            //Create a new task.
            return insertTask(n, 1,1);
        }

    }

    long findOrInsertTaskTag(String n, long taskID){
        if(n.length() == 0){
            return 0;
        }

        long tagID = getTagID(n);
        if(tagID!=0){
            //Determine if relationship exists already.
            if(!isTaskTagRelated(taskID,tagID)){
                return insertTaskTag(taskID,tagID);
            }else{
                return 0;
            }

        }else{
            //Nothing exists, so make a relationship.
            return insertTaskTag(taskID,insertTag(n,1,1));
        }

    }

    boolean isTaskTagRelated(long taskID,long tagID){
        String selection = DBMap.TaskTagTable.taskID + "=? AND " +
                DBMap.TaskTagTable.tagID + "=?";
        String[] args = {String.valueOf(taskID),String.valueOf(tagID)};

        return hasResult(DBMap.TaskTagTable.table,selection,args);
    }

}
