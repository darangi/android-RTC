package com.example.upwork_video_call;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.telecom.Call;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONObject;

import io.socket.client.Socket;

import static android.content.Context.MODE_PRIVATE;

public class IncomingCallFragment extends Fragment {
    private static MediaPlayer ringTone;
    private NotificationManager manager;
    private Socket socket;
    private  String deviceId;
    private String deviceName;
    private String position;
    final int OFFICE = 1;
    final int RECEPTION = 2;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        deviceId = getArguments().getString("deviceId");
        deviceName = getArguments().getString("deviceName");
        socket = mySocket.getSocket();
        String MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        SharedPreferences pref = this.getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        position = pref.getString("position",null);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.incoming_call_screen, container, false);
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState)
    {
        view.findViewById(R.id.answer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                answer();
            }
        });
        view.findViewById(R.id.disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });
        TextView textView = view.findViewById(R.id.updateText);
        textView.setText(deviceName);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    public void answer(){
        Intent intent =  new Intent(getContext(),MainActivity.class);
        intent.putExtra("deviceId",deviceId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        JSONObject data = new JSONObject();
        try{
            data.put("deviceId",deviceId);
            socket.emit("answered",data);
        }
        catch(Exception e){

        }
        //stop ring tone
        stopRingTone();
    }

    private void stopRingTone() {
        CallFragment fragment = (CallFragment) getActivity();
        fragment.stopRingTone();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //stop ring tone
        stopRingTone();
    }
    @Override
    public void onDetach() {
        super.onDetach();
        stopRingTone();
    }

    @Override
    public void onStop() {
        super.onStop();
        //stop ring tone
        stopRingTone();
    }

    public void disconnect(){
        //stop ring tone
        stopRingTone();
        JSONObject data = new JSONObject();
        try{
            data.put("deviceId",deviceId);
            socket.emit("hangup",data);
        }
        catch(Exception e){

        }
        CallFragment fragment = (CallFragment) getActivity();
        //check if i belong here
        if(position.equalsIgnoreCase("office")){
            fragment.FragmentHandler(OFFICE);

        }
        else{
            fragment.FragmentHandler(RECEPTION);

        }
    }


}
