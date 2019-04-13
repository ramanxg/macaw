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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ShowCamera extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {

    Camera camera;
    ImageView imageView;
    SurfaceHolder holder;
    private byte[] mPreviewFrameBuffer;
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

        System.out.println("hi");
        try {
            lock.lock();
            mPreviewFrameBuffer = bytes;
            System.out.println("hi");
            Bitmap b = convertYuvByteArrayToBitmap(bytes, camera);
            if(b != null)
            {
                //ImageView img = (ImageView)findViewById(R.id.imageView);
                System.out.println(this.imageView == null);
                this.imageView.setImageBitmap(b);
            }
        } finally {
            lock.unlock();
        }
    }
    public static Bitmap convertYuvByteArrayToBitmap(byte[] data, Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        YuvImage image = new YuvImage(data, parameters.getPreviewFormat(), size.width, size.height, null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, size.width, size.height), 100, out);
        byte[] imageBytes = out.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
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