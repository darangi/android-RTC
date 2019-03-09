package com.example.upwork_video_call;
import android.app.admin.DeviceAdminReceiver;
import android.content.ComponentName;
import android.content.Context;

public class DeviceAdmin extends DeviceAdminReceiver {
    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), DeviceAdmin.class);
    }
}