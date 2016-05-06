/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.knurld.alphabank.com.knurld.alphabank.request.AlphaRequestQueue;
import com.knurld.alphabank.com.knurld.alphabank.request.CustomRequest;
import com.knurld.alphabank.com.knurld.alphabank.request.util.AlphaBankUtil;
import com.knurld.alphabank.com.knurld.alphabank.request.util.KnurldRequestHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername;
    private EditText etPassword;
    ProgressDialog progressDialog;
    private final String TAG = "KNURLD_" + LoginActivity.class.getName();

    private void setupViews() {

        KnurldRequestHelper.initApp(getResources());
        //invalidate cache
        KnurldRequestHelper.invalidateCache();
        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassword = (EditText) findViewById(R.id.etPassword);

        Button loginButton = (Button) findViewById(R.id.btnSignin);
        //Button registerButton = (Button) findViewById(R.id.btnRegister);

        ImageView imageButton = (ImageView) findViewById(R.id.knurld_logo);

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                KnurldRequestHelper.invalidateCache();
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = etUsername.getText().toString().trim();
                if (username.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Enter username", Toast.LENGTH_SHORT).show();
                    return;
                }

                String password = etPassword.getText().toString().trim();
                if (password.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Enter password", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!KnurldRequestHelper.checkInternetConnection(LoginActivity.this)) {
                    return;
                }

                Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String token = response.getString("token");
                            KnurldRequestHelper.developerId = KnurldRequestHelper.knurldBearerConst + token;
                            KnurldRequestHelper.USERNAME = username;

                            checkUserIsValid(username);
                        } catch (JSONException e) {
                            progressDialog.dismiss();
                            if (AlphaBankUtil.debug)
                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }


                };

                Response.ErrorListener errorListener = new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        String message = KnurldRequestHelper.getErrorMessage(KnurldRequestHelper.formatVolleyErrorResponse(error));
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(LoginActivity.this);
                        alertDialog.setTitle("User Login failed")
                                .setMessage(message)
                                .setCancelable(true)
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });
                        alertDialog.create().show();
                        progressDialog.dismiss();
                    }
                };

                JSONObject parameters = new JSONObject();
                try {
                    parameters.accumulate("username", username);
                    parameters.accumulate("password", password);
                } catch (JSONException jse) {
                }
                //RequestQueue requestQueue = Volley.newRequestQueue(LoginActivity.this);
                CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, KnurldRequestHelper.USER_LOGIN_URL, parameters.toString(), responseListner, errorListener);

                //showing loading dialog
                progressDialog = new ProgressDialog(LoginActivity.this);
                progressDialog.setTitle("Login");
                progressDialog.setMessage("Please wait while we validate your credentials");
                progressDialog.setCancelable(false);

                progressDialog.show();

//                if (!KnurldRequestHelper.isUserLoggedIn()) {
//                    KnurldRequestHelper.Login(LoginActivity.this, jsObjRequest);
//                } else {
//                    requestQueue.add(jsObjRequest);
//                }
                AlphaRequestQueue.submit(LoginActivity.this, jsObjRequest, true);
            }
        });


//        registerButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                KnurldRequestHelper.invalidateCache();
//                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
//                startActivity(intent);
//            }
//        });

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, AlphaBankUtil.REQUEST_WRITE_STORAGE);
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, AlphaBankUtil.REQUEST_RECORD_AUDIO);
            return;
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AlphaRequestQueue.getInstance(this).cancleAllRequest();

    }

    private void checkUserIsValid(final String username) {

//        String cachedUsername = KnurldRequestHelper.getFromPref(LoginActivity.this, KnurldRequestHelper.CONSUMER_NAME_KEY);
//        if (username.equals(cachedUsername)) {
//            checkEnrollmentIsDoneOrNot();
//            return;
//        }
        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                boolean isEnrolled = false;
                if (response != null) {
                    KnurldRequestHelper.verifyResponse(LoginActivity.this, response.toString());
                    try {
                        JSONObject user = response.getJSONArray("items").getJSONObject(0);
                        String href = user.getString("href");
                        isEnrolled = !user.isNull("lastCompletedEnrollment");
                        Log.i(TAG, "New consumer href->" + href);
                        KnurldRequestHelper.setInPref(LoginActivity.this, KnurldRequestHelper.CONSUMER_HREF_KEY, href);
                        KnurldRequestHelper.setInPref(LoginActivity.this, KnurldRequestHelper.CONSUMER_NAME_KEY, username);

                    } catch (Exception jse) {
                        Log.wtf(TAG, "Not able to parse the get consumer api response", jse);
                    }
                    if (isEnrolled) {
                        Log.i(TAG, "User already enrolled so opening dashboard activity...");
                        Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                        startActivity(intent);
                    } else {
                        Log.i(TAG, "User not enrolled so opening enrollment activity...");
                        Intent intent = new Intent(LoginActivity.this, EnrollmentActivity.class);
                        startActivity(intent);
                    }
                    //checkEnrollmentIsDoneOrNot();

                }
                Log.i(TAG, response.toString());
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();

                JSONObject response = KnurldRequestHelper.formatVolleyErrorResponse(error);
                int statusCode = KnurldRequestHelper.getErrorCode(response);
                if (statusCode == 400 || statusCode == 401) {
                    Intent intent = new Intent(LoginActivity.this, EnrollmentActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginActivity.this, "Unknown error, please contact administrator statusCode:" + statusCode + " Message:" + KnurldRequestHelper.getErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }
        };

        JSONObject parameters = new JSONObject();
        //RequestQueue requestQueue = Volley.newRequestQueue(LoginActivity.this);
        String url = KnurldRequestHelper.buildUrl(KnurldRequestHelper.CONSUMER_ENDPOINT);

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, url, parameters.toString(), responseListner, errorListener);
//        if (!KnurldRequestHelper.isUserLoggedIn()) {
//            KnurldRequestHelper.Login(LoginActivity.this, jsObjRequest);
//        } else {
//            requestQueue.add(jsObjRequest);
//        }
        AlphaRequestQueue.submit(this, jsObjRequest, true);
    }


    private void checkEnrollmentIsDoneOrNot() {

        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    KnurldRequestHelper.VERIFICAION_HREF = response.getString("href");
                    Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
                    startActivity(intent);
                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                }
                progressDialog.dismiss();
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progressDialog.dismiss();

                JSONObject response = KnurldRequestHelper.formatVolleyErrorResponse(error);
                int statusCode = KnurldRequestHelper.getErrorCode(response);
                if (statusCode == 400 || statusCode == 401) {
                    Intent intent = new Intent(LoginActivity.this, EnrollmentActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(LoginActivity.this, "Unknown error, please contact administrator statusCode:" + statusCode + " Message:" + KnurldRequestHelper.getErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }
        };

        JSONObject parameters = new JSONObject();
        try {
            parameters.accumulate("application", KnurldRequestHelper.APP_MODEL_URL);
            parameters.accumulate("consumer", KnurldRequestHelper.getFromPref(LoginActivity.this, KnurldRequestHelper.CONSUMER_HREF_KEY));
        } catch (JSONException jse) {
            progressDialog.dismiss();

        }
        //RequestQueue requestQueue = Volley.newRequestQueue(LoginActivity.this);
        String url = KnurldRequestHelper.buildUrl(KnurldRequestHelper.VERIFICAION_ENDPOINT);

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, url, parameters.toString(), responseListner, errorListener);

        AlphaRequestQueue.submit(LoginActivity.this, jsObjRequest, true);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
        if (requestCode == AlphaBankUtil.REQUEST_RECORD_AUDIO) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Record audio phone granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Record audio phone denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }

        if (requestCode == AlphaBankUtil.REQUEST_WRITE_STORAGE) {
            if (grantResults.length == 1 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Write to storage permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Write to storage permission denied", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupViews();
    }
}
