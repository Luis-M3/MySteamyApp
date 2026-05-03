package io.ionic.starter;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WidgetUpdateWorker extends Worker {

    private static final String TAG       = "WidgetUpdateWorker";
    private static final String BASE_URL  = "https://www.cheapshark.com/api/1.0";
    private static final String WORK_NAME = "widget_update_unique";

    private final OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build();

    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork started");
        Context ctx = getApplicationContext();

        AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
        ComponentName component  = new ComponentName(ctx, GameWidget.class);
        int[] widgetIds          = manager.getAppWidgetIds(component);

        if (widgetIds.length == 0) {
            Log.d(TAG, "No widgets, stopping");
            return Result.success();
        }

        try {
            SharedPreferences prefs = ctx.getSharedPreferences(
                "CapacitorStorage", Context.MODE_PRIVATE);
            String favJson = prefs.getString("favoriteGame", null);
            Log.d(TAG, "favoriteGame: " + (favJson != null ? "found" : "null"));

            if (favJson == null) {
                showEmpty(manager, widgetIds, ctx);
                return Result.success();
            }

            JSONObject fav    = new JSONObject(favJson);
            String title      = fav.optString("title", "");
            String thumb      = fav.optString("thumb", "");
            String salePrice  = fav.optString("salePrice", "0");
            String savingsStr = fav.optString("savings", "0");
            String storeID    = fav.optString("storeID", "");

            int savingsPct = 0;
            try {
                savingsPct = (int) Math.round(Double.parseDouble(savingsStr));
            } catch (NumberFormatException ignored) {}

            // Paso 1: stores (una sola petición)
            JSONArray storesArr = fetchJsonArray(BASE_URL + "/stores");
            String logoUrl      = getStoreLogo(storesArr, storeID);

            // Paso 2: imágenes
            Bitmap gameBmp = getBitmap(thumb);
            Bitmap logoBmp = getBitmap(logoUrl);

            // Paso 3: mostrar favorito inmediatamente
            renderWidget(manager, widgetIds, ctx, 0,
                title, salePrice, savingsPct, gameBmp, logoBmp);

            // Paso 4: rotar con deals adicionales (con espera larga para no rate-limitar)
            Thread.sleep(3000);
            tryRotation(manager, widgetIds, ctx, fav, gameBmp, storesArr);

        } catch (Exception e) {
            Log.e(TAG, "doWork error: " + e.getMessage());
        }

        // Re-encolar para actualizar en 30 minutos
        scheduleNextUpdate(ctx);

        return Result.success();
    }

    private void scheduleNextUpdate(Context ctx) {
        try {
            OneTimeWorkRequest next = new OneTimeWorkRequest.Builder(WidgetUpdateWorker.class)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build();
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                next
            );
            Log.d(TAG, "Next update scheduled in 30 minutes");
        } catch (Exception e) {
            Log.w(TAG, "Could not schedule next update: " + e.getMessage());
        }
    }

    private void tryRotation(AppWidgetManager manager, int[] widgetIds,
                              Context ctx, JSONObject fav,
                              Bitmap gameBmp, JSONArray storesArr) {
        try {
            String title   = fav.optString("title", "");
            String encoded = java.net.URLEncoder.encode(title, "UTF-8");

            JSONArray dealsArr = fetchJsonArray(
                BASE_URL + "/deals?title=" + encoded + "&pageSize=5");

            if (dealsArr.length() <= 1) {
                Log.d(TAG, "Only 1 deal, no rotation");
                return;
            }

            int maxDeals = Math.min(dealsArr.length(), 4);

            for (int r = 0; r < maxDeals; r++) {
                if (isStopped()) break;

                JSONObject deal   = dealsArr.getJSONObject(r);
                String dealStore  = deal.optString("storeID", "");
                String dealPrice  = deal.optString("salePrice", "0");
                String dealNormal = deal.optString("normalPrice", "0");

                double retail = Double.parseDouble(dealNormal);
                double sale   = Double.parseDouble(dealPrice);
                int pct       = retail > 0
                    ? (int) Math.round((1 - sale / retail) * 100) : 0;

                String dealLogo    = getStoreLogo(storesArr, dealStore);
                Bitmap dealLogoBmp = getBitmap(dealLogo);

                renderWidget(manager, widgetIds, ctx, r % 2,
                    title, dealPrice, pct, gameBmp, dealLogoBmp);

                Log.d(TAG, "Rotation[" + r + "]: $" + dealPrice + " -" + pct + "%");

                if (r < maxDeals - 1) Thread.sleep(5000);
            }

        } catch (Exception e) {
            Log.w(TAG, "tryRotation failed: " + e.getMessage());
        }
    }

    private void renderWidget(AppWidgetManager manager, int[] widgetIds,
                               Context ctx, int viewIdx,
                               String title, String price, int pct,
                               Bitmap gameBmp, Bitmap logoBmp) {
        int titleId = viewIdx == 0 ? R.id.gameTitle1 : R.id.gameTitle2;
        int priceId = viewIdx == 0 ? R.id.salePrice1 : R.id.salePrice2;
        int savId   = viewIdx == 0 ? R.id.savings1   : R.id.savings2;
        int imgId   = viewIdx == 0 ? R.id.gameImage1 : R.id.gameImage2;
        int logoId  = viewIdx == 0 ? R.id.storeLogo1 : R.id.storeLogo2;

        for (int id : widgetIds) {
            RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.game_widget);
            v.setViewVisibility(R.id.viewFlipper, View.VISIBLE);
            v.setViewVisibility(R.id.emptyState,  View.GONE);
            v.setDisplayedChild(R.id.viewFlipper, viewIdx);
            v.setTextViewText(titleId, title);
            v.setTextViewText(priceId, "$" + price);
            v.setTextViewText(savId,   "-" + pct + "%");
            if (gameBmp != null) v.setImageViewBitmap(imgId,  gameBmp);
            if (logoBmp != null) v.setImageViewBitmap(logoId, logoBmp);
            manager.updateAppWidget(id, v);
        }
        Log.d(TAG, "Rendered view[" + viewIdx + "]: " + title + " $" + price);
    }

    private String getStoreLogo(JSONArray stores, String storeID) {
        try {
            for (int i = 0; i < stores.length(); i++) {
                JSONObject store = stores.getJSONObject(i);
                if (store.getString("storeID").equals(storeID)) {
                    return "https://www.cheapshark.com" +
                        store.getJSONObject("images").getString("logo");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getStoreLogo error: " + e.getMessage());
        }
        return "";
    }

    private void showEmpty(AppWidgetManager manager, int[] widgetIds, Context ctx) {
        for (int id : widgetIds) {
            RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.game_widget);
            v.setViewVisibility(R.id.viewFlipper, View.GONE);
            v.setViewVisibility(R.id.emptyState,  View.VISIBLE);
            manager.updateAppWidget(id, v);
        }
        Log.d(TAG, "Showing empty state");
    }

    private JSONArray fetchJsonArray(String url) throws Exception {
        return new JSONArray(fetch(url));
    }

    private String fetch(String url) throws Exception {
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0")
            .header("Accept", "application/json")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 429) {
                Log.w(TAG, "Rate limited 429, waiting 5s then retrying...");
                Thread.sleep(5000);
                try (Response retry = client.newCall(request).execute()) {
                    if (!retry.isSuccessful()) {
                        throw new Exception("HTTP " + retry.code() + " → " + url);
                    }
                    return retry.body().string();
                }
            }
            if (!response.isSuccessful()) {
                throw new Exception("HTTP " + response.code() + " → " + url);
            }
            return response.body().string();
        }
    }

    private Bitmap getBitmap(String urlStr) {
        try {
            if (urlStr == null || urlStr.isEmpty()) return null;
            Request request = new Request.Builder()
                .url(urlStr)
                .header("User-Agent", "Mozilla/5.0")
                .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) return null;
                InputStream is = response.body().byteStream();
                return BitmapFactory.decodeStream(is);
            }
        } catch (Exception e) {
            Log.e(TAG, "getBitmap error: " + urlStr, e);
            return null;
        }
    }
}