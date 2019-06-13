package com.example.r30_a.chattool;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
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

    private FirebaseListAdapter<ChatMessage> adapter;
    private ListView listView;

    private DatabaseReference reference;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        reference = FirebaseDatabase.getInstance().getReference();
        imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
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
                reference.push()
                         .setValue(new ChatMessage(
                                 FirebaseAuth.getInstance().getCurrentUser().getDisplayName(),
                                 edtInput.getText().toString()));

                edtInput.setText("");
                imm.hideSoftInputFromWindow(v.getWindowToken(),0);//送出後鍵盤收起

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();//啟動監聽，訊息可即時更新
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
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

            FirebaseListOptions<ChatMessage> options = new FirebaseListOptions.Builder<ChatMessage>()
                    .setQuery(reference, ChatMessage.class)//自定model
                    .setLifecycleOwner(this)
                    .setLayout(R.layout.message)//自定layout
                    .build();
            adapter = new FirebaseListAdapter<ChatMessage>(options) {//訊息有更新時會呼叫populateView更新清單
                @Override
                protected void populateView(@NonNull View v, @NonNull ChatMessage model, int position) {
                    TextView txvUser = (TextView) v.findViewById(R.id.txv_user);
                    TextView txvMsg = (TextView) v.findViewById(R.id.txv_msg);
                    TextView txvTime = (TextView) v.findViewById(R.id.txv_time);

                    txvMsg.setText(model.getMsg_txv());
                    txvUser.setText(model.getMsg_user());
                    txvTime.setText(DateFormat.format("yyyy-MM-dd (HH:mm:ss)", model.getTime()));

                }
            };

            listView = (ListView) findViewById(R.id.list_msg);
            listView.setAdapter(adapter);

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
}
