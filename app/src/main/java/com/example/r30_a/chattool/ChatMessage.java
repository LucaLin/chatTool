package com.example.r30_a.chattool;

import java.util.Date;

public class ChatMessage {

    private String userName;//使用者名稱
    private String message;//訊息內容
    private long time;//發送時間

    public ChatMessage(String userName, String message) {
        this.userName = userName;
        this.message = message;
        time = new Date().getTime();
    }

    public ChatMessage() {
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
