package com.sm.avatar;

import android.graphics.PointF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.face.Face;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(Parameterized.class)
public class CameraServiceTest {
    @Mock
    Handler mockHandler;
    CameraService cameraService;
    Message message;
    Bundle bundle;
    Face face;

    static float faceWidth = 50;
    static float faceHeight = 100;


    @Parameterized.Parameters
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0, 100, "LookLeftDown"},
                {280, 100, "LookStraightDown"},
                {450, 100, "LookRightDown"},
                {0, 250, "LookLeft"},
                {280, 250, "LookStraight"},
                {450, 250, "LookRight"},
                {0, 450, "LookLeftUp"},
                {280, 450, "LookStraightUp"},
                {450, 450, "LookRightUp"},
        });
    }

    @Before
    public void initialize() {
        cameraService = new CameraService();
        message = mock(Message.class);
        mockHandler = mock(Handler.class);
        bundle = mock(Bundle.class);
        face = mock(Face.class);
        when(face.getWidth()).thenReturn(faceWidth);
        when(face.getHeight()).thenReturn(faceHeight);
        when(message.getData()).thenReturn(bundle);
        when(mockHandler.obtainMessage()).thenReturn(message);
        cameraService.setHandler(mockHandler);
    }

    @Parameterized.Parameter(0)
    public float x;

    @Parameterized.Parameter(1)
    public float y;

    @Parameterized.Parameter(2)
    public String expectedOutput;

    @Test
    public void faceTrackerOnUpdateTest() {
        PointF faceLocation = mock(PointF.class);
        faceLocation.x = x;
        faceLocation.y = y;
        when(face.getPosition()).thenReturn(faceLocation);
        cameraService.faceTracker.onUpdate(mock(Detector.Detections.class), face);
        verify(bundle).putString("direction", expectedOutput);
    }

    @Test
    public void faceTrackerOnMissingTest() {
        cameraService.faceTracker.onMissing(mock(Detector.Detections.class));
        verify(bundle).putString("direction", "LookStraight");
    }
}
