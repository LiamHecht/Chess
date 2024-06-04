package com.example.chess.gameplay;

import android.os.CountDownTimer;
import android.widget.TextView;

public class GameTimer {
    private TextView timerTextView;
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis; // Time left in milliseconds
    private TimerListener mListener; // The callback

    public interface TimerListener {
        void onTimeOver();
    }

    public GameTimer(TextView timerTextView) {
        this.timerTextView = timerTextView;
    }

    public void setTimerListener(TimerListener listener) {
        this.mListener = listener;
    }

    public void startTimer(long initialMillis) {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(initialMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateTimerText();
            }

            @Override
            public void onFinish() {
                // Handle timer finish
                timerTextView.setText("Time's up!");
                if (mListener != null) {
                    mListener.onTimeOver();
                }
            }
        }.start();
    }

    public void stopTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    public long pauseTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        return timeLeftInMillis;
    }

    public void resumeTimer() {
        startTimer(timeLeftInMillis);
    }

    public void resetTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        timeLeftInMillis = 0;
        updateTimerText();
    }
    public void updateTimerText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;
        String timeLeftFormatted = String.format("%02d:%02d", minutes, seconds);
        timerTextView.setText(timeLeftFormatted);
    }

    public void setInitialTime(long initialTime) {
        timerTextView.setText(timeToString(initialTime));
    }

    private String timeToString(long time) {
        int totalSecs = (int) time / 1000;
        int minutes = totalSecs / 60;
        int seconds = totalSecs % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
