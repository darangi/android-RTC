package com.example.upwork_video_call;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.PowerManager;
import android.os.UserManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static java.security.AccessController.getContext;

public class CallFragment extends FragmentActivity {
    SharedPreferences pref;
    final int SET_UP = 0;
    final int OFFICE = 1;
    final int RECEPTION = 2;
    final int INCOMING_CALL_SCREEN = 3;
    final int RECEPTIONS = 4;
    private  DevicePolicyManager mDevicePolicyManager;
    private ComponentName mAdminComponentName;
    private  MediaPlayer ringTone;
    private PackageManager mPackageManager;
    private  String deviceId,deviceName,position;
    private  Boolean isRinging = false;
    private  FragmentTransaction fragmentTransaction;
    private  IncomingCallFragment fragment;
    private Boolean isDeviceOwner;
    private Boolean lastFragmentWasReceptions = false;
    private MediaPlayer mp;
    private  AudioManager am;
    private NotificationManager mNotificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_call);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        mySocket.activityResumed();
        fragment = new IncomingCallFragment();
        String MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        pref = getSharedPreferences(MY_PREFS_NAME,MODE_PRIVATE);
        position = pref.getString("position",null);
        deviceId = getIntent().getStringExtra("deviceId");
        deviceName = getIntent().getStringExtra("deviceName") ;
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyApp::MyWakelockTag");
        wakeLock.acquire();
        am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        ringTone = new MediaPlayer();
        LocalBroadcastManager.getInstance(this).registerReceiver(listener, new IntentFilter("UPWORK"));
        LocalBroadcastManager.getInstance(this).registerReceiver(hangup, new IntentFilter("HANGUP"));
        LocalBroadcastManager.getInstance(this).registerReceiver(answer, new IntentFilter("UPWORK-RECEPTION-ANSWER"));

        //get policy manager
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        mAdminComponentName = DeviceAdmin.getComponentName(this);
        mPackageManager = this.getPackageManager();
        isDeviceOwner = mDevicePolicyManager.isDeviceOwnerApp(getPackageName());

        if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
            setDefaultCosuPolicies(true);
        }
        if(checkAndRequestPermission()) {
            if (position == null) {
                FragmentHandler(SET_UP);
            }
            else {
                //connect the socket;
                mySocket connector = (mySocket) getApplication();
                connector.onCreate();
                if(deviceId != null){
                     FragmentHandler(INCOMING_CALL_SCREEN);
                }
                else if (position.equalsIgnoreCase("office")) {
                    FragmentHandler(OFFICE);
                } else {
                    FragmentHandler(RECEPTION);
                }
            }
        }


    }

    public void prepareToRing(){
        AudioManager am=(AudioManager)this.getSystemService(AUDIO_SERVICE);
        am.setMode(AudioManager.STREAM_RING);
        Uri ringtoneUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        mp = MediaPlayer.create(CallFragment.this,ringtoneUri);
        mp.start();
    }
    public Boolean checkAndRequestPermission(){
        String[] appPermissions = {
                Manifest.permission.INTERNET,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.DISABLE_KEYGUARD,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.READ_PHONE_STATE
        };
        List<String> permissionsNeeded = new ArrayList<>();
        for(String perm : appPermissions){
            if(ContextCompat.checkSelfPermission(this,perm) != PackageManager.PERMISSION_GRANTED){
                permissionsNeeded.add(perm);
            }
        }
        if(!permissionsNeeded.isEmpty()){
            ActivityCompat.requestPermissions(this,permissionsNeeded.toArray(new String[permissionsNeeded.size()]),0);
            return false;
        }
        return true;
    }

    @Override
    @NonNull
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 0){
            HashMap<String,Integer> permissionResults = new HashMap<>();
            int deniedCount = 0;
            for(int i = 0; i < grantResults.length ; i ++){
                if(grantResults[i] == PackageManager.PERMISSION_DENIED){
                    permissionResults.put(permissions[i],grantResults[i]);
                    deniedCount++;
                }
            }
            if(deniedCount == 0){
                Intent intent = getIntent();
                startActivity(intent);
                finish();
            }
            else{
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("App permissions");
                builder.setCancelable(true);
                builder.setMessage("You need to set all requested permissions");
                builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void setDefaultCosuPolicies(boolean active){
        // set user restrictions
//        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active);
//        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, active);
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
//        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active);

        // disable keyguard and status bar
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, active);
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, active);

        // enable STAY_ON_WHILE_PLUGGED_IN
        enableStayOnWhilePluggedIn(active);

        // set system update policy
        if (active){
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName,
                    SystemUpdatePolicy.createWindowedInstallPolicy(60, 120));
        } else {
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName,
                    null);
        }

        // set this Activity as a lock task package

        mDevicePolicyManager.setLockTaskPackages(mAdminComponentName,
                active ? new String[]{getPackageName()} : new String[]{});

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MAIN);
        intentFilter.addCategory(Intent.CATEGORY_HOME);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);

        if (active) {
            // set Cosu activity as home intent receiver so that it is started
            // on reboot
            mDevicePolicyManager.addPersistentPreferredActivity(
                    mAdminComponentName, intentFilter, new ComponentName(
                            getPackageName(), CallFragment.class.getName()));
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                    mAdminComponentName, getPackageName());
        }
    }
    private void setUserRestriction(String restriction, boolean disallow){
        if (disallow) {
            mDevicePolicyManager.addUserRestriction(mAdminComponentName,
                    restriction);
        } else {
            mDevicePolicyManager.clearUserRestriction(mAdminComponentName,
                    restriction);
        }
    }
    private void enableStayOnWhilePluggedIn(boolean enabled){
        if (enabled) {
            mDevicePolicyManager.setGlobalSetting(
                    mAdminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    Integer.toString(BatteryManager.BATTERY_PLUGGED_AC
                            | BatteryManager.BATTERY_PLUGGED_USB
                            | BatteryManager.BATTERY_PLUGGED_WIRELESS));
        } else {
            mDevicePolicyManager.setGlobalSetting(
                    mAdminComponentName,
                    Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
                    "0"
            );
        }
    }
    public void removeDPM(){
        if(isDeviceOwner) {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                    mAdminComponentName, getPackageName());
            mDevicePolicyManager.clearDeviceOwnerApp(getPackageName());
            stopLockTask();
            Toast.makeText(this,"Device Owner Removed, you need to set it using adb",Toast.LENGTH_LONG).show();

        }
        else{
            Toast.makeText(this,"Device owner not set",Toast.LENGTH_SHORT).show();
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
        mySocket.activityResumed();
        LocalBroadcastManager.getInstance(this).registerReceiver(listener, new IntentFilter("UPWORK"));
        LocalBroadcastManager.getInstance(this).registerReceiver(hangup, new IntentFilter("HANGUP"));
        LocalBroadcastManager.getInstance(this).registerReceiver(answer, new IntentFilter("UPWORK-RECEPTION-ANSWER"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        mySocket.activityPaused();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hangup);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(listener);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(answer);
        stopRingTone();
    }

    @Override
    public void onBackPressed() {
        if(lastFragmentWasReceptions){
            FragmentHandler(OFFICE);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mySocket.activityPaused();
    }

    public void pinScreen(){
        // start lock task mode if its not already active
        if(mDevicePolicyManager.isLockTaskPermitted(this.getPackageName())){
            ActivityManager am = (ActivityManager) getSystemService(
                    Context.ACTIVITY_SERVICE);
            if(am.getLockTaskModeState() ==
                    ActivityManager.LOCK_TASK_MODE_NONE) {
                startLockTask();
            }
        }
    }
    public void stopRingTone() {
        try{
            if(mp.isPlaying()) {
                mp.stop();
                mp.release();
            }
        }
        catch (Exception e){

        }
    }
    public void FragmentHandler(int val)
    {
        fragmentTransaction = getSupportFragmentManager().beginTransaction();
        lastFragmentWasReceptions = false;
        isRinging = false;
        switch (val)
        {
            case SET_UP:
                fragmentTransaction.replace(R.id.mainContainer, new SetupFragment(),"SET_UP");
                break;
            case OFFICE:
                stopRingTone();
                fragmentTransaction.replace(R.id.mainContainer, new OfficeFragment());
                break;
            case RECEPTION:
                stopRingTone();
                if(isDeviceOwner) {
                    pinScreen();
                }
                else{
                    Toast.makeText(this,
                            "This device haven't been set as owner", Toast.LENGTH_LONG)
                            .show();
                }
                fragmentTransaction.replace(R.id.mainContainer, new ReceptionFragment());
                break;

            case RECEPTIONS:
                fragmentTransaction.replace(R.id.mainContainer, new ReceptionsFragment());
                lastFragmentWasReceptions = true;
                break;
            case INCOMING_CALL_SCREEN:
                Bundle args = new Bundle();
                args.putString("deviceId",deviceId);
                args.putString("deviceName",deviceName);
                fragment.setArguments(args);
                //remove deviceId and deviceName from intents
                if(getIntent().getStringExtra("deviceId") != null){
                    getIntent().removeExtra("deviceId");
                }
                if(getIntent().getStringExtra("deviceId") != null){
                    getIntent().removeExtra("deviceName");
                }

            new android.os.Handler().postDelayed(
                        new Runnable() {
                            public void run() {
                                prepareToRing();
                            }
                        },
                    500);

                fragmentTransaction.replace(R.id.mainContainer,fragment);
                break;
        }
        fragmentTransaction.commitAllowingStateLoss();

    }

    public void callReceptionFromOffice(String deviceId, String deviceName){
        Bundle args = new Bundle();
        args.putString("deviceId",deviceId);
        args.putString("deviceName",deviceName);
        ReceptionFragment frag = new ReceptionFragment();
        frag.setArguments(args);
        FragmentTransaction fragTransaction = getSupportFragmentManager().beginTransaction();
        fragTransaction.replace(R.id.mainContainer,frag);
        fragTransaction.commitAllowingStateLoss();

    }
    protected void changeInterruptionFiler(int interruptionFilter){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){ // If api level minimum 23
            /*
                boolean isNotificationPolicyAccessGranted ()
                    Checks the ability to read/modify notification policy for the calling package.
                    Returns true if the calling package can read/modify notification policy.
                    Request policy access by sending the user to the activity that matches the
                    system intent action ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS.

                    Use ACTION_NOTIFICATION_POLICY_ACCESS_GRANTED_CHANGED to listen for
                    user grant or denial of this access.

                Returns
                    boolean

            */
            // If notification policy access granted for this package
            if(mNotificationManager.isNotificationPolicyAccessGranted()){
                /*
                    void setInterruptionFilter (int interruptionFilter)
                        Sets the current notification interruption filter.

                        The interruption filter defines which notifications are allowed to interrupt
                        the user (e.g. via sound & vibration) and is applied globally.

                        Only available if policy access is granted to this package.

                    Parameters
                        interruptionFilter : int
                        Value is INTERRUPTION_FILTER_NONE, INTERRUPTION_FILTER_PRIORITY,
                        INTERRUPTION_FILTER_ALARMS, INTERRUPTION_FILTER_ALL
                        or INTERRUPTION_FILTER_UNKNOWN.
                */

                // Set the interruption filter
                mNotificationManager.setInterruptionFilter(interruptionFilter);
            }else {
                /*
                    String ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS
                        Activity Action : Show Do Not Disturb access settings.
                        Users can grant and deny access to Do Not Disturb configuration from here.

                    Input : Nothing.
                    Output : Nothing.
                    Constant Value : "android.settings.NOTIFICATION_POLICY_ACCESS_SETTINGS"
                */
                // If notification policy access not granted for this package
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                startActivity(intent);
            }
        }
    }


    private BroadcastReceiver listener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            if(!isRinging){
                changeInterruptionFiler(NotificationManager.INTERRUPTION_FILTER_ALL);
                deviceId = intent.getStringExtra("deviceId");
                deviceName = intent.getStringExtra("deviceName");
                FragmentHandler(INCOMING_CALL_SCREEN);
                isRinging = true;
            }

        }
    };
    private BroadcastReceiver hangup = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            isRinging = false;
            if(position.equalsIgnoreCase("office")){
                FragmentHandler(OFFICE);
            }
            else {
                FragmentHandler(RECEPTION);
            }
            stopRingTone();

        }
    };
    private BroadcastReceiver answer = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            deviceId = intent.getStringExtra("deviceId");
            intent = new Intent(context,MainActivity.class);
            intent.putExtra("deviceId",deviceId);
            stopRingTone();
            startActivity(intent);
        }
    };

}

