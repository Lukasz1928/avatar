package com.sm.avatar;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.unity3d.player.UnityPlayer;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private String textInput;
    public final int MY_PERMISSIONS_RECORD_AUDIO = 0;
    public final int MY_PERMISSION_CAMERA = 1;
    public boolean recordAudioPermission = false;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private CameraService cameraService;
    private Intent cameraServiceIntent;
    private boolean cameraPermission = false;
    private UnityPlayer unityPlayer;

    private ServiceConnection cameraServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("cameraServiceConnection", componentName.toString() + " connected");
            CameraService.LocalBinder binder = (CameraService.LocalBinder) iBinder;
            cameraService = binder.getServiceInstance();
            cameraService.registerClient(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        Log.d("onCreate", "SpeechRecognizer created");
        cameraServiceIntent = new Intent(MainActivity.this, CameraService.class);
        Log.d("onCreate", "cameraServiceIntent created");

        setupAvatarView();
//        unityPlayer.UnitySendMessage("GameObject", "LookLeft", "");

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float v) {

            }

            @Override
            public void onBufferReceived(byte[] bytes) {

            }

            @Override
            public void onEndOfSpeech() {

            }

            @Override
            public void onError(int i) {
                Toast.makeText(MainActivity.this, "Error occurred during listening", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> matches = bundle.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    textInput = matches.get(0);
                    Toast.makeText(MainActivity.this, "Recognized text: " + textInput, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textInput = "";
                speechRecognizer.startListening(speechRecognizerIntent);
                Snackbar.make(view, "Started listening", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    private void setupAvatarView() {
        FrameLayout unityView = findViewById(R.id.unity_player);
        unityPlayer = new UnityPlayer(this);
        int glesMode = unityPlayer.getSettings().getInt("gles_mode", 1);
        unityPlayer.init(glesMode, false);


        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        unityView.addView(unityPlayer.getView(), 0, lp);

        unityPlayer.windowFocusChanged(true);
        unityPlayer.resume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            // If request is cancelled, the result arrays are empty.
            case MY_PERMISSIONS_RECORD_AUDIO: {
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    recordAudioPermission = true;
                }
            }
            case MY_PERMISSION_CAMERA: {
                if (grantResults.length > 0 && grantResults[0] ==
                        PackageManager.PERMISSION_GRANTED) {
                    Log.d("onRequestPermissionsResult", "cameraService starts");
                    if (!cameraPermission) {
                        bindService(cameraServiceIntent, cameraServiceConnection, Context.BIND_AUTO_CREATE);
                        cameraPermission = true;
                    }
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    MY_PERMISSIONS_RECORD_AUDIO);
        } else {
            recordAudioPermission = true;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    MY_PERMISSION_CAMERA);
        } else {
            Log.d("onStart", "cameraService starts");
            if (!cameraPermission) {
                bindService(cameraServiceIntent, cameraServiceConnection, Context.BIND_AUTO_CREATE);
                cameraPermission = true;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (cameraPermission) {
            stopService(cameraServiceIntent);
            unbindService(cameraServiceConnection);
            cameraPermission = false;
        }
    }
}
