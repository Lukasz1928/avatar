package com.sm.avatar;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;

import static com.sm.avatar.CameraService.HEIGHT;
import static com.sm.avatar.CameraService.WIDTH;

class FaceTracker extends Tracker<Face> {
    private Handler handler;
    private final static String LOG_TAG = "FaceTracker_";

    public FaceTracker(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void onUpdate(Detector.Detections<Face> detections, Face face) {
        super.onUpdate(detections, face);
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
        } else if (y<HEIGHT/3){
            direction = "LookUp";
        } else if (y>HEIGHT/3*2){
            direction = "LookDown";
        } else {

        }
        b.putString("direction", direction);
        msg.setData(b);
        handler.sendMessage(msg);
    }
}
