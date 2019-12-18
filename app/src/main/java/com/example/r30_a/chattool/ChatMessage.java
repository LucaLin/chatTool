package com.example.r30_a.chattool;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatMessage {

    private String userName;//使用者名稱
    private String message;//訊息內容
    private long time;//發送時間
    private String uuid;

    public ChatMessage(String userName, String message, String uuid) {
        this.userName = userName;
        this.message = message;
        time = Long.getLong(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(new Date()));
        this.uuid = uuid;
    }

    public ChatMessage() {
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }
}
