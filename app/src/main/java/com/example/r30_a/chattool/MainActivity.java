package com.microsugar.r30_a.chattool;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.r30_a.chattool.ChatMessage;
import com.example.r30_a.chattool.R;
import com.firebase.ui.auth.AuthUI;
//import com.firebase.ui.database.FirebaseListOptions;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
//import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.text.format.DateFormat;


public class MainActivity extends AppCompatActivity {

    public static final int SIGN_IN_REQUEST = 1;

    private FloatingActionButton fabButton;
    private EditText edtInput;

    //    private FirebaseListAdapter<ChatMessage> adapter;
    private FirebaseRecyclerAdapter<ChatMessage, ChatMessageHolder> adapter;
    private ListView listView;
    private RecyclerView recyclerView;
    private LinearLayoutManager linearLayoutManager;

    private DatabaseReference reference;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        reference = FirebaseDatabase.getInstance().getReference();
        
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        //檢查是否已登入，若沒登入會導頁至登入畫面
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_REQUEST);

        } else {
            Toast.makeText(this, getResources().getText(R.string.welcome) + FirebaseAuth.getInstance().getCurrentUser().getDisplayName(), Toast.LENGTH_SHORT).show();
            displayChatMsg();
        }

        //發送訊息
        fabButton = (FloatingActionButton) findViewById(R.id.fab);
        edtInput = (EditText) findViewById(R.id.edtInput);
        fabButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //客製訊息model並推送出去
                try {
                    reference.push()
                            .setValue(new ChatMessage(
                                    FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                                    edtInput.getText().toString()));

                    edtInput.setText("");
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);//送出後鍵盤收起
                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });
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
                            Toast.makeText(MainActivity.this, "sighOut", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
        }
        return true;
    }


    private void displayChatMsg() {

        try {

//            FirebaseListOptions<ChatMessage> options = new FirebaseListOptions.Builder<ChatMessage>()
//                    .setQuery(reference, ChatMessage.class)//自定model
//                    .setLifecycleOwner(this)
//                    .setLayout(R.layout.message)//自定layout
//                    .build();

//            FirebaseRecyclerOptions options = new FirebaseRecyclerOptions.Builder<ChatMessage>()
//                    .setQuery(reference, ChatMessage.class)//自定model
//                    .setLifecycleOwner(this)
////                    .setLayout(R.layout.message)//自定layout
//                    .build();

            adapter = new FirebaseRecyclerAdapter<ChatMessage, ChatMessageHolder>
                    (ChatMessage.class, R.layout.message, ChatMessageHolder.class, reference.limitToLast(10)) {

                public ChatMessageHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(MainActivity.this).inflate(R.layout.message, parent, false);
                    ChatMessageHolder holder = new ChatMessageHolder(view);
                    return holder;
                }

                @Override
                protected void populateViewHolder(ChatMessageHolder viewHolder, ChatMessage model, int position) {

                        viewHolder.setValues(model);

                }
            };


//            adapter = new FirebaseListAdapter<ChatMessage>(options) {//訊息有更新時會呼叫populateView更新清單
//                @Override
//                protected void populateView(@NonNull View v, @NonNull ChatMessage model, int position) {
//                    TextView txvUser = (TextView) v.findViewById(R.id.txv_user);
//                    TextView txvMsg = (TextView) v.findViewById(R.id.txv_msg);
//                    TextView txvTime = (TextView) v.findViewById(R.id.txv_time);
//
//                    txvMsg.setText(model.getMessage());
//                    txvUser.setText(model.getUserName());
//                    txvTime.setText(DateFormat.format("yyyy-MM-dd (HH:mm:ss)", model.getTime()));
//
//                }
//            };

            //listView = (ListView) findViewById(R.id.list_msg);
            //listView.setAdapter(adapter);
            recyclerView.setLayoutManager(linearLayoutManager);
            recyclerView.setAdapter(adapter);


        } catch (Exception e) {
            e.printStackTrace();
        }

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

    public class ChatMessageHolder extends RecyclerView.ViewHolder {
        private TextView txvUser;
        private TextView txvMsg;
        private TextView txvTime;

        public ChatMessageHolder(@NonNull View v) {
            super(v);
            txvUser = (TextView) v.findViewById(R.id.txv_user);
            txvMsg = (TextView) v.findViewById(R.id.txv_msg);
            txvTime = (TextView) v.findViewById(R.id.txv_time);

        }

        public void setValues(ChatMessage chatMessage) {
            txvUser.setText(chatMessage.getMsg_user());
            txvMsg.setText(chatMessage.getMsg_txv());
            txvTime.setText(DateFormat.format("yyyy-MM-dd (HH:mm:ss)", chatMessage.getTime()));

        }
    }


}
