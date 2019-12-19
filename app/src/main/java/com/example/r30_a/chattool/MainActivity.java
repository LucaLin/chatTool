package com.example.r30_a.chattool;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
//import com.firebase.ui.database.FirebaseListOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
//import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    public static final int SIGN_IN_REQUEST = 1;


    private EditText edtInput;

    //    private FirebaseListAdapter<ChatMessage> adapter;
    private FirebaseRecyclerAdapter<ChatMessage, ChatMessageHolder> adapter;
    private ListView listView;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;

    private DatabaseReference reference;
    private InputMethodManager imm;
    String uuid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("發哀儿貝斯聊天室");

        setUUID();//取得裝置uuid

        findViewAndGetInstance();

        checkIfLogin();//檢查是否已登入

        //訊息框
        edtInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (!TextUtils.isEmpty(edtInput.getText())) {
                        sendMsg();
                        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//送出後鍵盤收起
                    }
                }
                return true;
            }
        });


    }

    private void findViewAndGetInstance() {
        reference = FirebaseDatabase.getInstance().getReference();
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        edtInput = (EditText) findViewById(R.id.edtInput);
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        reference.addChildEventListener(new ChildEventListener() {
            @Override//收到新訊息時自動往下捲
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
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
            Toast.makeText(this, getResources().getText(R.string.welcome) + FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), Toast.LENGTH_SHORT).show();
            displayChatMsg();
        }
    }

    //送出訊息
    public void fabClick(View v) {
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

    private void sendMsg() {
        String msg = edtInput.getText().toString();
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        long time = new Date().getTime();
        reference.push()
                .setValue(new ChatMessage(userName, msg, time, uuid));

        edtInput.setText("");


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
            AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MainActivity.this, "已登出囉！", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
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
                    viewHolder.img_avatar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            showInfo(position);
                        }
                    });

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
        AlertDialog builder = new AlertDialog.Builder(this).create();
        builder.getWindow().setBackgroundDrawable(new ColorDrawable(0));

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_avatar_info,null);
        TextView txvName = dialogView.findViewById(R.id.txv_dialog_name);
        ChatMessage data = adapter.getItem(position);
        txvName.setText(data.getUserName());
//        LinearLayout.LayoutParams pm = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//        LinearLayout linearLayout = new LinearLayout(this);
//        linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);
//        linearLayout.addView(dialogView,pm);

        builder.setView(dialogView);
//        builder.create();

        builder.show();

    }

    @Override//獲取登入結果
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SIGN_IN_REQUEST) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, getResources().getText(R.string.loginSuccess), Toast.LENGTH_SHORT).show();
                displayChatMsg();
            } else {
                Toast.makeText(this, getResources().getText(R.string.loginFail), Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }


    //處理訊息的地方
    public class ChatMessageHolder extends RecyclerView.ViewHolder  {
        private TextView txvUser_Other;
        private TextView txvMsg_Other;
        private TextView txvTime_Other;

        private TextView txvMsg_User;
        private TextView txvTime_User;

        private ImageView img_avatar;

        RelativeLayout userLayout, otherUserLayout;

        public ChatMessageHolder(@NonNull View v) {
            super(v);
            txvUser_Other = (TextView) v.findViewById(R.id.txv_user_other);
            txvMsg_Other = (TextView) v.findViewById(R.id.txv_msg_other);
            txvTime_Other = (TextView) v.findViewById(R.id.txv_time_other);

            txvMsg_User = (TextView) v.findViewById(R.id.txv_msg_user);
            txvTime_User = (TextView) v.findViewById(R.id.txv_time_user);

            userLayout = (RelativeLayout) v.findViewById(R.id.userLayout);
            otherUserLayout = (RelativeLayout) v.findViewById(R.id.otherUserLayout);
            img_avatar = (ImageView)v.findViewById(R.id.img_avatar);

        }

        public void setValues(ChatMessage chatMessage) {
            if (!chatMessage.getUuid().equals(uuid)) {

                otherUserLayout.setVisibility(View.VISIBLE);
                userLayout.setVisibility(View.GONE);

                txvUser_Other.setText(chatMessage.getUserName());
                txvMsg_Other.setText(chatMessage.getMessage());
                txvTime_Other.setText(String.valueOf(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(chatMessage.getTime())));


            } else {
                userLayout.setVisibility(View.VISIBLE);
                otherUserLayout.setVisibility(View.GONE);

                txvMsg_User.setText(chatMessage.getMessage());
                txvTime_User.setText(String.valueOf(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(chatMessage.getTime())));
            }

        }
    }


}
