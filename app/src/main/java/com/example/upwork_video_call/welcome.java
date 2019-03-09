package com.example.upwork_video_call;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URISyntaxException;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
 import  com.example.upwork_video_call.mySocket;

import org.json.JSONObject;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class welcome extends Activity {
    private  Socket socket;
    private Button dialButton;
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
    private mySocket connector;
    private  JSONObject senderData;
    private  int chatCount;
    private SharedPreferences pref;
    private  String position;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_welcome);
        deviceId = getIntent().getStringExtra("deviceId");
        socket  = mySocket.getSocket();
        MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        pref = getSharedPreferences(MY_PREFS_NAME,MODE_PRIVATE);
        try{
            senderData = new JSONObject();
            senderData.put("senderId",pref.getString("deviceId",null));
        }
        catch (Exception e){

        }
        connectReception();
        LocalBroadcastManager.getInstance(this).registerReceiver(listener, new IntentFilter("UPWORK-RECEPTION"));
        LocalBroadcastManager.getInstance(this).registerReceiver(answer, new IntentFilter("UPWORK-RECEPTION-ANSWER"));
        LocalBroadcastManager.getInstance(this).registerReceiver(hangup, new IntentFilter("HANGUP"));
        this.dialButton = (Button) findViewById(R.id.dial);
        this.disconnectButton = (ImageButton) findViewById(R.id.disconnect);
        this.updateText = (TextView) findViewById(R.id.updateText);
        this.message = (Button) findViewById(R.id.message);
        position = pref.getString("position",null);
        if(deviceId!= null && !deviceId.equalsIgnoreCase("admin")){
            this.dialButton.performClick();
            Toast.makeText(this,"Calling "+getIntent().getStringExtra("deviceName"),Toast.LENGTH_LONG).show();
        }

        doAnimation();
    }


    public void connectReception(){
        socket.on("ringing", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        isRinging();
                    }
                });
            }
        }).on("message", new Emitter.Listener() {
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

    public  void isRinging(){
        dialButton.setBackgroundResource(R.drawable.btn_background_disconnect);
        dialButton.setText(RingingText);
    }

    public  void Settings(View view){
        dialog dialog = new dialog();
        dialog.showDialog(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        deviceId = null;
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        deviceId = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hangup);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(answer);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(listener);
        resetSocketListeners();
        finish();
    }

    private void resetSocketListeners() {
        socket.off("message");
        socket.off("unreadCount");
    }

    public void  chat(View view){
        if(!isRinging){
        intent = new Intent(this,Chat.class);
        intent.putExtra("recipientId","admin");
        intent.putExtra("deviceName","Office");
        startActivity(intent);
        }
    }
    public void disconnectCall(final Boolean  disconnect){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(disconnect) {
                        JSONObject data = new JSONObject();
                        try{
                            data.put("deviceId",deviceId);
                            socket.emit("hangup",data);
                        }
                        catch(Exception e){

                        };
                    }
                    dialButton.setText("Call Reception");
                    dialButton.setBackgroundResource(R.drawable.bg);
                    disconnectButton.setVisibility(View.GONE);
                    isRinging = false;
                    deviceId=null;
                    if(position.equalsIgnoreCase("Office")){
                        intent = new Intent(welcome.this,receptions.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();
                    }

                }
            });
    }


    public void Disconnect(final View view){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                disconnectCall(true);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        deviceId=null;

    }

    public void skypeAnimation(){
        final float fromRadius = getResources().getDimension(R.dimen.buttonRadius);
        final float toRadius = dialButton.getHeight() / 2;
        //create a new gradient color
        final GradientDrawable gd = new GradientDrawable();
        gd.setStroke(R.dimen.stroke,getResources().getColor(R.color.strokeColor));
        final ValueAnimator animator = ValueAnimator.ofFloat(fromRadius, toRadius);
        animator.setDuration(100)
                .addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (float) animation.getAnimatedValue();
                        gd.setColor(getResources().getColor(R.color.colorButton));
                        gd.setShape(GradientDrawable.OVAL);
                        gd.setCornerRadius(value);
                        dialButton.setBackground(gd);
                    }
                });
        animator.start();
    }

    public void doAnimation(){
        final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.shrink);
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

    public void Dial(View view){
        if(isRinging){
            return;
        }
        isRinging = true;
        disconnectButton.setVisibility(View.VISIBLE);
        dialButton.setText("Connecting...");
        Toast.makeText(welcome.this,"Connecting...",Toast.LENGTH_SHORT).show();
        intent = new Intent(this,MainActivity.class);
        JSONObject json = new JSONObject();
        try{
            SharedPreferences pref = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
            json.put("deviceName",pref.getString("receptionLocation",null));
            if(deviceId == null){
                deviceId = "admin";
                json.put("deviceId",  Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID));
                socket.emit("ring",json);
            }
            else{
                json.put("deviceId", deviceId);
                socket.emit("callReception",json);
            }

        }
        catch(Exception e){
            Log.d("EROR",e.getLocalizedMessage());
        }

    }
    private BroadcastReceiver listener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            deviceId = intent.getStringExtra("deviceId");
            String deviceName = intent.getStringExtra("deviceName");
            intent = new Intent(welcome.this,Office.class);
            intent.putExtra("ringing","ring");
            intent.putExtra("deviceId",deviceId);
            intent.putExtra("deviceName",deviceName);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            resetSocketListeners();
            startActivity(intent);
            finish();
        }
    };
    private BroadcastReceiver answer = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            disconnectCall(false);
            intent = new Intent(welcome.this, MainActivity.class);
            if(deviceId != null && deviceId.equalsIgnoreCase("admin")) {
                intent.putExtra("from", "receptions");
            }
            else{
                intent.putExtra("from", "welcome");
            }
            startActivity(intent);
            finish();
        }
    };
    private BroadcastReceiver hangup = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            disconnectCall(false);
        }
    };
}
