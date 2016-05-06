/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.knurld.alphabank.com.knurld.alphabank.request.AlphaRequestQueue;
import com.knurld.alphabank.com.knurld.alphabank.request.CustomRequest;
import com.knurld.alphabank.com.knurld.alphabank.request.util.AlphaBankUtil;
import com.knurld.alphabank.com.knurld.alphabank.request.util.KnurldRequestHelper;

import org.json.JSONException;
import org.json.JSONObject;

import android.view.MenuItem;

public class RegisterActivity extends AppCompatActivity {
    private EditText etUsername;
    private EditText etPassword;
    ProgressDialog progressDialog;

    private final String TAG = "KNURLD_" + RegisterActivity.class.getName();


    private void setupViews() {
        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassword = (EditText) findViewById(R.id.etPassword);

        Button registerButton = (Button) findViewById(R.id.btnRegister);
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = etUsername.getText().toString().trim();
                final String password = etPassword.getText().toString().trim();

                if (username.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Enter username", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Enter password", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!KnurldRequestHelper.checkInternetConnection(RegisterActivity.this)) {
                    return;
                }
                Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            KnurldRequestHelper.setInPref(RegisterActivity.this, KnurldRequestHelper.CONSUMER_HREF_KEY, response.getString("href"));
                            KnurldRequestHelper.setInPref(RegisterActivity.this, KnurldRequestHelper.CONSUMER_NAME_KEY, username);
                        } catch (JSONException e) {
                            Log.wtf(TAG, e.getMessage());
                        }
                        progressDialog.dismiss();
                        Toast.makeText(getApplicationContext(), "Registration success !", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                    }
                };

                Response.ErrorListener errorListener = new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        String message = KnurldRequestHelper.getErrorMessage(KnurldRequestHelper.formatVolleyErrorResponse(error));
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(RegisterActivity.this);
                        alertDialog.setTitle("User registraion failed")
                                .setMessage(message)
                                .setCancelable(true)
                                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                });
                        alertDialog.create().show();
                    }
                };

                JSONObject parameters = new JSONObject();
                try {
                    parameters.accumulate("username", username);
                    parameters.accumulate("password", password);
                    parameters.accumulate("gender", "M");
                } catch (JSONException jse) {
                }
                String url = KnurldRequestHelper.buildUrl(KnurldRequestHelper.CONSUMER_ENDPOINT);
//                RequestQueue requestQueue = Volley.newRequestQueue(RegisterActivity.this);
                CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, url, parameters.toString(), responseListner, errorListener);

                progressDialog = new ProgressDialog(RegisterActivity.this);
                progressDialog.setTitle("Register");
                progressDialog.setMessage("Please wait while we register you!");
                progressDialog.setCancelable(false);
                progressDialog.show();

                AlphaRequestQueue.submit(RegisterActivity.this, jsObjRequest, true);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AlphaRequestQueue.getInstance(this).cancleAllRequest();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupViews();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return super.onOptionsItemSelected(item);
    }

}
