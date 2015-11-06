package com.firebase.ui.authimpl;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTwitter = new TwitterFactory(new ConfigurationBuilder()
                .setOAuthConsumerKey("OnDqMFmCtY4uNiXBv3FLwg")
                .setOAuthConsumerSecret("GgJnJiYSUiHUJ5pCGJ1XQCL0yUsy8G8eBBW2LnAqaQ")
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
                    Log.e(TAG, te.toString());
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
                                Intent resultIntent = new Intent();
                                setResult(TwitterAuthHelper.RC_TWITTER_CANCEL, resultIntent);
                                finish();
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
                    Log.e(TAG, te.toString());
                }
                return accessToken;
            }

            @Override
            protected void onPostExecute(AccessToken token) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("oauth_token", token.getToken());
                resultIntent.putExtra("oauth_token_secret", token.getTokenSecret());
                resultIntent.putExtra("user_id", token.getUserId() + "");
                Log.d(TAG, token.getUserId() + "");
                setResult(TwitterAuthHelper.RC_TWITTER_LOGIN, resultIntent);
                finish();
            }
        }.execute();
    }
}