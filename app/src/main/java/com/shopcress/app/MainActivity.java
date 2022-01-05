package com.shopcress.app;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    ProgressBar splashProgress, progressBar;
    View splashView, errorView;
    WebView webView;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        splashProgress = findViewById(R.id.splashProgress);
        progressBar = findViewById(R.id.progressBar);

        splashView = findViewById(R.id.splashView);
        textView = findViewById(R.id.textView);
        errorView = findViewById(R.id.errorView);
        webView = findViewById(R.id.webView);


        webView.setWebViewClient(new myWebViewClient());
        webView.setWebChromeClient(new MyChrome());

        String loadUrl = getIntent().getStringExtra("url");

        if (loadUrl != null) {
            webView.loadUrl(loadUrl);
        } else {
            webView.loadUrl("https://shopcress.com/");
        }



        initWebViewSettings();

        findViewById(R.id.button).setOnClickListener(view -> {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                webView.reload();
            }
            errorView.setVisibility(View.GONE);
        });

        showAdsa();

        FirebaseMessaging.getInstance().subscribeToTopic("shopcress.com");
    }

    AdView mAdView;
    boolean isVisible;
    private InterstitialAd mInterstitialAd;
    private ScheduledExecutorService scheduler;

    private void showAdsa() {

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {

            }
        });
        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        AdRequest adRequesta = new AdRequest.Builder().build();

        InterstitialAd.load(this, getResources().getString(R.string.interstialAdsId), adRequesta,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {

                        mInterstitialAd = interstitialAd;
                        Log.i("TAG", "onAdLoaded");
                        if (!isVisible) {
                            mInterstitialAd.show(MainActivity.this);
                        }


                        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                            @Override
                            public void onAdDismissedFullScreenContent() {
                                Log.d("TAG", "The ad was dismissed.");
                                isVisible = false;
                            }

                            @Override
                            public void onAdFailedToShowFullScreenContent(AdError adError) {
                                // Called when fullscreen content failed to show.
                                Log.d("TAG", "The ad failed to show.");
                            }

                            @Override
                            public void onAdShowedFullScreenContent() {
                                mInterstitialAd = null;
                                Log.d("TAG", "The ad was shown.");
                                isVisible = true;
                            }
                        });
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error
                        Log.i("TAG", loadAdError.getMessage());
                        mInterstitialAd = null;
                    }
                });


    }

    protected void onStart() {
        super.onStart();
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    Log.i("hello", "world");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            showAdsa();
                        }
                    });
                }
            }, 10, 60, TimeUnit.SECONDS);

        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        scheduler.shutdownNow();
        scheduler = null;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }

    }

    public class myWebViewClient extends WebViewClient {


        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url.startsWith("tel:") || url.startsWith("sms:") || url.startsWith("smsto:")
                    || url.startsWith("mms:") || url.startsWith("mmsto:")
                    || url.startsWith("mailto:") /*|| htmlString.contains("youtube.com")*/
                    || url.contains("geo:") || url.contains("intent:")) {
                invokeNativeApp(url);
            } else {
                view.loadUrl(url);
            }

            return true;
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {

            errorView.setVisibility(View.VISIBLE);
            textView.setText(description);

        }

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            handler.cancel();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);


        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);


        }

    }


    public class MyChrome extends WebChromeClient {

        private View mCustomView;
        private CustomViewCallback mCustomViewCallback;
        protected FrameLayout mFullscreenContainer;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;


        public Bitmap getDefaultVideoPoster() {
            if (mCustomView == null) {
                return null;
            }
            return BitmapFactory.decodeResource(getResources(), 2130837573);
        }

        public void onHideCustomView() {

            ((FrameLayout) getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            setRequestedOrientation(this.mOriginalOrientation);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

            this.mCustomViewCallback.onCustomViewHidden();
            this.mCustomViewCallback = null;
        }

        public void onShowCustomView(View paramView, CustomViewCallback paramCustomViewCallback) {
            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
            this.mOriginalOrientation = getRequestedOrientation();
            this.mCustomViewCallback = paramCustomViewCallback;
            ((FrameLayout) getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            getWindow().getDecorView().setSystemUiVisibility(3846 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);

            if (newProgress > 50) {
                progressBar.setVisibility(View.GONE);
                splashView.setVisibility(View.GONE);
            } else {
                progressBar.setVisibility(View.VISIBLE);
                splashProgress.setProgress(newProgress * 2);
                progressBar.setProgress(newProgress * 2);
            }
        }
    }


    private void initWebViewSettings() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setAppCachePath(getCacheDir().getAbsolutePath());
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.getSettings().setPluginState(WebSettings.PluginState.ON);


        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES:
                    WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                case Configuration.UI_MODE_NIGHT_UNDEFINED:
                    WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_AUTO);
                    break;
            }
        }


    }

    public void invokeNativeApp(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}