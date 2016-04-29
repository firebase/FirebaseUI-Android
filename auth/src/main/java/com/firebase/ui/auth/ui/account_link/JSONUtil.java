package com.firebase.ui.auth.ui.account_link;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

class JSONUtil {
    private static final String FACEBOOK_SETTINGS_KEY = "facebook";
    private static final String GOOGLE_SETTINGS_KEY = "google";
    private static final String FACEBOOK_APPLICATION_ID = "application_id";
    private static final String GOOGLE_CLIENT_ID = "google_client_id";
    private static final String TAG = "JSONUtil";
    private static final String SETTINGS_FILE_NAME = "settings.json";


    public static JSONObject getJSON(Context context, String fileName) {
        BufferedReader reader = null;
        StringBuilder JSONStringBuilder = new StringBuilder();
        try {
            reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open(fileName)));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                JSONStringBuilder.append(mLine);
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }

        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(JSONStringBuilder.toString());
        } catch (JSONException e) {
            jsonObject = new JSONObject();
            e.printStackTrace();
        }

        return jsonObject;
    }

    public static String getGoogleClientId(Context context) {
        try {
            return getJSON(context, SETTINGS_FILE_NAME).getJSONObject(GOOGLE_SETTINGS_KEY)
                    .optString(GOOGLE_CLIENT_ID);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Unable to parse Google Client ID token");
        }
        return null;
    }

    public static String getFacebookApplicationId(Context context) {
        try {
            return getJSON(context, "settings.json")
                    .getJSONObject(FACEBOOK_SETTINGS_KEY).optString(FACEBOOK_APPLICATION_ID);
        } catch (JSONException e) {
            Log.e(TAG, "Unable to parse Facebook Application Id");
            e.printStackTrace();
        }
        return null;
    }
}
