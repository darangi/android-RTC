package com.example.upwork_video_call;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.content.Context.MODE_PRIVATE;

public class ReceptionFragment extends Fragment {
    private Socket socket;
    private Button dialButton;
    private ImageButton btnSettings;
    private ImageButton disconnectButton;
    private TextView updateText;
    private Button message;
    private Ringtone ringTone;
    private Intent intent;
    private String DefaultText = "Welcome, kindly place a call";
    private String RingingText = "Ringing...";
    private String ConnectingText = "Connecting...";
    private Boolean isRinging = false;
    private String MY_PREFS_NAME;
    private String deviceId;
    private String deviceName;
    private mySocket connector;
    private  JSONObject senderData;
    private  int chatCount;
    private SharedPreferences pref;
    private  String position;
    final int OFFICE = 1;
    final int RECEPTION = 2;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        socket  =  mySocket.getSocket();
        pref = this.getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        position = pref.getString("position",null);
        try{
            deviceId = getArguments().getString("deviceId",null);
            deviceName = getArguments().getString("deviceName",null);

        }
        catch (Exception ex){

        }

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.activity_welcome, container, false);
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState)
    {
        dialButton = view.findViewById(R.id.dial);
        disconnectButton = view.findViewById(R.id.disconnect);
        updateText = view.findViewById(R.id.updateText);
        btnSettings = view.findViewById(R.id.settings);
        message = view.findViewById(R.id.message);
        doAnimation();
        connectReception();
        if(deviceId != null){
            Dial();
        }
        dialButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dial();
            }
        });
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectCall(true);
            }
        });
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Settings();
            }
        });
    }

    public void connectReception(){
        socket.on("ringing", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if(isAdded()){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isRinging();
                        }
                    });
                }
            }
        });
    }
    public  void isRinging(){
        dialButton.setBackgroundResource(R.drawable.btn_background_disconnect);
        dialButton.setText(RingingText);
    }

    public  void Settings(){
        dialog dialog = new dialog();
        dialog.showDialog(getContext());
    }

    public void doAnimation(){
        final Animation myAnim = AnimationUtils.loadAnimation(getContext(), R.anim.shrink);
        // Use bounce interpolator with amplitude 0.2 and frequency 20
        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.3, 20);
        myAnim.setInterpolator(interpolator);
        myAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                dialButton.startAnimation(myAnim);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        dialButton.startAnimation(myAnim);
    }
    public void disconnectCall(final Boolean  disconnect){
        if(isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (disconnect) {
                        JSONObject data = new JSONObject();
                        try {
                            data.put("deviceId", deviceId);
                            socket.emit("hangup", data);
                        } catch (Exception e) {

                        }
                        ;
                    }
                    dialButton.setText("Call Reception");
                    dialButton.setBackgroundResource(R.drawable.bg);
                    disconnectButton.setVisibility(View.GONE);
                    isRinging = false;
                    deviceId = null;

                    CallFragment fragment = (CallFragment) getActivity();
                    if (position.equalsIgnoreCase("Office")) {
                        fragment.FragmentHandler(OFFICE);
                    } else {
                        fragment.FragmentHandler(RECEPTION);
                    }

                }
            });
        }
    }


    public void Dial(){
        if(isRinging){
            return;
        }
        isRinging = true;
        disconnectButton.setVisibility(View.VISIBLE);
        dialButton.setText("Connecting...");
        Toast.makeText(getContext(),"Connecting...",Toast.LENGTH_SHORT).show();
        JSONObject json = new JSONObject();
        try{
            SharedPreferences pref = getActivity().getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
            json.put("deviceName",pref.getString("receptionLocation",null));
            if(deviceId == null){
                deviceId = "admin";
                json.put("deviceId",  Settings.Secure.getString(getActivity().getContentResolver(),
                        Settings.Secure.ANDROID_ID));
                socket.emit("ring",json);
            }
            else{
                Toast.makeText(getContext(),"Dialing "+deviceName,Toast.LENGTH_SHORT).show();
                json.put("deviceId", deviceId);
                json.put("deviceName", "Office");
                socket.emit("callReception",json);
            }

        }
        catch(Exception e){
            Log.d("ERROR",e.getLocalizedMessage());
        }
    }
}
