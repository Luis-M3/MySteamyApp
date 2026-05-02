package io.ionic.starter;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WidgetUpdateService extends Service {

    private static final String BASE_URL = "https://www.cheapshark.com/api/1.0";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<JSONObject> deals = new ArrayList<>();
    private int currentDealIndex = 0;
    private Runnable rotateRunnable;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        executor.execute(this::loadDataAndUpdate);
        return START_STICKY;
    }

    private void loadDataAndUpdate() {
        try {
            SharedPreferences prefs = getSharedPreferences("CapacitorStorage", MODE_PRIVATE);
            String favJson = prefs.getString("favoriteGame", null);

            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            ComponentName widgetComponent = new ComponentName(this, GameWidget.class);
            int[] widgetIds = manager.getAppWidgetIds(widgetComponent);

            if (favJson == null) {
                for (int id : widgetIds) {
                    RemoteViews views = new RemoteViews(getPackageName(), R.layout.game_widget);
                    views.setViewVisibility(R.id.viewFlipper, android.view.View.GONE);
                    views.setViewVisibility(R.id.emptyState, android.view.View.VISIBLE);
                    manager.updateAppWidget(id, views);
                }
                return;
            }

            JSONObject fav = new JSONObject(favJson);
            String gameID = fav.getString("gameID");

            // Fetch deals for the game
            String dealsUrl = BASE_URL + "/games?id=" + gameID;
            JSONObject gameDetail = new JSONObject(fetchUrl(dealsUrl));
            JSONArray dealsArray = gameDetail.getJSONArray("deals");

            // Fetch stores
            JSONArray storesArray = new JSONArray(fetchUrl(BASE_URL + "/stores"));

            deals.clear();
            for (int i = 0; i < dealsArray.length(); i++) {
                JSONObject deal = dealsArray.getJSONObject(i);
                String storeID = deal.getString("storeID");

                for (int j = 0; j < storesArray.length(); j++) {
                    JSONObject store = storesArray.getJSONObject(j);
                    if (store.getString("storeID").equals(storeID)) {
                        deal.put("storeName", store.getString("storeName"));
                        String logoPath = store.getJSONObject("images").getString("logo");
                        deal.put("logoUrl", "https://www.cheapshark.com" + logoPath);
                        break;
                    }
                }
                deal.put("gameTitle", fav.getString("title"));
                deal.put("thumb", fav.getString("thumb"));
                deals.add(deal);
            }

            if (!deals.isEmpty()) {
                updateWidgetWithDeal(manager, widgetIds, 0);
                startRotation(manager, widgetIds);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateWidgetWithDeal(AppWidgetManager manager, int[] widgetIds, int index) {
        try {
            JSONObject deal = deals.get(index % deals.size());
            Bitmap gameBmp = downloadBitmap(deal.optString("thumb", ""));
            Bitmap logoBmp = downloadBitmap(deal.optString("logoUrl", ""));

            String title = deal.optString("gameTitle", "");
            String price = "$" + deal.optString("price", "");
            double retailD = Double.parseDouble(deal.optString("retailPrice", "0"));
            double priceD = Double.parseDouble(deal.optString("price", "0"));
            int savingsPct = retailD > 0 ? (int) Math.round((1 - priceD / retailD) * 100) : 0;

            for (int id : widgetIds) {
                RemoteViews views = new RemoteViews(getPackageName(), R.layout.game_widget);
                views.setViewVisibility(R.id.viewFlipper, android.view.View.VISIBLE);
                views.setViewVisibility(R.id.emptyState, android.view.View.GONE);

                int viewIdx = index % 2;
                int titleId = viewIdx == 0 ? R.id.gameTitle1 : R.id.gameTitle2;
                int priceId = viewIdx == 0 ? R.id.salePrice1 : R.id.salePrice2;
                int savId = viewIdx == 0 ? R.id.savings1 : R.id.savings2;
                int imgId = viewIdx == 0 ? R.id.gameImage1 : R.id.gameImage2;
                int logoId = viewIdx == 0 ? R.id.storeLogo1 : R.id.storeLogo2;

                views.setTextViewText(titleId, title);
                views.setTextViewText(priceId, price);
                views.setTextViewText(savId, "-" + savingsPct + "%");
                if (gameBmp != null) views.setImageViewBitmap(imgId, gameBmp);
                if (logoBmp != null) views.setImageViewBitmap(logoId, logoBmp);

                views.setDisplayedChild(R.id.viewFlipper, viewIdx);
                manager.updateAppWidget(id, views);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startRotation(AppWidgetManager manager, int[] widgetIds) {
        if (rotateRunnable != null) handler.removeCallbacks(rotateRunnable);
        rotateRunnable = new Runnable() {
            @Override
            public void run() {
                currentDealIndex = (currentDealIndex + 1) % deals.size();
                updateWidgetWithDeal(manager, widgetIds, currentDealIndex);
                handler.postDelayed(this, 5000);
            }
        };
        handler.postDelayed(rotateRunnable, 5000);
    }

    private String fetchUrl(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.connect();
        java.util.Scanner sc = new java.util.Scanner(conn.getInputStream());
        StringBuilder sb = new StringBuilder();
        while (sc.hasNextLine()) sb.append(sc.nextLine());
        return sb.toString();
    }

    private Bitmap downloadBitmap(String urlStr) {
        try {
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.connect();
            InputStream is = conn.getInputStream();
            return BitmapFactory.decodeStream(is);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rotateRunnable != null) handler.removeCallbacks(rotateRunnable);
        executor.shutdown();
    }
}