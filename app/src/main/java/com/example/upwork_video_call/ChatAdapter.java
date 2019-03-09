package com.example.upwork_video_call;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import io.socket.client.Socket;

import static android.content.Context.MODE_PRIVATE;

public class ChatAdapter  extends ArrayAdapter<chatMessages> implements AdapterView.OnItemClickListener {

    private ArrayList<chatMessages> dataSet;
    Context mContext;
    Socket socket = mySocket.getSocket();
    Timestamp ts;
    Date date;
    SimpleDateFormat dateFormat;
    public ChatAdapter(ArrayList<chatMessages> data, Context context) {
        super(context, R.layout.row, data);
        this.dataSet = data;
        this.mContext=context;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final chatMessages chatMessages = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            String MY_PREFS_NAME = mContext.getResources().getString(R.string.pref_key);
            SharedPreferences pref = mContext.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
            String deviceId = pref.getString("deviceId",null);
            if(deviceId.equalsIgnoreCase(chatMessages.senderId)){
                convertView = inflater.inflate(R.layout.chat_row, parent, false);
            }
            else{
                convertView = inflater.inflate(R.layout.chat_row_left, parent, false);

            }
            TextView chat = convertView.findViewById(R.id.chat);
            TextView time = convertView.findViewById(R.id.time);

            dateFormat = new SimpleDateFormat(
                    "yy/MM/dd hh:mm");
            try{
                ts=new Timestamp(chatMessages.time.getTime());
                time.setText(dateFormat.format(ts));
            }
            catch (Exception e){

            }

            chat.setText(chatMessages.message);

        }
        return  convertView;

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("hey","hey");
    }
}

