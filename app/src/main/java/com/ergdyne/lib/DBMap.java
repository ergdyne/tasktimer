package com.ergdyne.lib;

import android.provider.BaseColumns;

/**
 * Created by j on 3/10/17.
 */

public final class DBMap {
    //Short cuts for building SQL commands and such.
    public static final int version = 2; //don't upgrade unless we really have to! need to have export/import to handle it.
    public static final String name = "com.ergdyne.tasktimer.db";
    public static final String _ID = "_id";
    private static final String _HEXID = "hexID"; //This was meant to be used for implementing a central database and can be ignore. (Should be removed.)
    private static final String DESC = " DESC";
    private static final String provider = "com.ergdyne.tasktimer.DBProvider"; //Not used, but here for information.

    private static final String CREATE = "CREATE TABLE ";
    private static final String DELETE = "DROP TABLE IF EXISTS ";
    private static final String OPEN = " (";
    private static final String CLOSE = ")";
    private static final String STRING = " TEXT";
    private static final String ID = " INT"; //but is a long
    private static final String HEX_ID = " TEXT"; //Is probably not the right data type unless the central DB is NoSQL.
    private static final String INT = " INT";
    private static final String BOOLEAN = " INT"; //0 or 1
    private static final String DATE = " INT"; //SECONDS SINCE epoch as a long
    private static final String COMMA =", ";
    private static final String PRIMARY_ID = _ID + " INTEGER PRIMARY KEY AUTOINCREMENT"; //says integer, but really is a long


    //Empty constructor to meet requirements.
    private DBMap(){}

    /**********************/
    //Settings items, which is really the current item. Would be better as key/value pairs.
    /**********************/

    public static abstract class SettingsTable implements BaseColumns{
        public static final String table = "settings";

        public static final String start = "start"; //long, date
        public static final String taskID = "taskID"; //long

        //The settings table needs an item to start with. This was the sort of an error at one point because the default task was allowed to be deleted!
        public static final long defaultID = 1;

        public static final String createTable = CREATE + table +
                OPEN +      PRIMARY_ID +    COMMA + //0
                start +     DATE +          COMMA + //1
                taskID +    ID +            CLOSE; //2

        public static final String deleteTable = DELETE + table;


    }

    /**********************/
    //Event is the basic timing mechanism. This is the data the user is collecting.
    /**********************/
    public static abstract class EventTable implements BaseColumns{
        public static final String table = "events";

        public static final String start = "start"; //long, date
        public static final String end = "end"; //long, date
        public static final String taskID = "taskID"; //long
        public static final String duration = "duration"; //long seconds
        public static final String durationDisplay = "durationDisplay"; //string

        public static final String createTable = CREATE + table +
                OPEN +              PRIMARY_ID +    COMMA +
                start +             DATE +          COMMA +
                end +               DATE +          COMMA +
                taskID +            ID +            COMMA +
                duration +          INT +           COMMA +
                _HEXID +            HEX_ID +        COMMA +
                durationDisplay +   STRING +        CLOSE;

        public static final String deleteTable = DELETE + table;

        public static final String sortLatest = end + DESC;
    }

    /**********************/
    //Task are labels for events. It is the primary grouping mechanism for events.
    /**********************/
    public static abstract class TaskTable implements BaseColumns {
        public static final String table = "tasks";

        public static final String name = "name"; //text, string
        public static final String priority = "priority"; //int
        public static final String active = "active"; //int, boolean

        public static final String defaultTask = "Untracked";
        public static final long defaultID = 1;
        public static final Integer defaultPriority = 1;
        public static final Integer defaultActivity = 1;
        //This string is an example of something that you don't really need to store in the DB. This item can be easily generated on the fly.
        public static final String totalDuration = "total_duration";

        public static final String createTable = CREATE + table +
                OPEN +      PRIMARY_ID +    COMMA +
                name +      STRING +        COMMA +
                priority +  INT +           COMMA +
                _HEXID +    HEX_ID +        COMMA +
                active  +   BOOLEAN +       CLOSE;

        public static final String deleteTable = DELETE + table;

    }

    /**********************/
    //Tags are a secondary grouping mechanism that can be used to group or classify tasks.
    /**********************/
    public static abstract class TagTable implements BaseColumns {
        public static final String table = "tags";

        public static final String name = "name"; //text, string
        public static final String prime = "prime"; //int, boolean
        public static final String active = "active"; //int, boolean
        public static final String totalDuration = "total_duration";

        public static final long defaultID = 1;

        public static final String createTable = CREATE + table +
                OPEN +      PRIMARY_ID +    COMMA +
                name +      STRING +        COMMA +
                prime +     BOOLEAN +       COMMA +
                _HEXID +    HEX_ID +        COMMA +
                active  +   BOOLEAN +       CLOSE;

        public static final String deleteTable = DELETE + table;
    }

    /**********************/
    //Task to Tag relationships to allow for Tasks to have multiple Tags.
    /**********************/
    public static abstract class TaskTagTable implements BaseColumns {
        public static final String table = "taskTag";

        public static final String taskID = "taskID"; //long
        public static final String tagID = "tagID"; //long

        public static final String createTable = CREATE + table +
                OPEN +      PRIMARY_ID +    COMMA +
                taskID +    ID +            COMMA +
                _HEXID +    HEX_ID +        COMMA +
                tagID +     ID +            CLOSE;

        public static final String deleteTable = DELETE + table;
    }

    /**********************/
    //Task Reminders which are configurations for setting alarms.
    //Up to one per task in current configuration, so it could be a column in the Task table.
    //For multiple reminders, this table would be required.
    /**********************/

    public static abstract class TaskReminderTable implements BaseColumns {
        public static final String table = "taskReminder";

        public static final String taskID = "taskID";
        public static final String length = "length";

        public static final String createTable = CREATE + table +
                OPEN +      PRIMARY_ID +    COMMA +
                taskID +    ID +            COMMA +
                _HEXID +    HEX_ID +        COMMA +
                length +    INT +           CLOSE; //long seconds

        public static final String deleteTable = DELETE + table;
    }

    /**********************/
    //Create database upgrade strings.
    /**********************/

    //Version 1 to 2
        //Delete TagTagTable (and class removed)
        public static final String DELETE_TAG_TAG_TABLE = DELETE + "tagTag";
        //Add TaskReminderTable

}
