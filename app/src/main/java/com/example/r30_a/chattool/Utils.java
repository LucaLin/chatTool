package com.example.r30_a.chattool;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class Utils {


    private static Utils instance;

    public static Utils getInstance() {
        if (instance == null)
            instance = new Utils();

        return instance;
    }

    public  String getFileName() {
        Date d = Calendar.getInstance().getTime();
        String ts = new SimpleDateFormat("yyyyMMddHHmmss-SSS").format(d);
        String fileName = ts.concat(".jpg");

        return fileName;
    }

    public  File getFireDir() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File dir = new File(filepath + "/ChatTool/uploads");
        // 檢查資料夾是否存在
        if (!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    // 取得圖片旋轉的角度
    public int getRotationDegree(String path) {
        int degree = 0;

        try {
            //使用exif類取得或設定圖片的細部參數，此處只處理旋轉角度
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
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
    // 使用指定的角度旋轉圖片
    public Bitmap rotateBitmap(Bitmap bitmap, int degree) {
        Bitmap returnbitmap = null;

        //根據角度生成旋轉矩陣，並設定取得的需旋轉角度
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        //創建一個有角度的圖，寬高與傳入的圖一樣
        try {
            returnbitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            return returnbitmap;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }

    }


    /**
     * 計算圖片解析度
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height/ (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        return inSampleSize;
    }

    // bitmap 壓縮並寫入檔案
    public File bitmapToFile(File file, String cameraFileName,  Bitmap bitmap) {
        File uploadFile = null;

        Matrix matrix = new Matrix();
        matrix.setScale(0.5f, 0.5f);
        bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, os);
        byte[] bitmapdata = os.toByteArray();

        try {
            uploadFile = new File(file, cameraFileName);

            FileOutputStream fos = new FileOutputStream(uploadFile);
            fos.write(bitmapdata);
            fos.flush();
            fos.close();


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return uploadFile;
    }
}
