package com.sm.avatar;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.LargestFaceFocusingProcessor;

import java.io.IOException;

public class CameraService extends Service {
    protected static final float WIDTH = 640;
    protected static final float HEIGHT = 480;
    private static final String LOG_TAG = "CameraService_";
    private CameraSource cameraSource;
    private FaceDetector faceDetector;
    private final IBinder mBinder = new LocalBinder();
    private MainActivity mainActivity;
    private Handler handler;

    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    private Tracker faceTracker = new Tracker<Face>() {
        @Override
        public void onUpdate(Detector.Detections<Face> detections, Face face) {
            super.onUpdate(detections, face);
            Log.d(LOG_TAG + "processImage", face.toString());
            float x = face.getPosition().x + face.getWidth() / 2;
            float y = face.getPosition().y - face.getHeight() / 2;
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            String xdir = "Straight";
            String ydir = "";
            if (x < WIDTH / 3) {
                xdir = "Left";
            } else if (x > (WIDTH / 3) * 2) {
                xdir = "Right";
            }
            if (y < HEIGHT / 3) {
                ydir = "Down";
            } else if (y > (HEIGHT / 3) * 2) {
                ydir = "Up";
            }
            String direction = "Look" + xdir + ydir;
            Log.d(LOG_TAG + "processImage", String.format("Topleft:x:%f,y:%f", face.getPosition().x, face.getPosition().y));
            Log.d(LOG_TAG + "processImage", String.format("Center:x:%f,y:%f", x, y));
            b.putString("direction", direction);
            msg.setData(b);
            handler.sendMessage(msg);
        }

        @Override
        public void onMissing(Detector.Detections<Face> detections) {
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            b.putString("direction", "LookStraight");
            msg.setData(b);
            handler.sendMessage(msg);
        }
    };

    public void readyCamera() {
        try {
            faceDetector = new
                    FaceDetector.Builder(getApplicationContext())
                    .setTrackingEnabled(false)
                    .setProminentFaceOnly(true)
                    .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                    .setLandmarkType(FaceDetector.NO_LANDMARKS)
                    .setMode(FaceDetector.FAST_MODE)
                    .build();
            if (!faceDetector.isOperational()) {
                throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "FACEDETECTOR UNOPERATIONAL");
            }
            faceDetector.setProcessor(new LargestFaceFocusingProcessor(faceDetector, faceTracker));
            cameraSource = new CameraSource.Builder(getApplicationContext(), faceDetector)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .setRequestedPreviewSize((int) WIDTH, (int) HEIGHT)
                    .setRequestedFps(1)
                    .build()
                    .start();
            Log.d(LOG_TAG + "readyCamera", "imageReader created");
        } catch (CameraAccessException | IOException e) {
            Log.e(LOG_TAG + "readyCamera", e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG + "onStartCommand", "onStartCommand flags " + flags + " startId " + startId);

        readyCamera();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        Log.d(LOG_TAG + "onCreate", "onCreate service");
        super.onCreate();
    }

    public void registerClient(Activity activity) {
        this.mainActivity = (MainActivity) activity;
    }

    public class LocalBinder extends Binder {
        public CameraService getServiceInstance() {
            return CameraService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}