package io.ionic.starter;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class GameWidget extends AppWidgetProvider {

    private static final String TAG = "GameWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called, widgetIds: " + appWidgetIds.length);
        triggerUpdate(context);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled called");
        triggerUpdate(context);
    }

    public static void triggerUpdate(Context context) {
        Log.d(TAG, "triggerUpdate called");
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest
            .Builder(WidgetUpdateWorker.class)
            .build();
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "widget_update",
                ExistingWorkPolicy.REPLACE,
                workRequest
            );
    }
}