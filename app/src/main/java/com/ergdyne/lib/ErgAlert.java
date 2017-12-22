package com.ergdyne.lib;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import java.util.List;

/**
 * Created by j on 3/14/17.
 */


//For simple pre-configured alerts.
public class ErgAlert {
    public ErgAlert(){}

    public static void alert(Context context, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message);
        builder.setCancelable(true);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
            }
        });

        AlertDialog a = builder.create();
        a.show();
    }

    public static void alert(Context context, List<String> messages){
        //For cases with multiple messages that should be broken up by lines.
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        String message="";

        //I am missing some Scala functionality :(
        for(int i=0;i<messages.size();i++){
            message = message + messages.get(i)+ ((i+1<messages.size())?"\n":"");
        }

        builder.setMessage(message);
        builder.setCancelable(true);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog a = builder.create();
        a.show();
    }


}
