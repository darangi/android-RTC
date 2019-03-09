package com.example.upwork_video_call;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class MyIntentService extends IntentService {

    public static final String CUSTOM_ACTION = "YOUR_CUSTOM_ACTION";

    public MyIntentService() {
        super("MyIntentService");
    }

    @Override
    protected void onHandleIntent(Intent arg0) {
        Intent intent = new Intent(CUSTOM_ACTION);
        // send local broadcast
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
