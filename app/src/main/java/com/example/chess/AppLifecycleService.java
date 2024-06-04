package com.example.chess;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import androidx.annotation.Nullable;

import com.example.chess.firebase.FirebaseUtils;

public class AppLifecycleService extends Service {
	private int activityReferences = 0;
	private boolean isActivityChangingConfigurations = false;
	private FirebaseUtils firebaseUtils;
	private SharedPreferences sharedPreferences;

	private Application.ActivityLifecycleCallbacks activityLifecycleCallbacks = new Application.ActivityLifecycleCallbacks() {
		@Override
		public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
		}

		@Override
		public void onActivityStarted(Activity activity) {
			if (!activity.isChangingConfigurations()) {
				activityReferences++;
			}
		}

		@Override
		public void onActivityResumed(Activity activity) {
		}

		@Override
		public void onActivityPaused(Activity activity) {
		}

		@Override
		public void onActivityStopped(Activity activity) {
			if (!activity.isChangingConfigurations()) {
				activityReferences--;
			}
		}

		@Override
		public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
		}

		@Override
		public void onActivityDestroyed(Activity activity) {
		}
	};

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		firebaseUtils = new FirebaseUtils();
		sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
		String currentUsername = sharedPreferences.getString("loggedInUser", null);
		if (currentUsername != null) {
			firebaseUtils.addToActivePlayers(currentUsername);
		}

		getApplication().registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
		return START_STICKY;
	}

	@Override
	public void onTaskRemoved(Intent rootIntent) {
		// This is called if the service is currently running and the user has removed a task
		String currentUsername = sharedPreferences.getString("loggedInUser", null);
		if (currentUsername != null) {
			firebaseUtils.removeFromActivePlayers(currentUsername);
		}
		stopSelf();
	}

	@Override
	public void onDestroy() {
		getApplication().unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks);
		super.onDestroy();
	}
}
