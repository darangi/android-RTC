package com.example.upwork_video_call;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import io.socket.client.Socket;

import static android.content.Context.MODE_PRIVATE;

public class OfficeFragment extends Fragment {
    private Socket socket;
    private Button btnCallReception;
    private ImageButton btnSettings;
    private  String MY_PREFS_NAME;
    private  String position;
    private  SharedPreferences pref;
    private  JSONObject senderData;
    private  final int RECEPTIONS = 4;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        socket  = mySocket.getSocket();
        pref = this.getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);

        try{
            senderData = new JSONObject();
            senderData.put("senderId",pref.getString("deviceId",null));
        }
        catch (Exception e){

        }

        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.activity_office, container, false);
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState)
    {
        position = pref.getString("position",null);
        btnCallReception = view.findViewById(R.id.btnCallReception);
        btnSettings = view.findViewById(R.id.imageButton);
        btnCallReception.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallFragment fragment = (CallFragment)getActivity();
                fragment.FragmentHandler(RECEPTIONS);
            }
        });
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings();
            }
        });
    }
    public  void Settings(){
        dialog dialog = new dialog();
        dialog.showDialog(getContext());
    }
}
