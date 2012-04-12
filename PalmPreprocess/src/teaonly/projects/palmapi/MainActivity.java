package teaonly.projects.palmapi;

import teaonly.projects.palmapi.*;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.System;
import java.lang.Thread;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceView;

public class MainActivity extends Activity implements View.OnTouchListener, CameraView.CameraReadyCallback {
    private CameraView cameraView_;
    private OverlayView overlayView_;

    private boolean labelProcessing_ = false;
    private LabelThread labelThread_ = null;
    private byte[] labelFrame = null; 

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NativeAPI.LoadLibraries();

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);    
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN); 

        setContentView(R.layout.main);

        SurfaceView cameraSurface = (SurfaceView)findViewById(R.id.surface_camera);
        cameraView_ = new CameraView(cameraSurface, 640, 480);        
        cameraView_.setCameraReadyCallback(this);

        overlayView_ = (OverlayView)findViewById(R.id.surface_overlay);
        overlayView_.setOnTouchListener(this);
    }
    
    @Override
    public void onCameraReady() {
        int wid = cameraView_.PictureWidth();
        int hei = cameraView_.PictureHeight();
        labelFrame = new byte[wid * hei + wid * hei / 2];
        NativeAPI.nativePrepare( wid, hei ); 
        cameraView_.DoPreview( previewCb_ ); 
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }   

    @Override
    public void onStart(){
        super.onStart();
    }   

    @Override
    public void onResume(){
        super.onResume();
    }   

    @Override
    public void onPause(){    
        super.onPause();
    }  

    @Override
    public boolean onTouch(View v, MotionEvent evt) {
        return false;
    } 

    private void waitCompleteLastLabeling() {
        if ( labelThread_ == null)
            return;

        if( labelThread_.isAlive() ){
            try {
                labelThread_.join();
                labelThread_ = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private PreviewCallback previewCb_ = new PreviewCallback() {
        public void onPreviewFrame(byte[] frame, Camera c) {
            if ( labelProcessing_ )
                return;

            labelProcessing_ = true; 
            int wid = cameraView_.PictureWidth();
            int hei = cameraView_.PictureHeight();             

            ByteBuffer bbuffer = ByteBuffer.wrap(frame); 
            bbuffer.get(labelFrame, 0, wid * hei + wid * hei / 2);

            waitCompleteLastLabeling();
            labelThread_ = new LabelThread();
            labelThread_.start();
        }
    };

    private class LabelThread extends Thread{
        private int width = cameraView_.PictureWidth();
        private int height = cameraView_.PictureHeight();

        @Override
        public void run() {           
            NativeAPI.nativeLabelPalm( labelFrame, cameraView_.PictureWidth(), cameraView_.PictureHeight() );

            overlayView_.DrawResult(labelFrame, cameraView_.PictureWidth(), cameraView_.PictureHeight() );

            labelProcessing_ = false;
        }
    }
}