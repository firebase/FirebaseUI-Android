package com.firebase.ui.auth.twitter;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.firebase.ui.auth.core.FirebaseActions;
import com.firebase.ui.auth.core.FirebaseErrors;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.conf.ConfigurationBuilder;

public class TwitterPromptActivity extends Activity {
    private static final String TAG = TwitterPromptActivity.class.getSimpleName();
    private Twitter mTwitter;
    private WebView mTwitterView;

    @Override
    public void onBackPressed() {
        sendResultError(FirebaseActions.USER_ERROR, FirebaseErrors.LOGIN_CANCELLED, "User closed login prompt.");
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String twitterKey = "";
        String twitterSecret = "";

        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = ai.metaData;
            twitterKey = bundle.getString("com.firebase.ui.TwitterKey");
            twitterSecret = bundle.getString("com.firebase.ui.TwitterSecret");
        } catch (PackageManager.NameNotFoundException e) {
        } catch (NullPointerException e) {}

        if (twitterKey == null || twitterSecret == null) {
            sendResultError(FirebaseActions.PROVIDER_ERROR, FirebaseErrors.MISSING_PROVIDER_APP_KEY, "Missing Twitter key/secret, are they set in your AndroidManifest.xml?");
            return;
        }

        if (twitterKey.compareTo("") == 0|| twitterSecret.compareTo("") == 0) {
            sendResultError(FirebaseActions.PROVIDER_ERROR, FirebaseErrors.INVALID_PROVIDER_APP_KEY, "Invalid Twitter key/secret, are they set in your res/values/strings.xml?");
            return;
        }

        mTwitter = new TwitterFactory(new ConfigurationBuilder()
                .setOAuthConsumerKey(twitterKey)
                .setOAuthConsumerSecret(twitterSecret)
                .build()).getInstance();

        // setup ic_twitter webview
        mTwitterView = new WebView(this);
        mTwitterView.getSettings().setJavaScriptEnabled(true);

        // initialize view
        setContentView(mTwitterView);

        // fetch the oauth request token then prompt the user to authorize the application
        new AsyncTask<Void, Void, RequestToken>() {
            @Override
            protected RequestToken doInBackground(Void... params) {
                RequestToken token = null;
                try {
                    token = mTwitter.getOAuthRequestToken("oauth://cb");
                } catch (TwitterException te) {
                    sendResultError(FirebaseActions.PROVIDER_ERROR, FirebaseErrors.MISC_PROVIDER_ERROR, te.toString());
                }
                return token;
            }

            @Override
            protected void onPostExecute(final RequestToken token) {
                mTwitterView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(final WebView view, final String url) {
                        if (url.startsWith("oauth://cb")) {
                            mTwitterView.destroy();
                            if (url.contains("oauth_verifier")) {
                                getTwitterOAuthTokenAndLogin(token, Uri.parse(url).getQueryParameter("oauth_verifier"));
                            } else if (url.contains("denied")) {
                                sendResultError(FirebaseActions.USER_ERROR, FirebaseErrors.LOGIN_CANCELLED, "User denied access to their account.");
                            }
                        }
                    }
                });
                mTwitterView.loadUrl(token.getAuthorizationURL());
            }
        }.execute();
    }

    private void getTwitterOAuthTokenAndLogin(final RequestToken requestToken, final String oauthVerifier) {
        new AsyncTask<Void, Void, AccessToken>() {
            @Override
            protected AccessToken doInBackground(Void... params) {
                AccessToken accessToken = null;
                try {
                    accessToken = mTwitter.getOAuthAccessToken(requestToken, oauthVerifier);
                } catch (TwitterException te) {
                    sendResultError(FirebaseActions.PROVIDER_ERROR, FirebaseErrors.MISC_PROVIDER_ERROR, te.toString());
                }
                return accessToken;
            }

            @Override
            protected void onPostExecute(AccessToken token) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("oauth_token", token.getToken());
                resultIntent.putExtra("oauth_token_secret", token.getTokenSecret());
                resultIntent.putExtra("user_id", token.getUserId() + "");

                setResult(FirebaseActions.SUCCESS, resultIntent);
                finish();
            }
        }.execute();
    }

    private void sendResultError(Integer status, int errCode, String err) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("error", err);
        resultIntent.putExtra("code", errCode);
        setResult(status, resultIntent);
        finish();
    }
}