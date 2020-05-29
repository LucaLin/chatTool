package com.example.r30_a.chattool;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.IOException;

public class CropImageActivity extends AppCompatActivity
        implements View.OnClickListener {

    private ScaleImageView scaleImageView;
    private ImageView imgMask;
    public final static String EXTRA_IMAGE = "extra_image";
    private int imageSize;
    private int imageCropRadius;
    private final int cropDisplayScaleRate = 4;
    int maskWidth,maskHeight;

    private Button btnOK,btnCancell;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);
        init();

        //先設定ScaleImageView為bitmap
        Bitmap bitmap = ((BitmapDrawable) scaleImageView.getDrawable()).getBitmap();
        maskWidth = bitmap.getWidth();
        maskHeight = bitmap.getHeight();
        bitmap.recycle();
        bitmap = null;

        //取得拍到的圖片或選取的圖片
        Uri uri = getIntent().getData();
        if(uri != null){

            int degree = getIntent().getIntExtra("degree",0);
            //讀取圖片
            try {

                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(),uri);

                bitmap = BitmapUtil.rotateBitmap(bitmap,degree);

                //判斷畫素是否超過限制
                if(bitmap != null){
                    //預設大小寫死在2048
                    int maxSize = 2048;

                    //取得螢幕解析度
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    //如果寬或高大於螢幕解析度，就設定其中最大的為maxsize
                    if(metrics.heightPixels > maxSize || metrics.widthPixels > maxSize){
                        maxSize = (metrics.heightPixels > metrics.widthPixels)? metrics.heightPixels : metrics.widthPixels;
                    }
                    //調整過的寬高
                    int bitmapWidth = bitmap.getWidth();
                    int bitmapHeight = bitmap.getHeight();

                    //如果螢幕範圍比maxsize大，就調整比例到一致
                    if(bitmapWidth > maxSize ||  bitmapHeight > maxSize){
                        if(bitmapHeight > bitmapWidth){
                            bitmapWidth *= (float)maxSize / bitmapHeight;
                            bitmapHeight *= (float)maxSize / bitmapHeight;
                        }else {
                            bitmapWidth *= (float)maxSize / bitmapWidth;
                            bitmapHeight *= (float)maxSize / bitmapWidth;
                        }
                        Bitmap adjustBitmap = Bitmap.createScaledBitmap(bitmap,bitmapWidth,bitmapHeight,true);
                        if(adjustBitmap != null && adjustBitmap != bitmap){
                            bitmap.recycle();
                            bitmap = adjustBitmap;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        scaleImageView.setImageBitmap(bitmap);
        //設定圈圈的size跟半徑
        imageSize = getResources().getDimensionPixelSize(R.dimen.photo_size);
        imageCropRadius = getResources().getDimensionPixelSize(R.dimen.photo_crop_circle_radius);

        Bitmap outputMask = generateMask(maskWidth,maskHeight,
                maskWidth/2, maskHeight/2,
                imageCropRadius * cropDisplayScaleRate);
        imgMask.setImageBitmap(outputMask);

    }

    public void init(){
        btnOK = findViewById(R.id.btnok);
        btnCancell = findViewById(R.id.btncancell);
        btnOK.setOnClickListener(this);
        btnCancell.setOnClickListener(this);

        scaleImageView = findViewById(R.id.imgPic);
        scaleImageView.setDrawingCacheEnabled(true);
        imgMask = findViewById(R.id.img_mask);
    }


    //此方法產生一個半透明的遮罩，然後上面有個全透明的圓圈或方形
    public Bitmap generateMask(int width, int height, int centerX, int centerY, int radius){
        //建立一個bitmap放到畫布上
        Bitmap output = Bitmap.createBitmap(width,height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final Paint paint = new Paint();
        //畫半透明背景
        canvas.drawARGB(128,0,0,0);
        //留下畫完圓圈之後，以外的半透明圖
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));//setXfermode為遮避模式，詳情可google
//        canvas.drawCircle(centerX,centerY,radius,paint);//圓形
//        Rect rect = new Rect(centerX-300,centerY-300,centerX+300,centerY+300);//方形
        Rect rect = new Rect(centerX-600,centerY-350,centerX+600,centerY+350);//方形
        canvas.drawRect(rect,paint);
        return output;

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if(id == R.id.btnok){
            Bitmap bitmap = scaleImageView.getDrawingCache(true);
            //
            Bitmap cropBitmap = BitmapUtil.cropCircleWithColor(bitmap,imageSize * cropDisplayScaleRate,
                    imageSize * 2,
                    imageCropRadius * 2,
                    Color.parseColor("#404040"));

            Intent intent = getIntent();
            String forlderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM";

            File file = new File(forlderPath);
            if(!file.exists()){
                if(file.mkdirs()){
                    File nomediaFile = new File(file,".nomedia");
                    try {
                        nomediaFile.createNewFile();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //設定圖片
            String filePath = forlderPath + File.separator + "avatar_temp" + ".png";
            BitmapUtil.bitmapToFile(cropBitmap,filePath);
            //處理好的圖片放入intent帶回
            intent.putExtra(EXTRA_IMAGE,filePath);
            setResult(RESULT_OK,intent);
            finish();

        }else if(id == R.id.btncancell){
            setResult(RESULT_CANCELED);
            finish();
        }
    }
}
