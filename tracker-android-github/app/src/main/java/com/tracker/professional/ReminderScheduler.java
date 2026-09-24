package com.tracker.professional;

import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

final class ReminderScheduler {
    private static final String WORK_NAME = "tracker-reminders";

    private ReminderScheduler() {}

    static void schedule(Context ctx) {
        PeriodicWorkRequest request =
                new PeriodicWorkRequest.Builder(ReminderWorker.class, 30, TimeUnit.MINUTES).build();
        WorkManager.getInstance(ctx.getApplicationContext())
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    static void cancel(Context ctx) {
        WorkManager.getInstance(ctx.getApplicationContext()).cancelUniqueWork(WORK_NAME);
    }
}
