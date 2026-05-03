package io.ionic.starter;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class GameWidget extends AppWidgetProvider {

    private static final String TAG = "GameWidget";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate called");
        triggerUpdate(context);
    }

    @Override
    public void onEnabled(Context context) {
        Log.d(TAG, "onEnabled called");
        triggerUpdate(context);
    }

    public static void triggerUpdate(Context context) {
        Log.d(TAG, "triggerUpdate → starting WidgetService");
        Intent intent = new Intent(context, WidgetService.class);
        context.startService(intent);
    }
}