package com.tracker.professional;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Set;

/** Exposed to the web page as window.TrackerAndroid. */
public class TrackerBridge {
    static final String PREFS = "tracker_reminders";

    private final MainActivity activity;
    private final Context appContext;

    TrackerBridge(MainActivity activity) {
        this.activity = activity;
        this.appContext = activity.getApplicationContext();
    }

    @JavascriptInterface
    public boolean hasPermission() {
        return NotificationHelper.canPost(appContext);
    }

    @JavascriptInterface
    public void requestPermission() {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.requestNotificationPermission();
            }
        });
    }

    @JavascriptInterface
    public boolean show(String title, String body) {
        String t = title == null ? "" : title;
        String b = body == null ? "" : body;
        return NotificationHelper.show(appContext, t, b, t.hashCode());
    }

    /** Receives the reminder settings/state from the page and (un)schedules the background check. */
    @JavascriptInterface
    public void sync(String json) {
        try {
            JSONObject o = new JSONObject(json);
            appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .edit().putString("payload", json).apply();
            if (o.optBoolean("enabled", false)) {
                ReminderScheduler.schedule(appContext);
            } else {
                ReminderScheduler.cancel(appContext);
            }
        } catch (Exception ignored) {
        }
    }

    /** Returns what the background worker already did today: {"lastReminderAt":123,"fired":["calories"]}. */
    @JavascriptInterface
    public String getState(String date) {
        try {
            SharedPreferences p = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            JSONObject out = new JSONObject();
            out.put("lastReminderAt", p.getLong("lastReminderAt", 0L));
            JSONArray fired = new JSONArray();
            Set<String> set = p.getStringSet("fired:" + date, null);
            if (set != null) {
                for (String key : set) fired.put(key);
            }
            out.put("fired", fired);
            return out.toString();
        } catch (Exception e) {
            return "{}";
        }
    }
}
