package com.example.upwork_video_call;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.ArrayList;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static android.content.Context.MODE_PRIVATE;

public class ReceptionsFragment extends Fragment {
    Socket socket;
    JSONArray onlineReceptions;
    ListView list;
    TextView receptionText;
    final int OFFICE = 1;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        socket  = mySocket.getSocket();


    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.receptions, container, false);
    }

    @SuppressLint("InlinedApi")
    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState)
    {
        list = view.findViewById(R.id.list);
        receptionText = view.findViewById(R.id.noReceptions);
        list.setClickable(true);
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
                    if(isAdded()) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                    }
                }
                catch (Exception e){

                }
            }
        });
        socket.emit("getReceptions");
    }
    public void setReceptions(final ArrayList<dataModel> receptions){
        if(isAdded()) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (receptions.size() > 0) {
                        receptionText.setVisibility(View.GONE);
                        list.setVisibility(View.VISIBLE);
                        CustomAdapter adapter = new CustomAdapter(receptions, getContext(), getActivity());
                        list.setAdapter(adapter);
                    }
                }
            });
        }
    }
}
