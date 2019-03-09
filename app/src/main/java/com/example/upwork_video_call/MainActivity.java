package com.example.upwork_video_call;

import android.app.Activity;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URISyntaxException;
import java.util.ArrayList;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

public class MainActivity extends Activity {

    private static final String VIDEO_TRACK_ID = "video1";
    private static final String AUDIO_TRACK_ID = "audio1";
    private static final String LOCAL_STREAM_ID = "stream1";
    private static final String SDP_MID = "sdpMid";
    private static final String SDP_M_LINE_INDEX = "sdpMLineIndex";
    private static final String SDP = "sdp";

    private PeerConnectionFactory peerConnectionFactory;
    private VideoSource localVideoSource;
    private PeerConnection peerConnection;
    private MediaStream localMediaStream;
    private VideoRenderer otherPeerRenderer;
    private Socket socket;
    private boolean createOffer = false;
    private String caller;
    private AudioManager audioManager;
    private ImageButton micButton;
    private Boolean hangup = false;
    private  VideoTrack localVideoTrack;
    private VideoCapturerAndroid vc;
    private AudioSource audioSource;
    private GLSurfaceView videoView;
    private String position;
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

        setContentView(R.layout.activity_main);
        String MY_PREFS_NAME = getResources().getString(R.string.pref_key);
        SharedPreferences pref = getSharedPreferences(MY_PREFS_NAME,MODE_PRIVATE);
        position = pref.getString("position",null);
        caller     = getIntent().getStringExtra("from");
        socket  = mySocket.getSocket();
        onConnect();
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        audioManager.setSpeakerphoneOn(true);
        micButton = (ImageButton) findViewById(R.id.mic);
        createOffer = false;
        wakeScreen();
        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true,  // Hardware Acceleration Enabled
                null); // Render EGL Context

        peerConnectionFactory = new PeerConnectionFactory();

        vc = VideoCapturerAndroid.create(VideoCapturerAndroid.getNameOfFrontFacingDevice(),null);

        localVideoSource = peerConnectionFactory.createVideoSource(vc, new MediaConstraints());
        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);
        localVideoTrack.setEnabled(true);

        audioSource = peerConnectionFactory.createAudioSource(new MediaConstraints());
        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        localAudioTrack.setEnabled(true);

        localMediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID);
        localMediaStream.addTrack(localVideoTrack);
        localMediaStream.addTrack(localAudioTrack);
        videoView = (GLSurfaceView) findViewById(R.id.glview_call);
        LocalBroadcastManager.getInstance(this).registerReceiver(hangUp, new IntentFilter("HANGUP"));

        VideoRendererGui.setView(videoView, null);
        try {
            otherPeerRenderer = VideoRendererGui.createGui(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            VideoRenderer renderer = VideoRendererGui.createGui(50, 50, 50, 50, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            localVideoTrack.addRenderer(renderer);
        } catch (Exception e) {
            e.printStackTrace();
        }    }

    @Override
    public void onActivityReenter(int resultCode, Intent data) {
        super.onActivityReenter(resultCode, data);
        if(localVideoSource != null){
            localVideoSource.restart();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        this.videoView.onPause();
        this.localVideoSource.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.videoView.onResume();
        this.localVideoSource.restart();
    }
    public void wakeScreen(){
        PowerManager pm  = (PowerManager)getSystemService(POWER_SERVICE);
        PowerManager.WakeLock screenLock = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.SCREEN_BRIGHT_WAKE_LOCK
                ,getResources().getString(R.string.action_sign_in));
        screenLock.acquire(1000);
        screenLock.release();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(hangUp);
        finish();
    }

    public void  Disconnect(View view){
       endCall(true);
    }
    public  void endCall(Boolean shouldDisconnectFromServer){
        this.localVideoSource.stop();
        socket.off("createoffer");
        socket.off("offer");
        socket.off("answer");
        socket.off("candidate");
        try{
            this.peerConnection.close();
            this.peerConnection = null;
        }
        catch(Exception ex){
            Log.d("PEER COONNECTION",ex.toString());
        }
        if(shouldDisconnectFromServer){
            JSONObject data = new JSONObject();
            try{
                data.put("deviceId",getIntent().getStringExtra("deviceId"));
                this.socket.emit("hangup",data);
            }
            catch (Exception e){

            }
        }

        Intent intent = new Intent(this,CallFragment.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    public  void toggleMic(View view){
        if(!audioManager.isMicrophoneMute()){
            micButton.setBackgroundResource(R.drawable.off);
            audioManager.setMicrophoneMute(true);
        }
        else{
            micButton.setBackgroundResource(R.drawable.on);
            audioManager.setMicrophoneMute(false);
        }
    }
    @Override
    public void onBackPressed() {

    }
    private PeerConnection createPeer(){
        if(peerConnection != null){
            return peerConnection;
        }
        ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302"));
        peerConnection = peerConnectionFactory.createPeerConnection(
                iceServers,
                new MediaConstraints(),
                peerConnectionObserver);
                peerConnection.addStream(localMediaStream);

        return peerConnection;
    }

    public void onConnect() {
                 //connect the devices
                if(position.equalsIgnoreCase("Office")){
                    socket.emit("startHandShake");
                }
                else{
                    socket.on("handShakeReceived", new Emitter.Listener() {
                        @Override
                        public void call(Object... args) {
                            socket.emit("handShakeCompleted");
                        }
                    });
                }
                socket.on("handShakeCompleted", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        socket.emit("initiateCall");
                    }
                }).on("createoffer", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    createOffer = true;
                    createPeer().createOffer(sdpObserver, new MediaConstraints());
                }

            }).on("offer", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER,
                                obj.getString(SDP));
                        createPeer().setRemoteDescription(sdpObserver, sdp);
                        createPeer().createAnswer(sdpObserver, new MediaConstraints());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on("answer", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER,
                                obj.getString(SDP));
                        createPeer().setRemoteDescription(sdpObserver, sdp);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }).on("candidate", new Emitter.Listener() {

                @Override
                public void call(Object... args) {
                    try {
                        JSONObject obj = (JSONObject) args[0];
                        createPeer().addIceCandidate(new IceCandidate(obj.getString(SDP_MID),
                                obj.getInt(SDP_M_LINE_INDEX),
                                obj.getString(SDP)));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            });
    }

    SdpObserver sdpObserver = new SdpObserver() {
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            createPeer().setLocalDescription(sdpObserver, sessionDescription);
            try {
                JSONObject obj = new JSONObject();
                obj.put(SDP, sessionDescription.description);
                if (createOffer) {
                    socket.emit("offer", obj);
                } else {
                    socket.emit("answer", obj);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSetSuccess() {


        }

        @Override
        public void onCreateFailure(String s) {
            Log.d("CREATE_ERROR",s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.d("SET_FAILURE",s);

        }
    };

    PeerConnection.Observer peerConnectionObserver = new PeerConnection.Observer() {
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d("RTCAPP", "onSignalingChange:" + signalingState.toString());
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d("RTCAPP", "onIceConnectionChange:" + iceConnectionState.toString());
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            try {
                JSONObject obj = new JSONObject();
                obj.put(SDP_MID, iceCandidate.sdpMid);
                obj.put(SDP_M_LINE_INDEX, iceCandidate.sdpMLineIndex);
                obj.put(SDP, iceCandidate.sdp);
                socket.emit("candidate", obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            mediaStream.videoTracks.getFirst().addRenderer(otherPeerRenderer);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d("RENEGOTIATION_NEEDED","11");

        }
    };

    private BroadcastReceiver hangUp = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent ) {
            if(!hangup) {
                endCall(false);
                hangup = true;
            }
        }
    };

}
