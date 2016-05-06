/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank.com.knurld.alphabank.request;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.knurld.alphabank.com.knurld.alphabank.request.util.KnurldRequestHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ndarade on 4/26/16.
 */
public class AdminLoginRequest extends StringRequest {


    private static final String OAUTH_URL = KnurldRequestHelper.getOauthUrl();
    private HashMap<String, String> params;


    public AdminLoginRequest(String clientId, String client_secret, Response.Listener<String> successListener, Response.ErrorListener errorListener) {
        super(Method.POST, OAUTH_URL, successListener, errorListener);
        params=new HashMap<>();
        params.put("client_id", clientId);
        params.put("client_secret", client_secret);
    }

    @Override
    public HashMap<String, String> getParams() {
        return params;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        Map<String, String> headers = new HashMap<>();
        headers.put("ContentType", "application/x-www-form-urlencoded");
        return headers;
    }
}
