package com.sm.avatar;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import android.media.Image;
import android.media.ImageReader;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraService extends Service {
    protected static final int WIDTH = 640;
    protected static final int HEIGHT = 480;
    protected static final int CAMERA_CALIBRATION_DELAY = 3000;
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_FRONT;
    private static final String LOG_TAG = "CameraService_";
    protected static long cameraCaptureStartTime;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;
    protected FaceDetector faceDetector;
    private final IBinder mBinder = new LocalBinder();
    private MainActivity mainActivity;
    private Handler handler;

    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d(LOG_TAG + LOG_TAG + "cameraStateCallback.onOpened", "CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            actOnReadyCameraDevice();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w(LOG_TAG + LOG_TAG + "cameraStateCallback.onDisconnected", "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e(LOG_TAG + LOG_TAG + "cameraStateCallback.onError", "CameraDevice.StateCallback onError " + error);
        }
    };

    protected CameraCaptureSession.StateCallback sessionStateCallback = new CameraCaptureSession.StateCallback() {

        @Override
        public void onReady(CameraCaptureSession session) {
            CameraService.this.session = session;
            try {
                session.setRepeatingRequest(createCaptureRequest(), null, null);
                cameraCaptureStartTime = System.currentTimeMillis();
            } catch (CameraAccessException e) {
                Log.e(LOG_TAG + LOG_TAG + "sessionStateCallback.onReady", e.getMessage());
            }
        }

        @Override
        public void onConfigured(CameraCaptureSession session) {

        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        }
    };

    protected ImageReader.OnImageAvailableListener onImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Log.d(LOG_TAG + "onImageAvailableListener.onImageAvailable", "onImageAvailable");
//            if (System.currentTimeMillis() > cameraCaptureStartTime + CAMERA_CALIBRATION_DELAY) {
            Image img = reader.acquireLatestImage();
            if (img != null) {
                processImage(img);
                img.close();
            }
//            }
        }
    };

    public void readyCamera() {
        CameraManager manager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            String pickedCamera = getCamera(manager);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            manager.openCamera(pickedCamera, cameraStateCallback, null);
            imageReader = ImageReader.newInstance(WIDTH, HEIGHT, ImageFormat.JPEG, 2 /* images buffered */);
            imageReader.setOnImageAvailableListener(onImageAvailableListener, null);
//            faceDetector = new FaceDetector(WIDTH, HEIGHT, 1);
            faceDetector = new
                    FaceDetector.Builder(getApplicationContext())
                    .setTrackingEnabled(false)
                    .setProminentFaceOnly(true)
                    .setClassificationType(FaceDetector.NO_CLASSIFICATIONS)
                    .setLandmarkType(FaceDetector.NO_LANDMARKS)
                    .setMode(FaceDetector.FAST_MODE)
                    .build();
            if(!faceDetector.isOperational()){
                throw new CameraAccessException(CameraAccessException.CAMERA_ERROR, "FACEDETECTOR UNOPERATIONAL");
            }
            Log.d(LOG_TAG + LOG_TAG + "readyCamera", "imageReader created");
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG + LOG_TAG + "readyCamera", e.getMessage());
        }
    }

    public String getCamera(CameraManager manager) {
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cOrientation == CAMERACHOICE) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        return null;
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

    public void actOnReadyCameraDevice() {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG + "actOnReadyCameraDevice", e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        try {
            session.abortCaptures();
            session.close();
        } catch (CameraAccessException | NullPointerException e) {
            Log.e(LOG_TAG + "onDestroy", e.getMessage());
        }
        if (imageReader != null)
            imageReader.close();
        if (cameraDevice != null)
            cameraDevice.close();
    }

    private void processImage(Image image) {
        Log.d(LOG_TAG + "processImage", image.toString() + ", planesCount:" + image.getPlanes().length);
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
//        buffer.rewind();
        Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);//.copy(Bitmap.Config.RGB_565, false);\
//        Bitmap bitmapImage = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.RGB_565);
//        bitmapImage.copyPixelsFromBuffer(ByteBuffer.wrap(bytes));
//        bitmapImage.copyPixelsFromBuffer(buffer);
        Frame frame = new Frame.Builder().setBitmap(bitmapImage).build();
        SparseArray<Face> faces = faceDetector.detect(frame);
        if (faces.size() > 0) {
            Face face = faces.valueAt(0);
            Log.d(LOG_TAG + "processImage", face.toString());
            float x = face.getPosition().x + face.getWidth()/2;
            float y = face.getPosition().y + face.getHeight()/2;
            Message msg = handler.obtainMessage();
            Bundle b = new Bundle();
            String direction = "";
            if(x<WIDTH/3){
                direction = "LookLeft";
            } else if (x>WIDTH/3*2){
                direction = "LookRight";
            } else if (y<WIDTH/3){
                direction = "LookUp";
            } else if (y>WIDTH/3*2){
                direction = "LookDown";
            } else {

            }
            b.putString("direction", direction);
            msg.setData(b);
            handler.sendMessage(msg);
        } else {
            Log.d(LOG_TAG + "processImage", "noFaces");
        }
//        FaceDetector.Face[] faces = new FaceDetector.Face[1];
//        if (faceDetector.findFaces(bitmapImage, faces) >= 1) {
//            FaceDetector.Face face = faces[0];
//            Log.d(LOG_TAG + "processImage", face.toString());
//        } else {
//            Log.d(LOG_TAG + "processImage", "noFaces");
//        }
    }

    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(imageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e(LOG_TAG + "createCaptureRequest", e.getMessage());
            return null;
        }
    }

    public void registerClient(Activity activity, Handler handler) {
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