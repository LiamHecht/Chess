package com.example.chess.gameplay;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;

public class SoundManager {
    private Context context;
    private MediaPlayer mediaPlayer;
    private Handler handler;

    public SoundManager(Context context) {
        this.context = context;
        this.handler = new Handler(Looper.getMainLooper());
    }

    public void playSound(final int soundResourceId) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null) {
                    mediaPlayer.release();
                }
                mediaPlayer = MediaPlayer.create(context, soundResourceId);
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                }
            }
        });
    }
}
