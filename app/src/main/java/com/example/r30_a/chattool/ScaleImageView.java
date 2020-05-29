package com.example.r30_a.chattool;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

public class ScaleImageView extends android.support.v7.widget.AppCompatImageView{

    //初始狀態的Matrix & 變動狀態中的Matrix
    private Matrix matrix = new Matrix();
    private Matrix changeMatrix = new Matrix();
    //圖片的bitmap
    private Bitmap bitmap = null;
    //手機畫面尺寸資訊
    private DisplayMetrics displayMetrics;
    //最小縮放值 &  最大縮放值
    private float minScale = 0.1f;
    private float maxScale = 5.0f;
    //圖片的三種狀態：0→初始狀態，1→拖動狀態，2→縮放狀態
    private static final int STATE_NONE = 0;
    private static final int STATE_DRAG = 1;
    private static final int STATE_ZOOM = 2;

    //預設的當下狀態
    private int nowState = STATE_NONE;
    //按下的第一點座標與第二點座標
    private PointF p1 = new PointF();
    private PointF p2 = new PointF();
    //兩點之間的距離
    private float distance = 1f;
    //圖片中心的坐標
    private float centerX,centerY;


    public ScaleImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        //取得圖片
        BitmapDrawable bitmapDrawable = (BitmapDrawable)this.getDrawable();
        if(bitmapDrawable != null){
            bitmap = bitmapDrawable.getBitmap();
            buildImage();
        }
    }

    //圖片縮放設定
    @SuppressLint("ClickableViewAccessibility")
    private void buildImage() {
        //取得手機尺寸
        Context context = getContext();
        displayMetrics = context.getResources().getDisplayMetrics();
        //縮放的型態
        this.setScaleType(ScaleType.MATRIX);
        //代入bitmap
        this.setImageBitmap(bitmap);

        //將圖片放在畫面中央
        centerX = (float)(displayMetrics.widthPixels/2)-(bitmap.getWidth()/2);
        centerY = (float)(displayMetrics.heightPixels/2)-(bitmap.getHeight()/2);
        matrix.postTranslate(centerX,centerY);//設定縮放起點
        this.setImageMatrix(matrix);

        this.setOnTouchListener((v, event) -> {

            //進行多點偵測
            switch (event.getAction() & MotionEvent.ACTION_MASK){

                //一點觸碰時
                case MotionEvent.ACTION_DOWN:
                    changeMatrix.set(matrix);
                    p1.set(event.getX(),event.getY());
                    nowState = STATE_DRAG;
                    break;

                //兩點觸碰時
                case MotionEvent.ACTION_POINTER_DOWN:
                    distance = getSpace(event);

                    //距離大於指定距離時才判斷為兩點觸碰, 並設定為縮放狀態
                    if(distance > 10f){
                        changeMatrix.set(matrix);
                        getMiddlePoint(p2,event);
                        nowState = STATE_ZOOM;
                    }
                    break;
                //兩點都離開觸碰時
                case MotionEvent.ACTION_UP:break;

                //有一點離開觸碰時，狀態恢復
                case MotionEvent.ACTION_POINTER_UP:
                    nowState = STATE_NONE;
                    break;

                case MotionEvent.ACTION_MOVE:
                    if(nowState == STATE_DRAG){
                        matrix.set(changeMatrix);
                        matrix.postTranslate(event.getX() - p1.x,
                                event.getY() - p1.y);
                    }else if (nowState == STATE_ZOOM){
                        float newDistance = getSpace(event);
                        if(newDistance > 10f){
                            matrix.set(changeMatrix);
                            float newScale = newDistance / distance;
                            matrix.postScale(newScale,newScale,p2.x,p2.y);
                        }
                    }
                    break;
            }
            ScaleImageView.this.setImageMatrix(matrix);

            scale();

            return true;
        });

    }

    //圖片縮放層級設定
    private void scale() {
        float level[] = new float[9];
        matrix.getValues(level);

        //縮放狀態時才進入
        if(nowState == STATE_ZOOM){
            //設定層級範圍為自定義的最小最大層級
            if(level[0] < minScale){
                matrix.set(changeMatrix);
            }
            if(level[0] > maxScale){
                matrix.set(changeMatrix);
            }
        }
    }

    //取得兩觸碰點的中心
    private void getMiddlePoint(PointF pointF , MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        pointF.set(x/2 , y/2);
    }

    //取得兩觸碰點之間的直線距離
    private float getSpace(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt((x * x) + (y * y));

    }
}
