package com.tracker.professional;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class MainActivity extends Activity {
    private static final int REQ_NOTIFICATIONS = 1001;

    /** True while the app is on screen; the background reminder worker stays quiet then. */
    static volatile boolean inForeground = false;

    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        window.setStatusBarColor(0xFF111827);
        window.setNavigationBarColor(0xFF111827);

        NotificationHelper.ensureChannel(this);

        webView = new WebView(this);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        settings.setTextZoom(100);

        webView.addJavascriptInterface(new TrackerBridge(this), "TrackerAndroid");

        webView.loadUrl("file:///android_asset/index.html");
        setContentView(webView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        inForeground = true;
    }

    @Override
    protected void onPause() {
        inForeground = false;
        super.onPause();
    }

    /** Called (on the UI thread) by the web page when the user turns notifications on. */
    void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        } else {
            deliverPermissionResult(NotificationHelper.canPost(this));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            deliverPermissionResult(granted && NotificationHelper.canPost(this));
        }
    }

    private void deliverPermissionResult(boolean granted) {
        if (webView == null) return;
        webView.evaluateJavascript(
                "window.__trackerPermResult && window.__trackerPermResult(" + (granted ? "true" : "false") + ")",
                null);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
