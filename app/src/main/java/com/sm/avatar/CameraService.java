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
import android.media.FaceDetector;
import android.media.Image;
import android.media.ImageReader;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class CameraService extends Service {
    protected static final int WIDTH = 1920;
    protected static final int HEIGHT = 1080;
    protected static final int CAMERA_CALIBRATION_DELAY = 3000;
    protected static final int CAMERACHOICE = CameraCharacteristics.LENS_FACING_FRONT;
    protected static long cameraCaptureStartTime;
    protected CameraDevice cameraDevice;
    protected CameraCaptureSession session;
    protected ImageReader imageReader;
    protected FaceDetector faceDetector;
    private final IBinder mBinder = new LocalBinder();
    private MainActivity mainActivity;

    protected CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            Log.d("cameraStateCallback.onOpened", "CameraDevice.StateCallback onOpened");
            cameraDevice = camera;
            actOnReadyCameraDevice();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            Log.w("cameraStateCallback.onDisconnected", "CameraDevice.StateCallback onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.e("cameraStateCallback.onError", "CameraDevice.StateCallback onError " + error);
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
                Log.e("sessionStateCallback.onReady", e.getMessage());
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
            Log.d("onImageAvailableListener.onImageAvailable", "onImageAvailable");
            Image img = reader.acquireLatestImage();
            if (img != null) {
                if (System.currentTimeMillis() > cameraCaptureStartTime + CAMERA_CALIBRATION_DELAY) {
                    processImage(img);
                }
                img.close();
            }
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
            faceDetector = new FaceDetector(WIDTH, HEIGHT, 1);
            Log.d("readyCamera", "imageReader created");
        } catch (CameraAccessException e) {
            Log.e("readyCamera", e.getMessage());
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
        Log.d("onStartCommand", "onStartCommand flags " + flags + " startId " + startId);

        readyCamera();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        Log.d("onCreate", "onCreate service");
        super.onCreate();
    }

    public void actOnReadyCameraDevice() {
        try {
            cameraDevice.createCaptureSession(Arrays.asList(imageReader.getSurface()), sessionStateCallback, null);
        } catch (CameraAccessException e) {
            Log.e("actOnReadyCameraDevice", e.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        try {
            session.abortCaptures();
        } catch (CameraAccessException e) {
            Log.e("onDestroy", e.getMessage());
        }
        session.close();
        imageReader.close();
        cameraDevice.close();
    }

    private void processImage(Image image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
//        buffer.rewind();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        Bitmap bitmapImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);//.copy(Bitmap.Config.RGB_565, false);
        FaceDetector.Face[] faces = new FaceDetector.Face[10];
        if (faceDetector.findFaces(bitmapImage, faces) >= 1) {
            FaceDetector.Face face = faces[0];
            Log.d("processImage", face.toString());
        }
        else{
            Log.d("processImage", "noFaces");
        }
    }

    protected CaptureRequest createCaptureRequest() {
        try {
            CaptureRequest.Builder builder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            builder.addTarget(imageReader.getSurface());
            return builder.build();
        } catch (CameraAccessException e) {
            Log.e("createCaptureRequest", e.getMessage());
            return null;
        }
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