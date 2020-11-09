# chatTool
使用firebase技術做出的一個聊天室小工具，並有以下特點

．使用GMail即可登入
．使用人數無上限
．可更換自己的大頭貼
．即時的通訊功能，可輸入文字、圖片訊息

![image](https://github.com/LucaLin/chatTool/blob/master/screenshots/1.PNG)

一、使用前需先至Firebase平台註冊登入，網址為：https://console.firebase.google.com

![image](https://github.com/LucaLin/chatTool/blob/master/screenshots/2.png)



二、將Firebase工具導入gradle中

    implementation 'com.android.support:design:28.0.0'
    implementation 'com.firebaseui:firebase-ui:1.1.1'

    implementation 'com.google.firebase:firebase-database:17.0.0'
    implementation 'com.google.firebase:firebase-core:16.0.9'
    implementation 'com.google.firebase:firebase-auth:16.0.1'

    // FirebaseUI for Cloud Firestore
    implementation 'com.firebaseui:firebase-ui-firestore:5.0.0'
    // FirebaseUI for Firebase Auth
    implementation 'com.firebaseui:firebase-ui-auth:5.0.0'
    // FirebaseUI for Cloud Storage
    implementation 'com.firebaseui:firebase-ui-storage:5.0.0'

三、登入動作

    MainActivity.java
    
    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(), SIGN_IN_REQUEST);
    } 

使用AuthUI工具取得實體後，建立SignIn動作並回傳結果至Activity中

![image](https://github.com/LucaLin/chatTool/blob/master/screenshots/3.PNG)

四、登出動作

    AuthUI.getInstance().signOut(this)
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            Toast.makeText(MainActivity.this, "sighOut", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
  
一樣使用AuthUI工具取得實體，呼叫SignOut方法，並加上listener完成登出後的動作

![image](https://github.com/LucaLin/chatTool/blob/master/screenshots/4.PNG)

五、發送消息至平台

1、 取得FirebaseDatabase實體

    reference = FirebaseDatabase.getInstance().getReference();
    
2、自定義model

    public class ChatMessage {

    private String userName;//使用者名稱
    private String message;//訊息內容
    private long time;//發送時間

    public ChatMessage(String userName, String message) {
        this.userName = userName;
        this.message = message;
        time = new Date().getTime();
    }
    
3、 發送消息

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
                
![image](https://github.com/LucaLin/chatTool/blob/master/screenshots/5.PNG)
                
六、接收Firebase平台訊息

1、建立ChatMessageViewHolder

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
            txvUser.setText(chatMessage.getUserName());
            txvMsg.setText(chatMessage.getMessage());
            txvTime.setText(DateFormat.format("yyyy-MM-dd (HH:mm:ss)", chatMessage.getTime()));

        }
    }
    
2' 建立FirebaseRecyclerAdapter

displayChatMsg()中

        try {
            
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

            
![image](https://github.com/LucaLin/chatTool/blob/master/screenshots/6.PNG)


ps：reference.limitToLast(10) → 抓取Firebase的最新10筆資料

七、設置adapter

    recyclerView.setAdapter(adapter);
