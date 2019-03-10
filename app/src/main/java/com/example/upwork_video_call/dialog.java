package com.example.upwork_video_call;

import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.app.AlertDialog;
import  android.widget.EditText;
import android.widget.Toast;

import static android.content.Context.MODE_PRIVATE;

public class dialog {
    final int SET_UP = 0;

    public void showDialog(final Context mContext)
    {
        final  Context context = mContext;
        LayoutInflater li = LayoutInflater.from(context);
        View promptsView = li.inflate(R.layout.prompt, null);
        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptsView);

        final EditText userInput = (EditText) promptsView
                    .findViewById(R.id.password);


        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setNegativeButton("Enter",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                /** DO THE METHOD HERE WHEN PROCEED IS CLICKED*/
                                String user_text = (userInput.getText()).toString();
                                    if(user_text.equals(context.getResources().getString(R.string.password))) {
//                                        SharedPreferences settings = context.getSharedPreferences(context.getResources().getString(R.string.pref_key),MODE_PRIVATE);
//                                        SharedPreferences.Editor editor = settings.edit();
//                                        editor.clear();
//                                        editor.commit();
                                        CallFragment fragment = (CallFragment)mContext;
                                        fragment.FragmentHandler(SET_UP);


//                                        Intent intent = new Intent(context, set_up.class);
//                                        context.startActivity(intent);
                                    }
                                    else{
                                        dialog.dismiss();
                                        Toast.makeText(context,"You entered an incorrect password",Toast.LENGTH_SHORT).show();
                                    }


                            }
                        })
                .setPositiveButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                dialog.dismiss();
                            }

                        }

                );

      alertDialogBuilder.create().show();


    }

}
