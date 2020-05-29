package com.example.r30_a.chattool;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class BitmapUtil {

    //計算圖片需要的寬高以符合螢幕大小，將所需的寬高傳入，使用option參數來調整
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqwidth, int reqheight){
        //圖片原始的寬高
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;//預設比例是1

        if(height > reqheight || width > reqwidth){

            //計算所需寬高的比例, 用Math.round取四捨五入到整數
            final int heightRatio = Math.round((float)height / (float) reqheight);
            final int widthRatio = Math.round((float)width / (float) reqwidth);

            //取一個最小的比例當做要傳回的比例，寬或高
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;

            //PS:如有全景圖(必然大於螢幕的，一樣將所需的面積代入一個totalpixels，以免計憶體爆掉
            final float totalPixels = width * height;

            //超過需求兩倍大的像素圖也做處理
            final float totalReqPixelsCap = reqheight * reqwidth * 2;
            //只要超過要傳回的比例，處理的圖比例就往上+，直到大於它所需的寬高為止
            while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap ){
                inSampleSize ++;
            }
        }
        return inSampleSize;
    }

    //1: 讀取指定路徑的檔案，製作符合寬高的圖

    public static Bitmap decodeSampledBitmap(String filePath, int width, int height){
        File file = new File(filePath);

        if(file.exists()){
            //使用bitmap option設定圖片參數
            BitmapFactory.Options options = new BitmapFactory.Options();

            options.inJustDecodeBounds = true;//設定BitmapFactory.decodeStream只抓原始圖的長寬
            BitmapFactory.decodeFile(filePath,options);//抓該圖片的長寬

            options.inPreferredConfig = Bitmap.Config.ARGB_8888;//圖片品質
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inSampleSize = BitmapUtil.calculateInSampleSize(options, width, height);
            options.inJustDecodeBounds = false;

            return BitmapFactory.decodeFile(filePath,options);
        }else
            return null;
    }

    //2: 將bitmap轉成一個暫時檔

    public static void bitmapToFile(Bitmap bitmap, String filePath){
        //如果讀不到裝置有資料，不做任何事
        if(filePath.indexOf(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "DCIM")
                == -1){
            return;
        }

        FileOutputStream outputStream = null;
        File file = new File(filePath);
        if(file.exists()){
            file.delete();
        }

        try {
            outputStream = new FileOutputStream(filePath);
            bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);//取出的檔轉成png格式，品質是100%,代表不壓縮
        } catch (Exception e) {
            e.printStackTrace();
        }finally {

            if(outputStream != null){
                try {
                    outputStream.close();//做存檔動作
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //3: bitmap轉成base64字串
    public static String bitmapToBase64(Bitmap bitmap){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG,100,byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.NO_WRAP);//字串不換行
    }

    //3.1: url轉base64
//    public String urlToBase64()

    //4: base64字串轉成bitmap
    public static Bitmap base64ToBitmap(String base64String){
        try {
            //中間如果有逗點值，取從逗點值後的值開始
            if(base64String.indexOf(",") != -1){
                base64String = base64String.substring(base64String.indexOf(",")+1);
            }

            byte[] imageAsBytes = Base64.decode(base64String.getBytes(), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(imageAsBytes,0,imageAsBytes.length);//從頭到尾轉成bytearray

        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    //5: url取到的圖轉成bitmap
    public static DownloadImageTask task;
    public static void setBackgroundFromUrl(String urlString, DownloadImageTask.OnDownloadingListner listner){
        task = new DownloadImageTask(urlString,listner);
        task.execute();
    }
    public static void DownlodImageTaskClose(){
        if(task != null){
            task.cancel(true);
        }
    }

    //5-1 使用AsyncTask下載url圖片
    public static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private String urlString;
        public OnDownloadingListner onDownloadingListner;
        public interface OnDownloadingListner{
            void OnFinish(Bitmap bitmap);
        }

        public DownloadImageTask(String urlString, OnDownloadingListner onDownloadingListner){
            this.urlString = urlString;
            this.onDownloadingListner = onDownloadingListner;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {
                //丟入網址開啟下載
                InputStream inputStream = new URL(urlString).openStream();
                return BitmapFactory.decodeStream(inputStream);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }
        }
        protected void OnPostExecute(Bitmap result){
            //下載完後結束下載並載入結果
            if(onDownloadingListner != null){
                if(result != null){
                    onDownloadingListner.OnFinish(result);
                }
            }
        }
    }

    //6-1: bitmap中心裁切一個圓，其餘部分透明
    public static Bitmap cropTheBitmap(Bitmap bitmap, int rectSizeIn, int recSizeOut, int radius){
        final Paint paint = new Paint();
        paint.setAntiAlias(true);

        return cropTheBitmap(bitmap,rectSizeIn,recSizeOut,radius,paint, PorterDuff.Mode.SRC_IN);//只取中間的圓
    }

    //6-2: bitmap中心裁切一個圓，其餘部份指定顏色
    public static Bitmap cropCircleWithColor(Bitmap bitmap, int rectSizeIn, int rectSizeOut, int radius, int color){
        final Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(color);
        Bitmap cropBitmap = cropTheBitmap(bitmap,rectSizeIn,rectSizeOut,radius,paint,PorterDuff.Mode.SRC_ATOP);
        bitmap.recycle();
        return cropBitmap;
    }

    //圖片裁切成圓形的方法, PS:porterduff為圖片渲染模式
    private static Bitmap cropTheBitmap(Bitmap bitmap, int rectSizeIn, int rectSizeOut, int radius, Paint paint, PorterDuff.Mode mode){
        //建立一張空白的圖，長寬由導入的size決定
        Bitmap outputBitmap = Bitmap.createBitmap(rectSizeOut,rectSizeOut, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(outputBitmap);

        //畫出一個圓
        //canvas.drawCircle(rectSizeOut/2, rectSizeOut/2, radius, paint);
        //畫出一個方形
        Rect rect = new Rect(rectSizeOut/8,rectSizeOut/8,rectSizeOut-(rectSizeOut/8),rectSizeOut-(rectSizeOut/8));
        canvas.drawRect(rect,paint);
        //設定留下來部份是bitmap裁切後的圓形部份
        paint.setXfermode(new PorterDuffXfermode(mode));

        final Rect rectSrc = new Rect(bitmap.getWidth()/2 - rectSizeIn/3,
                bitmap.getHeight()/2 - rectSizeIn/3,
                bitmap.getWidth()/2 + rectSizeIn/3,
                bitmap.getHeight()/2 + rectSizeIn/3);
        final Rect rectDst = new Rect(0,0,outputBitmap.getWidth(),outputBitmap.getHeight());

        canvas.drawBitmap(bitmap,rectSrc,rectDst,paint);
        return outputBitmap;

    }

    //7: 取得圖片旋轉的角度
    public static int getBitmapDegree(String path){
        int degree = 0;

        try {
            //使用exif類取得或設定圖片的細部參數，此處只處理旋轉角度
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL);

            switch (orientation){
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return degree;
        }
        return degree;
    }

    //8: 使用指定的角度旋轉圖片
    public static Bitmap rotateBitmap(Bitmap bitmap, int degree){
        Bitmap returnbitmap = null;

        //根據角度生成旋轉矩陣，並設定取得的需旋轉角度
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        //創建一個有角度的圖，寬高與傳入的圖一樣
        try{
            returnbitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
            return  returnbitmap;
        }catch (OutOfMemoryError e){
            e.printStackTrace();
            return null;
        }

    }
    //9: bitmap存成圖檔
    public static void putBitmapToFile(Bitmap bitmap){
        String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "img";
        String filePath = folderPath + File.separator + "avatar.png";

        File file = new File(filePath);
        if(!file.exists()){
            boolean ret = file.mkdirs();
            if(!ret ){
                file.mkdir();
            }
        }

    }

    //10: 刪除暫存檔
    public static void delTempFile(){
        String folderPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "img";
        String filePath = folderPath + File.separator + "avatar_temp.png";
        File file = new File(filePath);
        file.delete();

        filePath = folderPath + File.separator + "temp" + ".png";
        file = new File(filePath);
        file.delete();

        filePath = folderPath + File.separator + "avatar" + ".png";
        file = new File(filePath);
        file.delete();
    }

    public static byte[] Bitmap2Bytes(Bitmap bm) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 100, baos);
        return baos.toByteArray();
    }

    public static String getRealFilePath(final Context context, final Uri uri ) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }


}
