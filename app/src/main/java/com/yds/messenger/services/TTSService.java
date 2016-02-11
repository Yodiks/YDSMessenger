package com.yds.messenger.services;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Service that invokes Text to Speech to speak out a message
 */
public class TTSService extends Service implements TextToSpeech.OnInitListener {

    private static final String TAG = "TTSService";
    private String str;
    private TextToSpeech mTts;

    @Override
    public void onCreate() {
        super.onCreate();
    }


    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        if (mTts != null) {
            mTts.stop();
            mTts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mTts = new TextToSpeech(this, this);
        str = intent.getStringExtra("text");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = mTts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.v(TAG, "Language is not available.");
                stopSelf();
            } else {
                sayHello(str);

            }
        } else {
            Log.v(TAG, "Could not initialize TextToSpeech.");
            stopSelf();
        }
    }

    @SuppressLint("NewApi")
    private void sayHello(String str) {
        mTts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
            }

            @Override
            public void onDone(String s) {
                stopSelf();
            }

            @Override
            public void onError(String s) {
                stopSelf();
            }
        });
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "messageID");
        mTts.speak(str, TextToSpeech.QUEUE_FLUSH, map);
    }
}
/**
 * Created by yds on 30/11/15.
 */