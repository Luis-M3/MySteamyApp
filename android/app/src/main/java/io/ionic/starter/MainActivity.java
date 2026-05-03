package io.ionic.starter;

import android.os.Bundle;
import android.util.Log;

import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final String TAG = "MainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(WidgetBridgePlugin.class);
        super.onCreate(savedInstanceState);

        // Inicializar WorkManager explícitamente para MIUI
        try {
            WorkManager.getInstance(this);
            Log.d(TAG, "WorkManager initialized");
        } catch (Exception e) {
            // Si ya está inicializado, ignorar
            Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build();
            WorkManager.initialize(this, config);
            Log.d(TAG, "WorkManager re-initialized");
        }
    }
}