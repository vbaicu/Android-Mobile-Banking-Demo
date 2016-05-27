/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank.com.knurld.alphabank.request.util;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.knurld.alphabank.R;
import com.knurld.alphabank.com.knurld.alphabank.request.AdminLoginRequest;
import com.knurld.alphabank.com.knurld.alphabank.request.AlphaRequestQueue;
import com.knurld.alphabank.com.knurld.alphabank.request.CustomRequest;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ndarade on 4/25/16.
 */
public class KnurldRequestHelper {


    public static String HOST, V1, AUTH_URL, USER_LOGIN_URL, APP_MODEL_ID, PREFS_NAME, APP_MODEL_URL, ANALYTICS_ENDPOINT;
    public static String VERIFICATION_URL, masterDeveloperId, developerId, clientId, client_secret, dropbox_access_token;
    public static String knurldBearerConst = "Bearer: ";
    public static String authHeader = null;
    public static long st;
    public static String VERIFICAION_HREF = "";
    public static String AUDIO_URL = "";
    public static String CONSUMER_HREF_KEY = "CONSUMER_HREF";
    public static String CONSUMER_ENDPOINT = "/consumers";
    public static String UPLOAD_URL = "https://voicetools-api.knurld-demo.com/rest/file/upload";
    public static String ENROLLMENT_ENDPOINT = "/enrollments";
    public static String VERIFICAION_ENDPOINT = "/verifications";

    public static String CONSUMER_NAME_KEY = "CONSUMER_NAME_KEY";
    public static String USERNAME = null;

    public static void initApp(Resources resources) {
        HOST = resources.getString(R.string.host);
        V1 = resources.getString(R.string.api_version);
        AUTH_URL = HOST + "/oauth/client_credential/accesstoken?grant_type=client_credentials";
        USER_LOGIN_URL = HOST + V1 + CONSUMER_ENDPOINT + "/token";
        APP_MODEL_ID = resources.getString(R.string.app_model);
        PREFS_NAME = "KnurldPrefFile";
        APP_MODEL_URL = HOST + "/app-models/" + APP_MODEL_ID;
        ANALYTICS_ENDPOINT = HOST + V1 + "/endpointAnalysis";
        masterDeveloperId = knurldBearerConst + resources.getString(R.string.developer_id);
        developerId = masterDeveloperId;
        clientId = resources.getString(R.string.client_id);
        client_secret = resources.getString(R.string.client_secret);
        dropbox_access_token = resources.getString(R.string.dropbox_access_token);
    }

    public static String buildUrl(String endpoint) {
        return HOST + V1 + endpoint;
    }


    public static JSONObject formatVolleyErrorResponse(VolleyError volleyError) {
        if (volleyError != null) {
            String message = volleyError.getMessage();
            if (message != null) {
                try {
                    return new JSONObject(message);
                } catch (JSONException je) {
                    // IGNORE
                }
            }
        }
        return null;
    }

    public static String getErrorMessage(JSONObject jsonObject) {
        if (jsonObject != null) {
            try {
                return jsonObject.getString("message");
            } catch (JSONException e) {
                // IGNORE
            }
        }
        return "Unknown error!";
    }

    public static int getErrorCode(JSONObject jsonObject) {
        if (jsonObject != null) {
            try {
                return jsonObject.getInt("statusCode");
            } catch (JSONException e) {
                // IGNORE
            }
        }
        return -1;
    }

    public static String getOauthUrl() {
        return AUTH_URL;
    }

    public static boolean isUserLoggedIn() {
        if (authHeader == null) {
            return false;
        }
        if (st == 0) {
            return false;
        }
        if (!((System.currentTimeMillis() - st) < (45 * 60 * 1000))) {
            return false;
        }
        return true;
    }

    public static void Login(final Context context, final CustomRequest request) {
        //final RequestQueue requestQueue = Volley.newRequestQueue(context);

        Response.Listener<String> responseListner = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject jsonObject = new JSONObject(response);
                    authHeader = "Bearer " + jsonObject.getString("access_token");
                    if (request != null) {
                        //requestQueue.add(request);
                        AlphaRequestQueue.submit(context, request, false);

                    }
                } catch (JSONException jse) {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(context, jse.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = KnurldRequestHelper.getErrorMessage(KnurldRequestHelper.formatVolleyErrorResponse(error));
                if (AlphaBankUtil.debug)
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                request.getErrorListener().onErrorResponse(error);
            }
        };
        AdminLoginRequest adminLoginRequest = new AdminLoginRequest(clientId, client_secret, responseListner, errorListener);
        //requestQueue.add(adminLoginRequest);
        AlphaRequestQueue.submitAdminLogin(context, adminLoginRequest);
    }


    public static void verifyResponse(final Context context, String response) {

    }

    public static String getFromPref(final Context context, String key) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        return settings.getString(key, "");
    }

    public static void setInPref(final Context context, String key, String value) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void invalidateCache() {
        authHeader = null;
        developerId = masterDeveloperId;
    }

    public static boolean checkInternetConnection(final Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle("No Internet Connection")
                    .setMessage("Sorry, no internet connectivity detected. Please reconnect and try again.")
                    .setCancelable(true)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
            alertDialog.create().show();
            return false;
        } else {
            return true;
        }
    }
}
