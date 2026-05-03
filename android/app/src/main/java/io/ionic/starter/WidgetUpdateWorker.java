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
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class WidgetUpdateWorker extends Worker {

    private static final String TAG = "WidgetUpdateWorker";
    private static final String BASE_URL = "https://www.cheapshark.com/api/1.0";

    public WidgetUpdateWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork started");
        Context ctx = getApplicationContext();

        AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
        ComponentName widgetComponent = new ComponentName(ctx, GameWidget.class);
        int[] widgetIds = manager.getAppWidgetIds(widgetComponent);

        Log.d(TAG, "Found " + widgetIds.length + " widgets");

        if (widgetIds.length == 0) return Result.success();

        try {
            SharedPreferences prefs = ctx.getSharedPreferences(
                "CapacitorStorage", Context.MODE_PRIVATE
            );
            String favJson = prefs.getString("favoriteGame", null);
            Log.d(TAG, "favoriteGame: " + favJson);

            if (favJson == null) {
                showEmpty(manager, widgetIds, ctx);
                return Result.success();
            }

            JSONObject fav      = new JSONObject(favJson);
            String gameID       = fav.getString("gameID");
            String gameTitle    = fav.optString("title", "");
            String thumb        = fav.optString("thumb", "");

            // Fetch ofertas del juego
            JSONObject gameDetail = new JSONObject(fetch(BASE_URL + "/games?id=" + gameID));
            JSONArray  dealsArr   = gameDetail.getJSONArray("deals");

            // Fetch tiendas
            JSONArray storesArr = new JSONArray(fetch(BASE_URL + "/stores"));

            List<JSONObject> deals = new ArrayList<>();
            for (int i = 0; i < dealsArr.length(); i++) {
                JSONObject deal   = dealsArr.getJSONObject(i);
                String    storeID = deal.getString("storeID");
                for (int j = 0; j < storesArr.length(); j++) {
                    JSONObject store = storesArr.getJSONObject(j);
                    if (store.getString("storeID").equals(storeID)) {
                        deal.put("storeName", store.getString("storeName"));
                        deal.put("logoUrl",
                            "https://www.cheapshark.com" +
                            store.getJSONObject("images").getString("logo"));
                        break;
                    }
                }
                deal.put("gameTitle", gameTitle);
                deal.put("thumb", thumb);
                deals.add(deal);
            }

            if (deals.isEmpty()) {
                showEmpty(manager, widgetIds, ctx);
                return Result.success();
            }

            // Mostrar todas las ofertas rotando (máx 6 rotaciones = 30 seg)
            int maxRotations = Math.min(deals.size(), 6);
            for (int r = 0; r < maxRotations; r++) {
                if (isStopped()) break;
                render(manager, widgetIds, ctx, deals, r);
                if (r < maxRotations - 1) {
                    Thread.sleep(5000);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in doWork", e);
            showEmpty(manager, widgetIds, ctx);
        }

        return Result.success();
    }

    private void render(AppWidgetManager manager, int[] widgetIds,
                        Context ctx, List<JSONObject> deals, int idx) {
        try {
            JSONObject deal = deals.get(idx % deals.size());

            Bitmap gameBmp = getBitmap(deal.optString("thumb", ""));
            Bitmap logoBmp = getBitmap(deal.optString("logoUrl", ""));

            String title    = deal.optString("gameTitle", "");
            String price    = "$" + deal.optString("price", "0");
            double retail   = Double.parseDouble(deal.optString("retailPrice", "0"));
            double saleP    = Double.parseDouble(deal.optString("price", "0"));
            int    savings  = retail > 0
                ? (int) Math.round((1 - saleP / retail) * 100) : 0;

            int view = idx % 2; // alterna entre view 1 y 2

            for (int id : widgetIds) {
                RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.game_widget);
                v.setViewVisibility(R.id.viewFlipper, View.VISIBLE);
                v.setViewVisibility(R.id.emptyState,  View.GONE);
                v.setDisplayedChild(R.id.viewFlipper, view);

                int titleId = view == 0 ? R.id.gameTitle1 : R.id.gameTitle2;
                int priceId = view == 0 ? R.id.salePrice1 : R.id.salePrice2;
                int savId   = view == 0 ? R.id.savings1   : R.id.savings2;
                int imgId   = view == 0 ? R.id.gameImage1 : R.id.gameImage2;
                int logoId  = view == 0 ? R.id.storeLogo1 : R.id.storeLogo2;

                v.setTextViewText(titleId, title);
                v.setTextViewText(priceId, price);
                v.setTextViewText(savId,   "-" + savings + "%");
                if (gameBmp != null) v.setImageViewBitmap(imgId,  gameBmp);
                if (logoBmp != null) v.setImageViewBitmap(logoId, logoBmp);

                manager.updateAppWidget(id, v);
                Log.d(TAG, "Rendered deal[" + idx + "]: " + title + " " + price);
            }
        } catch (Exception e) {
            Log.e(TAG, "render error", e);
        }
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

    private String fetch(String urlStr) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
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
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.connect();
            InputStream is = conn.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(is);
            conn.disconnect();
            return bmp;
        } catch (Exception e) {
            Log.e(TAG, "getBitmap error: " + urlStr, e);
            return null;
        }
    }
}