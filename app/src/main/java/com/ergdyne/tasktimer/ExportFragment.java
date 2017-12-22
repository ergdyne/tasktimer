package com.ergdyne.tasktimer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.ergdyne.lib.AppConstants;
import com.ergdyne.lib.DBMap;
import com.ergdyne.lib.EpicDate;
import com.ergdyne.lib.ErgAlert;
import com.ergdyne.lib.ErgFormats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by j on 3/23/17.
 */

//The export and import functionality are just bandaids to give a basic backup functionality.
    //It would be much nicer to have the SQLite database on the user's phone syncing with a server.
public class ExportFragment extends Fragment {

    /**********************/
    //variable definitions
    /**********************/

    private Button exportButton;
    private Button importButton;
    private Activity mainActivityConnection;
    public static String filename = "ERGDATA.txt";
    private DBHelper db;


    /**********************/
    //Fragment lifecycle
    /**********************/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_export, container,false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState){

        mainActivityConnection = getActivity();

        exportButton = (Button) view.findViewById(R.id.button_export);
        importButton = (Button) view.findViewById(R.id.button_import);

        checkWritePermission();

        {
            //todo move text to the string file since this export has moved from temporary to fixture.
            exportButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!isExternalStorageAvailable() || !isExternalStorageWritable()) {
                        ErgAlert.alert(mainActivityConnection, getResources().getString(R.string.err_export_blaim_usb));
                    }else{

                        if(exportData()){
                            ErgAlert.alert(mainActivityConnection,getResources().getString(R.string.warn_success));
                        }else{
                            ErgAlert.alert(mainActivityConnection,getResources().getString(R.string.warn_permission_write));}

                    }
                }
            });

            importButton.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    AlertDialog.Builder b = new AlertDialog.Builder(mainActivityConnection);
                    b.setTitle("Ummm!?");
                    b.setMessage(getResources().getString(R.string.warn_import_crazy));
                    b.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(importData()){
                                ErgAlert.alert(mainActivityConnection,getResources().getString(R.string.warn_success));
                            }else{
                                ErgAlert.alert(mainActivityConnection,getResources().getString(R.string.err_import_failure));}

                        }
                    });
                    b.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    });
                    b.setIcon(android.R.drawable.ic_dialog_alert);
                    b.show();



                }
            });
        }

    }

    /**********************/
    //functions used in lifecycle
    /**********************/
    private void checkWritePermission(){
        //Check for permissions and get if not available.
        if(ContextCompat.checkSelfPermission(mainActivityConnection,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(mainActivityConnection,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    AppConstants.PERMISSION_WRITE_EXTERNAL
            );
        }
    }
    //These could probably be moved somewhere else.
    private static boolean isExternalStorageWritable(){
        String storageState = Environment.getExternalStorageState();
        return !Environment.MEDIA_MOUNTED_READ_ONLY.equals(storageState);
    }
    private static boolean isExternalStorageAvailable(){
        String storageState = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(storageState);
    }


    /**********************/
    //export handling
    /**********************/
    private boolean exportData() {

        //TODO at least convert this to CSV
        boolean works;
        int rowCount; //Always at least one because of headings!
        String s = "\t"; //Column separator
        String n = "\n"; //Row separator
        String seconds = "(seconds since epoch)";

        db = new DBHelper(mainActivityConnection);
        Cursor events = db.getAllEvents();

        rowCount = events.getCount() + 1; //Add 1 for the header.
        List<String> rows = new ArrayList<>(rowCount);

        //Add column headers.
        rows.add(DBMap.TaskTable.name + s + DBMap.EventTable.start + s + DBMap.EventTable.end + s + DBMap.EventTable.start + seconds + s + DBMap.EventTable.end + seconds +  n);

        //Load up the Array with data.
        while(events.moveToNext()){

            String taskName = events.getString(events.getColumnIndex(DBMap.TaskTable.name));
            long start = events.getLong(events.getColumnIndex(DBMap.EventTable.start));
            long end = events.getLong(events.getColumnIndex(DBMap.EventTable.end));

            rows.add(taskName + s + EpicDate.toDateTimeString(start) + s + EpicDate.toDateTimeString(end) + s + start + s + end + n);
        }
        File file;
        FileOutputStream outputStream;

        //Try to write the data.
        try{
            file = new File(Environment.getExternalStorageDirectory(),filename);
            outputStream = new FileOutputStream(file);


            for(int i=0;i<rowCount;i++) {
                outputStream.write(rows.get(i).getBytes());
            }

            outputStream.close();
            works = true;

        }catch(IOException e){
            e.printStackTrace();
            works=false;
        }

        return works;
    }


    //TODO this would be nicer if it didn't delete everything,
    // but I was in a hurry to get my copy of the app back to running after the need suddenly appeared
    // when my Nexus 5x power button went bad.
    private boolean importData() {

        boolean works;

        File file;
        BufferedReader bufferedReader;

        db = new DBHelper(mainActivityConnection);

        try{
            file = new File(Environment.getExternalStorageDirectory(),filename);
            bufferedReader = new BufferedReader(new FileReader(file));

            //Drop and Rebuild the database.
            db.resetDB();

            String line;

            while((line = bufferedReader.readLine())!=null){

                String[] columns = line.split("\t");

                if(columns.length == 5){
                    if(isLong(columns[3]) && isLong(columns[4])){
                        String name = columns[0];
                        EpicDate start = new EpicDate(Long.parseLong(columns[3]));
                        EpicDate end = new EpicDate(Long.parseLong(columns[4]));

                        insertImport(name, start, end);

                    }else{
                    }

                }else{
                }

            }

            bufferedReader.close();

            works = true;

        }catch(IOException e){
            e.printStackTrace();
            works=false;
        }

        //Find the latest event and use it's end time as start for settings.
        //Based on the way export is done, the latest event should be the first event.
        //So it should be id 1.
        //It would be more stable to actually look for the latest event.
        Cursor firstEvent = db.getByID(DBMap.EventTable.table,1);
        while(firstEvent.moveToNext()){
            db.updateSettings(firstEvent.getLong(firstEvent.getColumnIndex(DBMap.EventTable.end)), DBMap.TaskTable.defaultID);
        }

        //Return value is to inform dialogs.
        return works;
    }

    private void insertImport(String name, EpicDate start, EpicDate end){
        if(start.sEpoch < end.sEpoch && end.sEpoch < db.rightNow()){

            long taskID = db.findOrInsertTask(name);
            long duration = end.sEpoch - start.sEpoch;

            String durationDisplay = ErgFormats.durationHMS(duration);

            db.insertEvent(start.sEpoch, end.sEpoch, taskID, duration, durationDisplay);
        }
    }

//TODO move this to a better location. Like in a library.
    public boolean isLong(String input){
        try{
            Long.parseLong(input);
            return true;
        }catch(Exception e){
            return false;
        }
    }






}
