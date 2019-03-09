package com.example.upwork_video_call;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.IntentService;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONObject;

import java.net.URISyntaxException;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;


class MyBounceInterpolator implements android.view.animation.Interpolator {
    private double mAmplitude = 1;
    private double mFrequency = 10;

    MyBounceInterpolator(double amplitude, double frequency) {
        mAmplitude = amplitude;
        mFrequency = frequency;
    }

    public float getInterpolation(float time) {
        return (float) (-1 * Math.pow(Math.E, -time/ mAmplitude) *
                Math.cos(mFrequency * time) + 1);
    }
}

public class Office extends Activity {
    private Socket socket;
    private TextView welcomeText;
    private TextView updateText;
    private LinearLayout linearLayout;
    private LinearLayout btnCallOffice;
    private LinearLayout buttons;
    private static MediaPlayer ringTone;
    private NotificationManager manager;

    private Boolean isRinging = false;
    private Boolean appInFront = false;
    private Intent intent;
    private String RingingText = "Ringing...";
    private  String MY_PREFS_NAME;
    private  String deviceId = "admin";
    private String deviceName = "Office";
    private SharedPreferences pref;
    private  int chatCount;
    private  Button message;
    private  JSONObject senderData;
    private String position;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);

        setContentView(R.layout.activity_office);
        MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        socket  = new mySocket().getSocket();
        pref = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        position = pref.getString("position",null);
        this.updateText = (TextView) findViewById(R.id.updateText);
        this.welcomeText = (TextView) findViewById(R.id.office);
        this.linearLayout = (LinearLayout) findViewById(R.id.glow);
        this.btnCallOffice = (LinearLayout) findViewById(R.id.btnCallOffice);
        this.buttons = (LinearLayout) findViewById(R.id.buttons);
        this.message = (Button) findViewById(R.id.message);
        try{
            senderData = new JSONObject();
            senderData.put("senderId",pref.getString("deviceId",null));
        }
        catch (Exception e){

        }

        appInFront = false;
        LocalBroadcastManager.getInstance(Office.this).registerReceiver(listener, new IntentFilter("UPWORK"));
        LocalBroadcastManager.getInstance(this).registerReceiver(hangup, new IntentFilter("HANGUP"));
        connectOffice();
        String shouldRing = getIntent().getStringExtra("ringing");
        deviceId = getIntent().getStringExtra("deviceId") != null ? getIntent().getStringExtra("deviceId") : deviceId;
        deviceName = getIntent().getStringExtra("deviceName") != null ? getIntent().getStringExtra("deviceName") : deviceName;
        wakeScreen();
        if(shouldRing != null){
            ring();
            LocalBroadcastManager.getInstance(this).unregisterReceiver(listener);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(hangup);
        }


    }



    public  void prepareToRing(){

        AudioManager am=(AudioManager)getSystemService(Context.AUDIO_SERVICE);
        am.setMode(AudioManager.MODE_NORMAL);
        Uri ringtoneUri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringTone = new MediaPlayer();
        try {
            ringTone.setDataSource(getApplicationContext(), ringtoneUri);
            ringTone.setAudioStreamType(AudioManager.STREAM_RING);
            ringTone.prepare();
        }
        catch (Exception ex){

        }
    }
    public void wakeScreen(){
        PowerManager pm  = (PowerManager)getSystemService(POWER_SERVICE);
        PowerManager.WakeLock screenLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                ,getResources().getString(R.string.action_sign_in));
        screenLock.acquire(500);
        screenLock.release();

    }
    @Override
    protected void onResume() {
        super.onResume();
        appInFront  = true;
        LocalBroadcastManager.getInstance(Office.this).registerReceiver(listener, new IntentFilter("UPWORK"));
        LocalBroadcastManager.getInstance(this).registerReceiver(hangup, new IntentFilter("HANGUP"));
        mySocket.activityResumed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(Office.this).registerReceiver(listener, new IntentFilter("UPWORK"));
        LocalBroadcastManager.getInstance(this).registerReceiver(hangup, new IntentFilter("HANGUP"));
        mySocket.activityPaused();
        resetLayout();
    }

    public void Answer(View view){
        isRinging = false;
        intent =  new Intent(this,MainActivity.class);
        intent.putExtra("deviceId",deviceId);
        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        resetLayout();
        resetSocketListeners();

        JSONObject data = new JSONObject();
        try{
            data.put("deviceId",deviceId);
            socket.emit("answered",data);
            finish();
        }
        catch(Exception e){

        }
    }
    public void  chat(View view){
        startActivity(new Intent(this,receptions.class));
        finish();
    }
    public void Disconnect(View view){
        resetLayout();
        //check if i belong here
        if(!position.equals("office")){
            Intent intent = new Intent(this,welcome.class);
            intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
        JSONObject data = new JSONObject();
        try{
            data.put("deviceId",deviceId);
            socket.emit("hangup",data);
        }
        catch(Exception e){

        }
    }

    public  Intent getIntentt(){
        Intent answerCall = new Intent(this,MainActivity.class);
        answerCall.putExtra("from", "Office");
        return answerCall;
    }

    public  void resetSocketListeners(){
        if(socket!=null) {
            socket.off("unreadCount");
            socket.off("message");
        }
    }
    public  void Settings(View view){
        dialog dialog = new dialog();
        dialog.showDialog(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        resetSocketListeners();
        resetLayout();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hangup);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(listener);
        if(ringTone != null){
            ringTone.stop();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetSocketListeners();
        resetLayout();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hangup);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(listener);
        if(ringTone != null){
            ringTone.stop();
        }
        finish();
    }

    public void resetLayout(){
        isRinging = false;
        if(ringTone != null){
            ringTone.reset();
            ringTone.stop();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                linearLayout.setVisibility(View.GONE);
                btnCallOffice.setVisibility(View.VISIBLE);
                buttons.setVisibility(View.GONE);
                updateText.setVisibility(View.GONE);
                welcomeText.setVisibility(View.VISIBLE);
            }
        });
        if(intent != null && intent.getStringExtra("ringing") != null){
            intent.removeExtra("ringing");
        }
        if(!deviceId.equalsIgnoreCase("admin") && position.equalsIgnoreCase("reception")){
            intent = new Intent(this,welcome.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
    public  void  CallReception(View view){
        intent = new Intent(this,receptions.class);
        intent.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
    public void ring(){
        try {
            if(ringTone != null){
                ringTone.stop();
            }
            prepareToRing();
            ringTone.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run () {
                linearLayout.setVisibility(View.VISIBLE);
                btnCallOffice.setVisibility(View.GONE);
                buttons.setVisibility(View.VISIBLE);
                welcomeText.setVisibility(View.GONE);
                updateText.setVisibility(View.VISIBLE);
                updateText.setText("Incoming call from "+deviceName);
                if(deviceId == "admin"){
                    socket.emit("receptionIsRinging");
                }
            }
        });
    }

    public void connectOffice() {
        socket.on("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatCount++;
                        message.setText(Integer.toString(chatCount));
                    }
                });
            }
        }).on("unreadCount", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject object = (JSONObject)args[0];
                try{
                    int count =Integer.parseInt(object.getString("count"));
                    chatCount = chatCount + count;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(chatCount > 0){
                                message.setText(Integer.toString(chatCount));
                            }
                        }
                    });
                }
                catch(Exception e){

                }

            }
        }).emit("unreadCount",senderData);


    }
    private BroadcastReceiver listener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            String deviceId = intent.getStringExtra("deviceId");
            String deviceName = intent.getStringExtra("deviceName");
            intent = new Intent(Office.this,Office.class);
            intent.putExtra("ringing","ring");
            intent.putExtra("deviceId",deviceId);
            intent.putExtra("deviceName",deviceName);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            wakeScreen();
            resetSocketListeners();
            startActivity(intent);
            finish();
        }
    };
    private BroadcastReceiver hangup = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            resetLayout();
        }
    };

}
