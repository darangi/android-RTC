package com.example.upwork_video_call;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.app.admin.SystemUpdatePolicy;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.UserManager;
import android.provider.Settings;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.content.ContentValues.TAG;
import static java.security.AccessController.getContext;

public class set_up extends Activity {
    SharedPreferences.Editor editor;
    private EditText serverAddress;
    private EditText receptionAddress;
    private RadioGroup radioGroup;
    private  String position;
    private Intent intent;
    private  String MY_PREFS_NAME;
    private SharedPreferences pref;
    private Boolean isDeviceOwner;
    private ComponentName mAdminComponentName;
    private PackageManager mPackageManager;
    private  DevicePolicyManager mDevicePolicyManager;
    private static final String KIOSK_PACKAGE = "com.example.upwork_video_call/.DeviceAdmin";

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
        setContentView(R.layout.activity_set_up);
        if(checkAndRequestPermission()) {
            MY_PREFS_NAME = getResources().getString(R.string.pref_key);
            pref = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
            String _positon = pref.getString("position", null);
            String server = pref.getString("server", null);
            editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
            serverAddress = (EditText) findViewById(R.id.serverAddress);
            receptionAddress = (EditText) findViewById(R.id.receptionAddress);
            try {
                Runtime.getRuntime().exec("dpm set-device-owner " + KIOSK_PACKAGE);

            } catch (Exception ex) {

            }
            // get policy manager
            mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            mAdminComponentName = DeviceAdmin.getComponentName(this);
            mPackageManager = this.getPackageManager();
            if (mDevicePolicyManager.isDeviceOwnerApp(getPackageName())) {
                setDefaultCosuPolicies(true);
            } else {
                if(_positon != null && _positon.equalsIgnoreCase("reception")){
                    Toast.makeText(this,
                            "This device havent been set as owner", Toast.LENGTH_LONG)
                            .show();
                }
            }
            if (server != null) {
                serverAddress.setText(server);
            }
            if (_positon != null) {
                checkSetup(_positon);
            }
            radioGroup = (RadioGroup) findViewById(R.id.position);
            radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    if (checkedId == R.id.office) {
                        position = "office";
                        receptionAddress.setVisibility(View.GONE);

                    } else if (checkedId == R.id.reception) {
                        position = "reception";
                        receptionAddress.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
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

    public  void unlock(View view){
        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                stopLockTask();
            }

        }
        catch (Exception ex){

        }
    }
    private void setDefaultCosuPolicies(boolean active){
        // set user restrictions
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, active);
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, active);
        setUserRestriction(UserManager.DISALLOW_ADD_USER, active);
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, active);
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, active);

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
                            getPackageName(), set_up.class.getName()));
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

    public void save(View view){
                String server_address = serverAddress.getText().toString();
                if(server_address.isEmpty()){
                    Toast.makeText(set_up.this,"Please enter server address",Toast.LENGTH_SHORT).show();
                }
                else if(position == null){
                    Toast.makeText(set_up.this,"Please select where the device will be placed",Toast.LENGTH_SHORT).show();
                }
                else if(position == "reception" && receptionAddress.getText().toString().isEmpty()){
                    Toast.makeText(set_up.this,"Please provide the name of the reception's location",Toast.LENGTH_SHORT).show();
                }
                else {
                    editor.putString("server",server_address);
                    editor.putString("position",position);
                    String deviceId = position.equals("reception") ? Settings.Secure.getString(getContentResolver(),
                            Settings.Secure.ANDROID_ID) : "admin";
                    editor.putString("receptionLocation",receptionAddress.getText().toString());
                    editor.putString("deviceId",deviceId);
                    editor.apply();
                    checkSetup(position);

                }

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
    public  void checkSetup(String position){
        isDeviceOwner = mDevicePolicyManager.isDeviceOwnerApp(
                getApplicationContext().getPackageName());
        if(isDeviceOwner) {
            pinScreen();
        }
        mySocket connector = (mySocket) getApplication();
        connector.onCreate();
        if (position.equalsIgnoreCase("office")) {
                intent = new Intent(this, Office.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
                finish();
        } else if (position.equalsIgnoreCase("reception")) {
                intent = new Intent(this, welcome.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
                finish();
        }


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
}
