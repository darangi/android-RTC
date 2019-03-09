package com.example.upwork_video_call;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.lang.reflect.Type;
import java.util.ArrayList;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class Chat extends Activity {

    private Socket socket;
    private ListView list;
    private EditText chat;
    private  JSONObject chatData;
    private  ArrayList<chatMessages> messages;
    String deviceName;
    String recipientId;
    String senderId;
    SharedPreferences pref;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        list = (ListView) findViewById(R.id.list);
        chat = (EditText) findViewById(R.id.chatContent);


        socket = mySocket.getSocket();
        JSONObject data = new JSONObject();
        chatData = new JSONObject();
        String MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        pref = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
        senderId = pref.getString("deviceId",null);
        deviceName = getIntent().getStringExtra("deviceName");
        recipientId = getIntent().getStringExtra("recipientId");
        TextView isChattingWith = (TextView)findViewById(R.id.deviceName);
        isChattingWith.setText(deviceName);
        try{
            data.put("senderId",senderId);
        }
        catch (Exception e){

        }
        socket.once("messages", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                try{
                    Type listType = new TypeToken<ArrayList<chatMessages>>(){}.getType();
                    messages = new Gson().fromJson(args[0].toString(), listType);
                    setMessages(messages);
                    messageReceived();
                }
                catch(Exception e){
                    Log.d("ERROR",e.getLocalizedMessage());
                }
            }
        }).once("message", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                chatMessages message = new Gson().fromJson(args[0].toString(),chatMessages.class);
                if(messages == null){
                    messages = new ArrayList<chatMessages>();
                }
                messages.add(message);
                setMessages(messages);
                messageReceived();
            }
        }).emit("getMessages",data);
    }
    public void messageReceived(){
        JSONObject messageData = new JSONObject();
        try{
            messageData.put("recipientId",pref.getString("deviceId",null));
            socket.emit("messageReceived",messageData);
        }
        catch (Exception e){

        }
    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent;
        if(senderId.equalsIgnoreCase("admin")){
            intent = new Intent(this,Office.class);
        }
        else{
            intent = new Intent(this,welcome.class);

        }
        startActivity(intent);
        finish();
    }

    public void  chat(View view){
        String message = chat.getText().toString();
        if(!message.isEmpty()){
            try{
                chatData.put("message",message);
                chatData.put("recipientId",recipientId);
                chatData.put("senderId",senderId);
                chatMessages msg = new Gson().fromJson(chatData.toString(),chatMessages.class);
                if(messages == null){
                    messages = new ArrayList<chatMessages>();
                }
                messages.add(msg);
                setMessages(messages);

                socket.emit("message",chatData);
                chat.setText("");
            }
            catch (Exception e){
                System.out.println(e.getLocalizedMessage());
            }
        }
    }
    public void setMessages(final ArrayList<chatMessages> messages){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(messages.size() > 0){
                    list.setVisibility(View.VISIBLE);
                    ChatAdapter adapter = new ChatAdapter(messages,Chat.this);
                    list.setAdapter(adapter);
                    list.scrollListBy(list.getHeight());
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        socket.off("messages");
        socket.off("message");
    }
}
