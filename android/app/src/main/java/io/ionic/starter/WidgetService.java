package io.ionic.starter;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

public class WidgetService extends Service {

    private static final String TAG     = "WidgetService";
    private static final String API_URL = "https://www.cheapshark.com/api/1.0";
    private static final AtomicBoolean running = new AtomicBoolean(false);

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");

        if (running.compareAndSet(false, true)) {
            new Thread(() -> {
                try {
                    run();
                } finally {
                    running.set(false);
                    stopSelf();
                }
            }).start();
        } else {
            Log.d(TAG, "Already running, skip");
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void run() {
        Log.d(TAG, "run started");
        Context ctx = getApplicationContext();

        AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
        ComponentName component  = new ComponentName(ctx, GameWidget.class);
        int[] widgetIds          = manager.getAppWidgetIds(component);

        Log.d(TAG, "widgetIds count: " + widgetIds.length);
        if (widgetIds.length == 0) return;

        try {
            SharedPreferences prefs = ctx.getSharedPreferences(
                "CapacitorStorage", Context.MODE_PRIVATE);
            String favJson = prefs.getString("favoriteGame", null);
            Log.d(TAG, "favoriteGame: " + (favJson != null ? "FOUND" : "NULL"));

            if (favJson == null) {
                showEmpty(manager, widgetIds, ctx);
                return;
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

            Log.d(TAG, "Loading widget for: " + title);

            // Obtener logo de la tienda
            JSONArray stores   = fetchArray(API_URL + "/stores");
            String    logoUrl  = getStoreLogo(stores, storeID);

            // Descargar imágenes
            Bitmap gameBmp = getBitmap(thumb);
            Bitmap logoBmp = getBitmap(logoUrl);

            // Mostrar favorito inmediatamente
            render(manager, widgetIds, ctx, 0,
                title, salePrice, savingsPct, gameBmp, logoBmp);

            Log.d(TAG, "Widget rendered successfully");

            // Intentar rotar con otras ofertas
            rotate(manager, widgetIds, ctx, fav, gameBmp, stores);

        } catch (Exception e) {
            Log.e(TAG, "run error: " + e.getMessage(), e);
        }
    }

    private void rotate(AppWidgetManager manager, int[] widgetIds,
                        Context ctx, JSONObject fav,
                        Bitmap gameBmp, JSONArray stores) {
        try {
            String title   = fav.optString("title", "");
            String encoded = java.net.URLEncoder.encode(title, "UTF-8");

            Thread.sleep(2000);
            JSONArray deals = fetchArray(
                API_URL + "/deals?title=" + encoded + "&pageSize=5");

            if (deals.length() <= 1) return;

            int max = Math.min(deals.length(), 4);
            for (int r = 0; r < max; r++) {
                JSONObject deal  = deals.getJSONObject(r);
                String store     = deal.optString("storeID", "");
                String price     = deal.optString("salePrice", "0");
                String normal    = deal.optString("normalPrice", "0");
                double retail    = Double.parseDouble(normal);
                double sale      = Double.parseDouble(price);
                int pct          = retail > 0
                    ? (int) Math.round((1 - sale / retail) * 100) : 0;

                String logoUrl  = getStoreLogo(stores, store);
                Bitmap logoBmp  = getBitmap(logoUrl);

                render(manager, widgetIds, ctx, r % 2,
                    title, price, pct, gameBmp, logoBmp);

                Log.d(TAG, "Rotation[" + r + "]: $" + price + " -" + pct + "%");
                if (r < max - 1) Thread.sleep(5000);
            }

        } catch (Exception e) {
            Log.w(TAG, "rotate failed (ok): " + e.getMessage());
        }
    }

    private void render(AppWidgetManager manager, int[] widgetIds,
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
        Log.d(TAG, "render[" + viewIdx + "]: " + title + " $" + price);
    }

    private String getStoreLogo(JSONArray stores, String storeID) {
        try {
            for (int i = 0; i < stores.length(); i++) {
                JSONObject s = stores.getJSONObject(i);
                if (s.getString("storeID").equals(storeID)) {
                    return "https://www.cheapshark.com" +
                        s.getJSONObject("images").getString("logo");
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "getStoreLogo: " + e.getMessage());
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
        Log.d(TAG, "showEmpty");
    }

    private JSONArray fetchArray(String urlStr) throws Exception {
        return new JSONArray(fetch(urlStr));
    }

    private String fetch(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setRequestProperty("Accept", "application/json");
        int code = conn.getResponseCode();
        if (code == 429) {
            Log.w(TAG, "429 rate limit, waiting 4s");
            Thread.sleep(4000);
            conn.disconnect();
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            code = conn.getResponseCode();
        }
        if (code != 200) throw new Exception("HTTP " + code);
        java.util.Scanner sc = new java.util.Scanner(conn.getInputStream());
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) sb.append(sc.nextLine());
        sc.close();
        conn.disconnect();
        return sb.toString();
    }

    private Bitmap getBitmap(String urlStr) {
        try {
            if (urlStr == null || urlStr.isEmpty()) return null;
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.connect();
            InputStream is = conn.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(is);
            conn.disconnect();
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "getBitmap: " + e.getMessage());
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
