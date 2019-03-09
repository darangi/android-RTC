package com.example.upwork_video_call;

import java.util.Date;

public class chatMessages {
    chatMessages(){
        time = new Date();
    }
    public String message;
    public String recipientId;
    public String senderId;
    public Date time;
}
