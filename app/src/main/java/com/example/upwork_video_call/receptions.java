package com.example.upwork_video_call;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;

import java.net.URISyntaxException;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class receptions extends Activity {
    Socket socket;
    JSONArray onlineReceptions;
    ListView list;
    TextView receptionText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.receptions);
        String MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        SharedPreferences pref = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(hangup, new IntentFilter("HANGUP"));
        LocalBroadcastManager.getInstance(this).registerReceiver(listener, new IntentFilter("UPWORK"));
        KeyguardManager manager = (KeyguardManager) this.getSystemService(Context.KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock lock = manager.newKeyguardLock("abc");
        lock.disableKeyguard();

        if(pref.getString("position",null).equalsIgnoreCase("reception")){
          startActivity(new Intent(this,welcome.class));
          finish();
        }
        list = (ListView) findViewById(R.id.list);
        receptionText = (TextView) findViewById(R.id.noReceptions);
        list.setClickable(true);
        socket  = mySocket.getSocket();
        socket.on("receptions", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try{
                    Type listType = new TypeToken<ArrayList<dataModel>>(){}.getType();
                    ArrayList<dataModel> receptions = new Gson().fromJson(args[0].toString(), listType);
                    setReceptions(receptions);
                }
                catch(Exception e){
                    Log.d("ERROR",e.getLocalizedMessage());
                }

                }
        }).on("receptionAnswered", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(receptions.this,MainActivity.class);
                            intent.putExtra("from","receptions");
                            Toast.makeText(receptions.this,"Reception Picked",Toast.LENGTH_LONG).show();
                            startActivity(intent);
                            finish();
                        }
                    });
                }
                catch (Exception e){

                }
            }
        })
        .on("ringing", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(receptions.this,"Ringing",Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
        socket.emit("getReceptions");
        }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        goToOffice();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        goToOffice();
    }

    private void goToOffice() {
        resetSocketListeners();
        Intent intent = new Intent(this,Office.class);
        startActivity(intent);
        finish();
    }

    public void setReceptions(final ArrayList<dataModel> receptions){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(receptions.size() > 0){
                    receptionText.setVisibility(View.GONE);
                    list.setVisibility(View.VISIBLE);
//                    CustomAdapter adapter = new CustomAdapter(receptions,receptions.this,receptions.getClass());
//                    list.setAdapter(adapter);
                }
            }
        });
    }

    private void resetSocketListeners() {

        socket.off("answered");
        socket.off("receptions");
        socket.off("ringing");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    private BroadcastReceiver listener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            String deviceId = intent.getStringExtra("deviceId");
            String deviceName = intent.getStringExtra("deviceName");
            intent = new Intent(receptions.this,Office.class);
            intent.putExtra("ringing","ring");
            intent.putExtra("deviceId",deviceId);
            intent.putExtra("deviceName",deviceName);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            resetSocketListeners();
            startActivity(intent);
            finish();
        }
    };
    private BroadcastReceiver hangup = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            goToOffice();
        }
    };
}
