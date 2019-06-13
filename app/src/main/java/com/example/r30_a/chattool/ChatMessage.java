package com.example.r30_a.chattool;

import java.util.Date;

public class ChatMessage {

    private String msg_user;
    private String msg_txv;
    private long time;

    public ChatMessage(String msg_user, String msg_txv) {
        this.msg_user = msg_user;
        this.msg_txv = msg_txv;
        time = new Date().getTime();
    }

    public ChatMessage() {
    }

    public String getMsg_user() {
        return msg_user;
    }

    public void setMsg_user(String msg_user) {
        this.msg_user = msg_user;
    }

    public String getMsg_txv() {
        return msg_txv;
    }

    public void setMsg_txv(String msg_txv) {
        this.msg_txv = msg_txv;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
