package com.example.upwork_video_call;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;
import android.util.Log;


import com.example.upwork_video_call.R;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public  class mySocket extends Application {
    private static Socket socket;
    public  String isFromOffice;
    SharedPreferences pref;
    String endPoint;
    static Context context;
    static Intent previousIntent;
    @Override
    public void onCreate() {
        super.onCreate();
        String MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        pref = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        endPoint = pref.getString("server", null);
        context = this;
        connectDevice();
    }
    public void connectDevice(){
        try{
            if(endPoint != null) {
                if(socket != null){
                    return;
                }
                socket = IO.socket(endPoint).connect();
                socket.on("connect", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        try{
                            JSONObject json = new JSONObject();
                            json.put("deviceName",getDeviceName());
                            json.put("deviceId", getDeviceId());
                            socket.emit("init",json);
                        }
                        catch (Exception ex){
                        }
                    }
                }).on("reconnect", new Emitter.Listener() {
                            @Override
                            public void call(Object... args) {
                                try{
                                    JSONObject json = new JSONObject();
                                    json.put("deviceName",getDeviceName());
                                    json.put("deviceId", getDeviceId());
                                    socket.emit("init",json);
                                }
                                catch (Exception ex){
                                }
                            }
                }).on("ring", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        JSONObject data = new JSONObject();
                        JSONObject object = (JSONObject)args[0];
                        String deviceId = "" ,deviceName = "";
                        try {
                             deviceId = object.getString("deviceId");
                             deviceName = object.getString("deviceName");
                             data.put("deviceId",deviceId);
                             socket.emit("ringing",data);
                        }
                        catch (Exception e){
                        }
                        if(!isActivityVisible()){
                            activityVisible = true;
                            Intent intent = new Intent(context,CallFragment.class).addFlags(FLAG_ACTIVITY_NEW_TASK);
                            intent.putExtra("deviceId",deviceId);
                            intent.putExtra("deviceName",deviceName);
                            startActivity(intent);
                            return;
                        }
                        activityVisible = true;
                            Intent intent =  new Intent("UPWORK");
                            intent.putExtra("deviceId",deviceId);
                            intent.putExtra("deviceName",deviceName);
                            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }
                }).on("callFromOffice", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Intent intent =  new Intent("UPWORK");
                        intent.putExtra("ringing","ring");
                        intent.putExtra("deviceId", "admin");
                        intent.putExtra("deviceName", "Office");
                        try{
                            JSONObject data = new JSONObject();
                            data.put("deviceId","admin");
                            socket.emit("ringing",data);
                        }
                        catch (Exception e){

                        }
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }
                })
                .on("answered", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Intent intent =  new Intent("UPWORK-RECEPTION-ANSWER");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

                    }
                }).on("hangup", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Intent intent =  new Intent("HANGUP");
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                    }
                });

                Log.d("MUSA", endPoint);
            }
        }
        catch(URISyntaxException ex){
        }
    }
    public static boolean isActivityVisible() {
        return activityVisible;
    }

    public static void activityResumed() {
        activityVisible = true;
    }
    public static   void navigateToPrevious(){
        context.getApplicationContext().startActivity(previousIntent);
    }
    public  static  void setPrevious(Intent intent){
        previousIntent = intent;
    }
    public static void activityPaused() {
        activityVisible = false;
    }

    private static boolean activityVisible;

    public  String getDeviceName(){
        String receptionLocation =  pref.getString("receptionLocation",null) == null ? "Office" :
                pref.getString("receptionLocation",null);
        return receptionLocation;
    }
    public String getDeviceId(){
        String android_id = pref.getString("deviceId",null);
        return android_id;
    }
    public static Socket getSocket(){
        return socket;
    }
    public  void disconnect(){
        socket.disconnect();
    }



}

