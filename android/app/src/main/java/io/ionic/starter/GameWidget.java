package io.ionic.starter;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

public class GameWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            Intent intent = new Intent(context, WidgetUpdateService.class);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            context.startService(intent);
        }
    }

    @Override
    public void onEnabled(Context context) {
        Intent intent = new Intent(context, WidgetUpdateService.class);
        context.startService(intent);
    }

    @Override
    public void onDisabled(Context context) {
        Intent intent = new Intent(context, WidgetUpdateService.class);
        context.stopService(intent);
    }
}