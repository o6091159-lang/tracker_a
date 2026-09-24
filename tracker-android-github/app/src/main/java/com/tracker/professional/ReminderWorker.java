package com.tracker.professional;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Runs about every 30 minutes in the background. Mirrors the reminder rules of the web page:
 * daily shortfall alerts (after the chosen hour, once per macro) and the periodic meal reminder.
 * It stays silent while the app is open, because the page handles reminders itself then.
 */
public class ReminderWorker extends Worker {

    public ReminderWorker(Context context, WorkerParameters params) {
        super(context, params);
    }

    @Override
    public Result doWork() {
        try {
            run(getApplicationContext());
        } catch (Exception ignored) {
        }
        return Result.success();
    }

    private static void run(Context ctx) throws Exception {
        if (MainActivity.inForeground) return;

        SharedPreferences p = ctx.getSharedPreferences(TrackerBridge.PREFS, Context.MODE_PRIVATE);
        String raw = p.getString("payload", null);
        if (raw == null) return;
        JSONObject o = new JSONObject(raw);
        if (!o.optBoolean("enabled", false)) return;
        if (!NotificationHelper.canPost(ctx)) return;

        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        String today = String.format(Locale.US, "%04d-%02d-%02d",
                now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH));
        boolean fresh = today.equals(o.optString("date", ""));

        cleanOldFired(p, today);

        // 1) Daily shortfall alerts, once per macro per day, after the chosen hour.
        if (fresh && hour >= o.optInt("reminderHour", 20)) {
            JSONArray pending = o.optJSONArray("pending");
            if (pending != null && pending.length() > 0) {
                Set<String> fired = new HashSet<String>();
                Set<String> saved = p.getStringSet("fired:" + today, null);
                if (saved != null) fired.addAll(saved);
                boolean changed = false;
                for (int i = 0; i < pending.length(); i++) {
                    JSONObject a = pending.getJSONObject(i);
                    String key = a.optString("key", "");
                    if (key.length() == 0 || fired.contains(key)) continue;
                    String title = a.optString("title", "");
                    if (NotificationHelper.show(ctx, title, a.optString("body", ""), title.hashCode())) {
                        fired.add(key);
                        changed = true;
                    }
                }
                if (changed) p.edit().putStringSet("fired:" + today, fired).apply();
            }
        }

        // 2) Periodic meal reminder.
        if (o.optBoolean("meal", false)) {
            int quietStart = o.optInt("quietStart", 8);
            int quietEnd = o.optInt("quietEnd", 23);
            if (hour >= quietStart && hour < quietEnd) {
                double intervalHours = Math.max(1.0, o.optDouble("intervalHours", 3.0));
                long intervalMs = (long) (intervalHours * 3600000.0);
                long nowMs = System.currentTimeMillis();
                long lastMealAt = o.optLong("lastMealAt", 0L);
                long lastReminderAt = Math.max(o.optLong("lastReminderAt", 0L), p.getLong("lastReminderAt", 0L));

                boolean ateRecently = lastMealAt > 0 && (nowMs - lastMealAt) < intervalMs;
                boolean remindedRecently = (nowMs - lastReminderAt) < intervalMs;
                if (!ateRecently && !remindedRecently) {
                    // A snapshot from an earlier day means nothing was logged today yet: every goal is short.
                    List<String> shorts = labels(o.optJSONArray(fresh ? "shortLabels" : "goalLabels"));
                    boolean posted;
                    if (!shorts.isEmpty()) {
                        posted = NotificationHelper.show(ctx,
                                "حان وقت الاهتمام بتغذيتك",
                                "أنت في عجز عن هدف " + join(shorts) + " اليوم. تناول وجبة أو وجبة خفيفة تساعد على تعويض النقص.",
                                "meal-reminder".hashCode());
                    } else {
                        String body;
                        if (lastMealAt > 0) {
                            long hours = Math.round((nowMs - lastMealAt) / 3600000.0);
                            body = "لم تسجّل وجبة منذ " + hours + " ساعة تقريبًا. لا تنسَ وجبتك القادمة.";
                        } else {
                            body = "لم تسجّل أي وجبة بعد. لا تنسَ وجبتك القادمة.";
                        }
                        posted = NotificationHelper.show(ctx, "حان وقت وجبة", body, "meal-reminder".hashCode());
                    }
                    if (posted) p.edit().putLong("lastReminderAt", nowMs).apply();
                }
            }
        }
    }

    private static List<String> labels(JSONArray arr) {
        List<String> out = new ArrayList<String>();
        if (arr == null) return out;
        for (int i = 0; i < arr.length(); i++) {
            String s = arr.optString(i, "");
            if (s.length() > 0) out.add(s);
        }
        return out;
    }

    private static String join(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append("، ");
            sb.append(items.get(i));
        }
        return sb.toString();
    }

    private static void cleanOldFired(SharedPreferences p, String today) {
        SharedPreferences.Editor editor = null;
        for (Map.Entry<String, ?> entry : p.getAll().entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("fired:") && !key.equals("fired:" + today)) {
                if (editor == null) editor = p.edit();
                editor.remove(key);
            }
        }
        if (editor != null) editor.apply();
    }
}
