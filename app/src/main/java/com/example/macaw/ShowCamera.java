package com.example.macaw;

import android.content.Context;
import android.content.res.Configuration;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.ImageView;

import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ShowCamera extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    Camera camera;
    ImageView imageView;
    SurfaceHolder holder;
    private byte[] mPreviewFrameBuffer;
    private byte[] currentImageByte;
    // Holds the current frame, so we can react on a click event:
    private final Lock lock = new ReentrantLock();

    public ShowCamera(Context context, Camera camera, ImageView img) {
        super(context);
        this.camera = camera;
        this.imageView = img;
        holder = getHolder();
        holder.addCallback(this);
    }
    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

        try {
            lock.lock();
            mPreviewFrameBuffer = bytes;
            currentImageByte = convertYuvByteArrayToByteArray(bytes, camera);
            Bitmap bitmap = BitmapFactory.decodeByteArray(currentImageByte, 0, currentImageByte.length);
            if(bitmap != null)
            {
                this.imageView.setImageBitmap(bitmap);
            }
        } finally {
            lock.unlock();
        }
    }


    public static byte[] convertYuvByteArrayToByteArray(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        YuvImage image = new YuvImage(data, parameters.getPreviewFormat(), size.width, size.height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, out);
        byte[] imageBytes = out.toByteArray();
        return imageBytes;
    }
    public FirebaseVisionImage imageFromArray() {
        FirebaseVisionImageMetadata metadata = new FirebaseVisionImageMetadata.Builder()
                .setWidth(480)   // 480x360 is typically sufficient for
                .setHeight(360)  // image recognition BUT TOO BAD
                .setFormat(FirebaseVisionImageMetadata.IMAGE_FORMAT_NV21)
                //.setRotation(FirebaseVisionImageMetadata.ROTATION_90)
                .build();
        // [START image_from_array]
        return FirebaseVisionImage.fromByteArray(mPreviewFrameBuffer, metadata);
        // [END image_from_array]
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        camera.setPreviewCallback(this);


    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Camera.Parameters params = camera.getParameters();
        if (this.getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE)
        {
            params.set("orientation","portrait");
            camera.setDisplayOrientation(90);
            params.setRotation(90);
        }
        else
        {
            params.set("orientation","landscape");
            camera.setDisplayOrientation(0);
            params.setRotation(0);
        }

        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        camera.setParameters(params);

        try {
            camera.setPreviewDisplay(holder);
            camera.setPreviewCallback(this);
            camera.startPreview();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }

    }
}