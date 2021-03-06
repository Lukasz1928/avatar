package com.sm.avatar;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.auth.oauth2.GoogleCredentials;
import com.sm.avatar.chatbot.ChatbotHandler;
import com.sm.avatar.chatbot.dialogflow.DialogflowChatbotHandler;
import com.sm.avatar.chatbot.exceptions.ChatbotInitializationException;
import com.unity3d.player.UnityPlayer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.unity3d.player.UnityPlayer;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private String textInput;
    public final int MY_PERMISSIONS_RECORD_AUDIO = 0;
    public final int MY_PERMISSION_CAMERA = 1;
    public boolean recordAudioPermission = false;
    private SpeechRecognizer speechRecognizer;
    private Intent speechRecognizerIntent;
    private TextToSpeech textToSpeech;
    private CameraService cameraService;
    private Intent cameraServiceIntent;
    private boolean cameraPermission = false;
    private Handler cameraHandler;
    private UnityPlayer unityPlayer;
    private boolean textToSpeechInitialized = false;
    private String utteranceID;
    private final Locale locale = Locale.UK;

    private ChatbotHandler chatbotHandler;

    public UnityPlayer getUnityPlayer() {
        return unityPlayer;
    }

    private ServiceConnection cameraServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d("cameraServiceConnection", componentName.toString() + " connected");
            CameraService.LocalBinder binder = (CameraService.LocalBinder) iBinder;
            cameraService = binder.getServiceInstance();
            cameraService.setHandler(cameraHandler);
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
        setUpSpeechRecognizer();
        setUpTextToSpeech();
        setupAvatarView();
        setUpChatbot();
//        unityPlayer.UnitySendMessage("GameObject", "LookLeft", "");

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

        cameraServiceIntent = new Intent(this, CameraService.class);
        cameraHandler = new Handler() {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String direction = msg.getData().getString("direction");
                Log.d("CameraHandler", "Received: " + direction);
                MainActivity.this.getUnityPlayer().UnitySendMessage("GameObject", direction, "");
            }
        };
    }

    private void setUpSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale);

        speechRecognizer.setRecognitionListener(new RecognitionListener() {

            private boolean isRealOnResultsCall = false;

            public void onError(int i) {
                Toast.makeText(MainActivity.this, "Error occurred during listening", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle bundle) {
                    ArrayList<String> matches = bundle.getStringArrayList(speechRecognizer.RESULTS_RECOGNITION);
                    Log.d("ONRESULTS", "ONRESULTS" + String.valueOf(isRealOnResultsCall));
                    if (matches != null) {
                        if(isRealOnResultsCall) {
                            textInput = matches.get(0);
                            Toast.makeText(MainActivity.this, "Recognized text: " + textInput, Toast.LENGTH_LONG).show();
                            chatbotHandler.requestResponse(textInput, locale.toLanguageTag());
                            isRealOnResultsCall = false;
                        }
                    }
            }

            @Override
            public void onReadyForSpeech(Bundle bundle) {

            }

            @Override
            public void onBeginningOfSpeech() {
                isRealOnResultsCall = true;
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
            public void onPartialResults(Bundle bundle) {

            }

            @Override
            public void onEvent(int i, Bundle bundle) {

            }
        });

    }

    private void setUpTextToSpeech() {
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(locale);

                    if (result == TextToSpeech.LANG_NOT_SUPPORTED ||
                            result == TextToSpeech.LANG_MISSING_DATA) {
                        Toast.makeText(MainActivity.this, "Language not supported", Toast.LENGTH_SHORT).show();
                    } else {
                        textToSpeechInitialized = true;
                        utteranceID = (new Random().nextInt() % 9999999) + "";

                        Set<Voice> voices = textToSpeech.getVoices();
                        if (voices != null && !voices.isEmpty()) {
                            for (Voice voice : voices) {
                                if (voice.getLocale().equals(locale) && voice.getName().contains("#male")) {
                                    textToSpeech.setVoice(voice);
                                    break;
                                }
                            }
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Text to speech initialization failed", Toast.LENGTH_SHORT).show();
                }
            }
        }, "com.google.android.tts");
    }

    private void setUpChatbot() {
        InputStream credentialsStream = getResources().openRawResource(R.raw.agent_credentials);
        GoogleCredentials credentials = null;
        try {
            credentials = GoogleCredentials.fromStream(credentialsStream);
        }
        catch(IOException e) {
            Toast.makeText(MainActivity.this, "Credentials fail", Toast.LENGTH_SHORT).show();
        }
        chatbotHandler = new DialogflowChatbotHandler(new AsyncResponse() {
            @Override
            public void taskFinished(String result) {
                if(textToSpeechInitialized){
                    textToSpeech.speak(result, TextToSpeech.QUEUE_FLUSH, null, utteranceID);
                }
                else{
                    Toast.makeText(MainActivity.this, "Response: " + result, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void taskFailed() {
                if(textToSpeechInitialized){
                    textToSpeech.speak("Failed to get the response", TextToSpeech.QUEUE_FLUSH, null, utteranceID );
                }
                else{
                    Toast.makeText(MainActivity.this, "Failed to get the response", Toast.LENGTH_LONG).show();
                }
            }
        });
        try {
            ((DialogflowChatbotHandler)chatbotHandler).initialize(credentials);
        }
        catch(ChatbotInitializationException e) {
            Toast.makeText(MainActivity.this, "Init error", Toast.LENGTH_SHORT).show();
        }
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
                        startService(cameraServiceIntent);
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
                startService(cameraServiceIntent);
                cameraPermission = true;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            unityPlayer.quit();

        }
        super.onDestroy();
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus)
    {
        super.onWindowFocusChanged(hasFocus);
        unityPlayer.windowFocusChanged(hasFocus);
    }
    @Override
    public void onPause() {
        super.onPause();
        unityPlayer.pause();
    }
    @Override
    public void onResume() {
        super.onResume();
        unityPlayer.resume();
    }

}
