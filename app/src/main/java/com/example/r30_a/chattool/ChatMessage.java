package com.example.r30_a.chattool;

import android.graphics.Bitmap;

import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatMessage {

    private String userName;//使用者名稱
    private String message;//訊息內容
    private long time;//發送時間
    private String uuid;//用來記錄裝置，區分訊息來源
    private String imgBase64;//照片的base64
    private String filePath;



    public ChatMessage(String userName, String message,long time, String uuid) {
        this.userName = userName;
        this.message = message;
        this.time = time;
        this.uuid = uuid;
    }

//    public ChatMessage(String userName, long time, String uuid, String imgBase64) {
//        this.userName = userName;
//        this.time = time;
//        this.uuid = uuid;
//        this.imgBase64 = imgBase64;
//    }



    public ChatMessage(String userName, long time, String uuid, String filePath){
        this.userName = userName;
        this.time = time;
        this.uuid = uuid;
        this.filePath = filePath;
    }


    public ChatMessage() {
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getImgBase64() {
        return imgBase64;
    }

    public void setImgBase64(String imgBase64) {
        this.imgBase64 = imgBase64;
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
