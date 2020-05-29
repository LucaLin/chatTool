package com.example.r30_a.chattool.Controller;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.r30_a.chattool.Model.ChatMessage;
import com.example.r30_a.chattool.R;
import com.example.r30_a.chattool.Util.BitmapUtil;
import com.example.r30_a.chattool.Util.PermissionTool;
import com.example.r30_a.chattool.Util.Utils;
import com.firebase.ui.auth.AuthUI;
//import com.firebase.ui.database.FirebaseListOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
//import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class MainActivity extends AppCompatActivity {

    // 取得結果用的 Request Code
    private final int CAMERA_REQUEST = 5001;
    private final int ALBUM_REQUEST = 5002;
    private final int AVATAR_CAMERA_REQUEST = 5003;
    private final int AVATAR_ALBUM_REQUEST = 5004;
    private final int CROP_REQUEST = 5005;
    private final int AVATAR_CROP_REQUEST = 5006;
    public static final int SIGN_IN_REQUEST = 1;
    private static final int REQUEST_CAMERA_AND_WRITE_STORAGE = 5000;
    public static final String AUTHORITY = "com.example.r30_a.fileprovider";

    private Toast toast;

    private EditText edtInput;

    private FirebaseRecyclerAdapter<ChatMessage, ChatMessageHolder> adapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;

    private DatabaseReference reference;
    private StorageReference storageReference;
    private InputMethodManager imm;
    private String uuid;

    private Uri camera_uri;
    private File cameraFile;
    private String cameraFileName;
    private String cameraPath;

    FloatingActionButton fabCamera, fabAlbum;
    private String filePath, avatarPath;

    ArrayList<String> keyList = new ArrayList<>();

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        setTitle("聊天小工具");

        setUUID();//取得裝置uuid

        findViewAndGetInstance();//綁定各種view與實體化

        checkIfLogin();//檢查是否已登入

        //訊息框
        edtInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (!TextUtils.isEmpty(edtInput.getText())) {
                    sendMsg();
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//送出後鍵盤收起
                }
            }
            return true;
        });


    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //取回user的發話keyList
        keyList = new ArrayList<>();
        Set<String> saveKeyList = new HashSet<>();
        saveKeyList = sharedPreferences.getStringSet("keyList", saveKeyList);
        for (String s : saveKeyList) {
            keyList.add(s);
        }
        avatarPath = sharedPreferences.getString("avatarPath", "");

    }

    private void findViewAndGetInstance() {
        sharedPreferences = getSharedPreferences("chatTool", MODE_PRIVATE);
        toast = Toast.makeText(MainActivity.this, "", Toast.LENGTH_LONG);
        reference = FirebaseDatabase.getInstance().getReference();
        storageReference = FirebaseStorage.getInstance().getReference();

        recyclerView = findViewById(R.id.recyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        edtInput = findViewById(R.id.edtInput);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        fabCamera = findViewById(R.id.fabCamera);
        fabAlbum = findViewById(R.id.fabAlbum);

        fabCamera.setOnClickListener(v -> fabTakePhoto(v, CAMERA_REQUEST));
        fabAlbum.setOnClickListener(v -> fabAlbum(v, ALBUM_REQUEST));

        reference.addChildEventListener(new ChildEventListener() {
            @Override//收到新訊息時自動往下捲
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if (adapter != null)
                    recyclerView.scrollToPosition(adapter.getItemCount() - 1);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void setUUID() {
        if (Build.VERSION.SDK_INT < 28) {
            uuid = Build.SERIAL;
        } else {
            uuid = Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }
    }

    //檢查是否已登入，若沒登入會導頁至登入畫面
    private void checkIfLogin() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_REQUEST);

        } else {
            toast.setText(getResources().getText(R.string.welcome) + FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
            toast.show();
            displayChatMsg();
        }
    }

    //送出訊息
    public void fabSend(View v) {
        try {
            if (!TextUtils.isEmpty(edtInput.getText())) {
                sendMsg();
                recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//送出後鍵盤收起
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //拍照
    public void fabTakePhoto(View v, int requestCode) {
        //檢查權限後拍照
        if (checkPhotoPermission()) {

            cameraFileName = Utils.getInstance().getFileName();

            File dir = Utils.getInstance().getFireDir();
            cameraFile = new File(dir, cameraFileName);
            cameraPath = cameraFile.getPath();
            if (Build.VERSION.SDK_INT >= 24) {
                camera_uri = FileProvider.getUriForFile(getApplicationContext(),
                        AUTHORITY, cameraFile);
            } else {
                camera_uri = Uri.fromFile(cameraFile);
            }

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);//使用拍照
            intent.putExtra(MediaStore.EXTRA_OUTPUT, camera_uri);
            startActivityForResult(intent, requestCode);

        } else {
            // 要求權限
            PermissionTool.getInstance()
                    .requestMultiPermission(this,
                            new String[]{
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.CAMERA
                            },
                            REQUEST_CAMERA_AND_WRITE_STORAGE);

        }

    }


    public void fabAlbum(View v, int requestCode) {//開啟相簿
        if (checkAlbumPermission()) {

            cameraFileName = Utils.getInstance().getFileName();
            File dir = Utils.getInstance().getFireDir();
            cameraFile = new File(dir, cameraFileName);
            cameraPath = cameraFile.getPath();

            Intent albumIntent = new Intent();
            albumIntent.setType("image/*");//設定只顯示圖片區，不要秀其它的資料夾
            albumIntent.setAction(Intent.ACTION_GET_CONTENT);//取得本機相簿的action
            startActivityForResult(albumIntent, requestCode);

        } else {
            PermissionTool.getInstance().requestReadExternalStoragePermission(this);
        }
    }


    private boolean checkPhotoPermission() {
        if (PermissionTool.getInstance().isWriteExternalStorageGranted(this)
                && PermissionTool.getInstance().isCameraGranted(this)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean checkAlbumPermission() {
        if (PermissionTool.getInstance().isWriteExternalStorageGranted(this)
                && PermissionTool.getInstance().isCameraGranted(this)) {
            return true;
        } else {
            return false;
        }
    }


    private void sendMsg() {
        String msg = edtInput.getText().toString();
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        long time = new Date().getTime();
        String key = reference.push().getKey();
        keyList.add(key);
        if (TextUtils.isEmpty(avatarPath))
            avatarPath = "";
        reference.child(key).setValue(new ChatMessage(userName, msg, time, uuid, "", avatarPath));
        //{
        //  "-M8TvppfDc_JZulwYR9e" : {
        //    "avatarPath" : "20200529134748-344.jpg",
        //    "filePath" : "",
        //    "message" : "jrd",
        //    "time" : 1590730652270,
        //    "userName" : "Luca Lin",
        //    "uuid" : "GAGQRKIVUGRSKJQO"
        //  },

        edtInput.setText("");
        Set<String> saveKeyList = new HashSet<>();
        for (int i = 0; i < keyList.size(); i++) {
            saveKeyList.add(keyList.get(i));
        }
        sharedPreferences.edit().putStringSet("keyList", saveKeyList).commit();

    }

    @Override
    protected void onStart() {
        super.onStart();

//        adapter.startListening();//啟動監聽，訊息可即時更新

    }

    @Override
    protected void onStop() {
        super.onStop();
//        adapter.stopListening();
    }

    //設置登出詢問
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    //設置登出動作
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_sign_out) {

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false)
                    .setTitle("登出")
                    .setMessage("確定要登出了嗎？")
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            AuthUI.getInstance().signOut(MainActivity.this)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            toast.setText("已登出囉！");
                                            toast.show();
                                            finish();
                                        }
                                    });
                        }
                    }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            }).create();
            builder.show();

        }
        return true;
    }

    //秀出訊息
    private void displayChatMsg() {

        try {
            adapter = new FirebaseRecyclerAdapter<ChatMessage, ChatMessageHolder>
                    (ChatMessage.class, R.layout.message, ChatMessageHolder.class, reference.limitToLast(10)) {

                public ChatMessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.message, parent, false);
                    ChatMessageHolder holder = new ChatMessageHolder(view);

                    return holder;
                }


                @Override
                protected void populateViewHolder(ChatMessageHolder viewHolder, ChatMessage model, final int position) {
                    viewHolder.setValues(model);
                    viewHolder.img_avatar_other.setOnClickListener(v -> showInfo(position));
                    viewHolder.img_avatar_user.setOnClickListener(v -> showInfo(position));

                }
            };

            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setHasFixedSize(true);
            recyclerView.setAdapter(adapter);
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void showInfo(int position) {
        final Dialog dialog = new Dialog(MainActivity.this, R.style.edit_AlertDialog_style);
        dialog.setContentView(R.layout.dialog_avatar_info);
        dialog.setCanceledOnTouchOutside(false);

        final ImageView img_avatar = dialog.findViewById(R.id.img_info_avatar);
        ImageView img_close = dialog.findViewById(R.id.img_close);
        ImageView img_changeAvatar = dialog.findViewById(R.id.img_changeAvatar);
        TextView txv_name = dialog.findViewById(R.id.txv_dialog_name);

        final ChatMessage data = adapter.getItem(position);
        txv_name.setText(data.getUserName());

        img_close.setOnClickListener(v -> dialog.dismiss());

        if (!TextUtils.isEmpty(data.getAvatarPath())) {
            storageReference = FirebaseStorage.getInstance().getReference();
            storageReference = storageReference.child(data.getAvatarPath());
            storageReference.getDownloadUrl().addOnSuccessListener(uri -> Glide.with(MainActivity.this)
                    .load(uri)
                    .into(img_avatar)).addOnFailureListener(e -> e.printStackTrace());
        }

        if (data.getUuid().equals(uuid)) {//如果是user才問要不要換
            img_changeAvatar.setVisibility(View.VISIBLE);
        }
        img_changeAvatar.setOnClickListener(v -> {

            ArrayList<String> typeList = new ArrayList<>();
            typeList.add("拍照");
            typeList.add("從相簿選");

            View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.popup_window, null, false);
            ListView listView = view.findViewById(R.id.type_listview);
            ArrayAdapter<String> nameAdapter = new ArrayAdapter<>(MainActivity.this,
                    android.R.layout.simple_list_item_1,
                    typeList);
            listView.setAdapter(nameAdapter);

            PopupWindow popupWindow = new PopupWindow(view, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
            popupWindow.setBackgroundDrawable(getDrawable(android.R.color.white));
            popupWindow.setTouchable(true);
            popupWindow.showAsDropDown(v, 0, 0);
            popupWindow.setContentView(view);

            listView.setOnItemClickListener((parent, view1, position1, id) -> {
                switch (position1) {
                    case 0: //拍照
                        fabTakePhoto(view1, AVATAR_CAMERA_REQUEST);
                        dialog.dismiss();
                        break;
                    case 1://從相簿選
                        fabAlbum(view1, AVATAR_ALBUM_REQUEST);
                        dialog.dismiss();
                        break;
                }
            });
        });

        dialog.show();

    }

    @Override//獲取登入結果
    protected void onActivityResult(final int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_REQUEST) {
            if (resultCode == RESULT_OK) {
                toast.setText(getResources().getText(R.string.loginSuccess));
                toast.show();
                displayChatMsg();
            } else {
                toast.setText(getResources().getText(R.string.loginFail));
                toast.show();
                finish();
            }
        } else if (requestCode == CAMERA_REQUEST || requestCode == AVATAR_CAMERA_REQUEST) {//獲取拍照結果
            if (resultCode == RESULT_OK) {
                try {
                    //https://www.itread01.com/content/1547700324.html
                    //https://givemepass.blogspot.com/2017/03/firebase-storage.html
                    File tempFile = getCacheDir();

                    final Uri uri = Uri.fromFile(Utils.getInstance().compressUploadPhoto(tempFile, cameraPath, cameraFileName));
                    doCropPhoto(uri, 0, requestCode);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                toast.setText("請重試一次");
                toast.show();
            }

        } else if (requestCode == ALBUM_REQUEST || requestCode == AVATAR_ALBUM_REQUEST) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                doCropPhoto(data.getData(), 0, requestCode);

            } else {
                toast.setText("請重試一次");
                toast.show();
            }
        } else if (requestCode == AVATAR_CROP_REQUEST || requestCode == CROP_REQUEST) {
            if (data.hasExtra(CropImageActivity.EXTRA_IMAGE) && data != null) {
                //取得裁切後圖片的暫存位置
                String filePath = data.getStringExtra(CropImageActivity.EXTRA_IMAGE);
                if (filePath.indexOf(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM") != -1) {
                    File imgFile = new File(filePath);
                    if (imgFile.exists()) {
                        //代入已設定好的圖片size
                        int photoSize = getResources().getDimensionPixelSize(R.dimen.photo_size);
                        //使用寫好的方法將路徑檔做成bitmap檔
                        Bitmap realBitmap = BitmapUtil.decodeSampledBitmap(imgFile.getAbsolutePath(), photoSize, photoSize);
                        File file = Utils.getInstance().bitmapToFile(getCacheDir(), cameraFileName, realBitmap);

                        if (requestCode == AVATAR_CROP_REQUEST) {//改大頭貼的動作才換路徑
                            avatarPath = file.getName();
                            sharedPreferences.edit().putString("avatarPath", avatarPath).commit();
                        }

                        uploadFile(Uri.fromFile(file), file.getName(), requestCode);
                    }
                }
            }
        }
    }

    private void doCropPhoto(Uri uri, int degree, int requestCode) {
        Intent intent = new Intent(MainActivity.this, CropImageActivity.class);
        intent.setData(uri);
        intent.putExtra("degree", degree);
        if (requestCode == ALBUM_REQUEST || requestCode == CAMERA_REQUEST) {
            startActivityForResult(intent, CROP_REQUEST);
        } else if (requestCode == AVATAR_ALBUM_REQUEST || requestCode == AVATAR_CAMERA_REQUEST) {
            startActivityForResult(intent, AVATAR_CROP_REQUEST);
        }
    }

    //上傳圖片訊息至firebase雲端
    private void uploadFile(Uri uri, final String fileName, final int requestCode) {

        storageReference = FirebaseStorage.getInstance().getReference();
        storageReference = storageReference.child(fileName);

        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpg")
                .build();

        UploadTask uploadTask = storageReference.putFile(uri, metadata);
        uploadTask.addOnSuccessListener(taskSnapshot -> {
            String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
            long time = new Date().getTime();

            if (requestCode == CAMERA_REQUEST || requestCode == ALBUM_REQUEST || requestCode == CROP_REQUEST) {//上傳圖片
                String key = reference.push().getKey();
                keyList.add(key);
                reference.child(key).setValue(new ChatMessage(userName, "", time, uuid, fileName, avatarPath));
                Set<String> saveKeyList = new HashSet<>();
                for (int i = 0; i < keyList.size(); i++) {
                    saveKeyList.add(keyList.get(i));
                }
                sharedPreferences.edit().putStringSet("keyList", saveKeyList).commit();

            } else if (requestCode == AVATAR_CROP_REQUEST) {//更新大頭貼

                Map<String, Object> map = new HashMap<>();
                for (int i = 0; i < keyList.size(); i++) {
                    map.put(keyList.get(i) + "/avatarPath", fileName);
                }
                reference.updateChildren(map);
                sharedPreferences.edit().putString("avatarPath", fileName).commit();

            }


        }).addOnFailureListener(e -> e.printStackTrace());
    }

    //處理訊息的地方
    public class ChatMessageHolder extends RecyclerView.ViewHolder {
        private TextView txvUser_Other;
        private TextView txvMsg_Other;
        private TextView txvTime_Other;

        private TextView txvMsg_User;
        private TextView txvTime_User;
        private TextView txv_time_imgOther;
        private ImageView img_avatar_other, img_avatar_user;

        private TextView txv_time_imgUSer;
        RelativeLayout userLayout, otherUserLayout;

        ImageView imgMsg_user, imgMsg_other;


        public ChatMessageHolder(@NonNull View v) {
            super(v);
            txvUser_Other = v.findViewById(R.id.txv_user_other);
            txvMsg_Other = v.findViewById(R.id.txv_msg_other);
            txvTime_Other = v.findViewById(R.id.txv_time_other);

            txvMsg_User = v.findViewById(R.id.txv_msg_user);
            txvTime_User = v.findViewById(R.id.txv_time_user);

            userLayout = v.findViewById(R.id.userLayout);
            otherUserLayout = v.findViewById(R.id.otherUserLayout);
            img_avatar_other = v.findViewById(R.id.img_avatar_other);
            img_avatar_user = v.findViewById(R.id.img_avatar_user);

            imgMsg_user = v.findViewById(R.id.imgmsg_user);
            imgMsg_other = v.findViewById(R.id.imgmsg_otheruser);

            txv_time_imgUSer = v.findViewById(R.id.txv_time_imgUSer);
            txv_time_imgOther = v.findViewById(R.id.txv_time_imgOther);

        }

        public void setValues(final ChatMessage chatMessage) {
            if (chatMessage != null) {

                String sendTime = new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(chatMessage.getTime());
                String filePath = chatMessage.getFilePath();
                String avatarPath = chatMessage.getAvatarPath();
                String chatMsg = chatMessage.getMessage();
                if (chatMsg == null) {
                    otherUserLayout.setVisibility(View.GONE);
                    userLayout.setVisibility(View.GONE);
                } else {
                    if (!chatMessage.getUuid().equals(uuid)) {//使用裝置id讓判斷訊息來自使用者或對方
                        otherUserLayout.setVisibility(View.VISIBLE);
                        userLayout.setVisibility(View.GONE);

                        if (!TextUtils.isEmpty(avatarPath)) {
                            storageReference = FirebaseStorage.getInstance().getReference();
                            storageReference = storageReference.child(avatarPath);
                            storageReference.getDownloadUrl().addOnSuccessListener(uri -> Glide.with(MainActivity.this)
                                    .load(uri)
                                    .into(img_avatar_other)).addOnFailureListener(e -> e.printStackTrace());
                        }

                        if (TextUtils.isEmpty(filePath)) {//如果有圖片訊息就秀圖
                            txvMsg_Other.setVisibility(View.VISIBLE);
                            imgMsg_other.setVisibility(View.GONE);
                            txv_time_imgOther.setVisibility(View.GONE);

                            txvMsg_Other.setText(chatMsg);
                            txvTime_Other.setText(sendTime);
                        } else {
                            txvMsg_Other.setVisibility(View.GONE);
                            imgMsg_other.setVisibility(View.VISIBLE);

                            txvTime_Other.setVisibility(View.GONE);
                            txv_time_imgOther.setVisibility(View.VISIBLE);

                            storageReference = FirebaseStorage.getInstance().getReference();
                            storageReference = storageReference.child(filePath);
                            //讀取圖片
                            storageReference.getDownloadUrl().addOnSuccessListener(uri -> Glide.with(MainActivity.this)
                                    .load(uri)
                                    .into(imgMsg_other)).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    e.printStackTrace();
                                    toast.setText("請重新讀取");
                                    toast.show();
                                }
                            });
                            txv_time_imgOther.setText(sendTime);
                            imgMsg_other.setOnClickListener(v -> showPhoto(chatMessage));
                        }

                        txvUser_Other.setText(chatMessage.getUserName());

                    } else {//自己
                        userLayout.setVisibility(View.VISIBLE);
                        otherUserLayout.setVisibility(View.GONE);

                        if (!TextUtils.isEmpty(avatarPath)) {
                            storageReference = FirebaseStorage.getInstance().getReference();
                            storageReference = storageReference.child(avatarPath);
                            storageReference.getDownloadUrl().addOnSuccessListener(uri -> Glide.with(MainActivity.this)
                                    .load(uri)
                                    .into(img_avatar_user)).addOnFailureListener(e -> e.printStackTrace());
                        }

                        if (TextUtils.isEmpty(filePath)) {//如果有圖片訊息就秀圖
                            txvMsg_User.setVisibility(View.VISIBLE);
                            txvMsg_User.setText(chatMsg);
                            imgMsg_user.setVisibility(View.GONE);

                            txv_time_imgUSer.setVisibility(View.GONE);
                            txvTime_User.setVisibility(View.VISIBLE);
                            txvTime_User.setText(sendTime);

                        } else {

                            txvMsg_User.setVisibility(View.GONE);
                            imgMsg_user.setVisibility(View.VISIBLE);

                            storageReference = FirebaseStorage.getInstance().getReference();
                            storageReference = storageReference.child(filePath);
                            storageReference.getDownloadUrl().addOnSuccessListener(uri -> Glide.with(MainActivity.this)
                                    .load(uri)
                                    .into(imgMsg_user)).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    e.printStackTrace();
                                }
                            });

                            txv_time_imgUSer.setVisibility(View.VISIBLE);
                            txvTime_User.setVisibility(View.GONE);
                            txv_time_imgUSer.setText(sendTime);

                            imgMsg_user.setOnClickListener(v -> showPhoto(chatMessage));
                        }


                    }

                }
            }
        }

        public void showPhoto(ChatMessage chatMessage) {
            Dialog dialog = new Dialog(MainActivity.this, R.style.edit_AlertDialog_style);
            dialog.setContentView(R.layout.dialog_photo);
            final ImageView img_photo = dialog.findViewById(R.id.img_dialog_photo);
            storageReference = FirebaseStorage.getInstance().getReference();
            storageReference = storageReference.child(chatMessage.getFilePath());
            storageReference.getDownloadUrl().addOnSuccessListener(uri -> Glide.with(MainActivity.this)
                    .load(uri)
                    .into(img_photo));

            dialog.setCanceledOnTouchOutside(true);
            dialog.show();
        }
    }

}
