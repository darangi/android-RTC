package com.example.upwork_video_call;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;

import static android.content.Context.MODE_PRIVATE;

public class SetupFragment extends Fragment {
    private  EditText serverAddress;
    private  EditText receptionAddress;
    private  Button btnSave;
    private  Button btnUnlock;
    private  Button btnRemoveDpm;
    private  String position;
    private RadioGroup radioGroup;
    private SharedPreferences.Editor editor;
    final int OFFICE = 1;
    final int RECEPTION = 2;
    SharedPreferences pref;
    String MY_PREFS_NAME;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        pref = this.getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.activity_set_up, container, false);
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState)
    {
        position = pref.getString("position", null);
        String server = pref.getString("server", null);
        editor = this.getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
        serverAddress =  view.findViewById(R.id.serverAddress);
        receptionAddress = view.findViewById(R.id.receptionAddress);
        btnSave = view.findViewById(R.id.btlogin);
        btnUnlock = view.findViewById(R.id.btnUnlock);
        btnRemoveDpm = view.findViewById(R.id.btnRemoveDpm);
        radioGroup = view.findViewById(R.id.position);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.office) {
                    position = "office";
                    receptionAddress.setVisibility(View.GONE);

                } else if (checkedId == R.id.reception) {
                    position = "reception";
                    receptionAddress.setVisibility(View.VISIBLE);
                }
            }
        });
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                save();
            }
        });
        btnUnlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unlock();
            }
        });
        btnRemoveDpm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Remove App As Device Owner")
                        .setMessage("Proceeding with this action removes the Device Policy Management (Device owner) set on this device?")

                        // Specifying a listener allows you to take an action before dismissing the dialog.
                        // The dialog is automatically dismissed when a dialog button is clicked.
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                CallFragment fragment = (CallFragment)getActivity();
                                fragment.removeDPM();
                            }
                        })
                        // A null listener allows the button to dismiss the dialog and take no further action.
                        .setNegativeButton(android.R.string.no, null)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }
    public  void unlock(){
        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                this.getActivity().stopLockTask();
            }

        }
        catch (Exception ex){

        }
    }
    public void save(){
        String server_address = serverAddress.getText().toString();
        if(server_address.isEmpty()){
            Toast.makeText(this.getContext(),"Please enter server address",Toast.LENGTH_SHORT).show();
        }
        else if(position == null){
            Toast.makeText(this.getContext(),"Please select where the device will be placed",Toast.LENGTH_SHORT).show();
        }
        else if(position.equalsIgnoreCase("reception") && receptionAddress.getText().toString().isEmpty()){
            Toast.makeText(this.getContext(),"Please provide the name of the reception's location",Toast.LENGTH_SHORT).show();
        }
        else {
            editor.putString("server",server_address);
            editor.putString("position",position);
            String deviceId = position.equals("reception") ? Settings.Secure.getString(this.getActivity().getContentResolver(),
                    Settings.Secure.ANDROID_ID) : "admin";
            editor.putString("receptionLocation",receptionAddress.getText().toString());
            editor.putString("deviceId",deviceId);
            editor.apply();
            startActivity(new Intent(getContext(),CallFragment.class));
//            CallFragment fragment = (CallFragment)getActivity();
//            if(position.equalsIgnoreCase("Office")){
//                fragment.FragmentHandler(OFFICE);
//            }
//            else{
//                fragment.FragmentHandler(RECEPTION);
//            }
        }

    }


}
