package com.example.upwork_video_call;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

import io.socket.client.Socket;

public class CustomAdapter extends ArrayAdapter<dataModel> implements AdapterView.OnItemClickListener {

    private ArrayList<dataModel> dataSet;
    Context mContext;
    Activity mActivity;
    Socket socket = mySocket.getSocket();
    public CustomAdapter(ArrayList<dataModel> data, Context context, Activity activity) {
        super(context, R.layout.row, data);
        this.dataSet = data;
        this.mContext=context;
        this.mActivity = activity;

    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        final dataModel dataModel = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row, parent, false);
            TextView reception = convertView.findViewById(R.id.txtReception);
            Button btnCall = convertView.findViewById(R.id.btnCall);
            Button btnTxt = convertView.findViewById(R.id.btnText);
            if(dataModel.unreadCount != null && Integer.parseInt(dataModel.unreadCount) > 0){
                btnTxt.setText(dataModel.unreadCount);
            }
            btnCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CallFragment fragment = (CallFragment)mActivity;
                    fragment.callReceptionFromOffice(dataModel.deviceId,dataModel.deviceName);
                }
            });
            btnTxt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext,Chat.class);
                    intent.putExtra("recipientId",dataModel.deviceId);
                    intent.putExtra("deviceName",dataModel.deviceName);
                    mContext.startActivity(intent);
                }
            });
            reception.setText(dataModel.deviceName);
        }
        return  convertView;

    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.d("hey","hey");
    }
}

