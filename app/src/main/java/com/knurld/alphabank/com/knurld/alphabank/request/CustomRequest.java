/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank.com.knurld.alphabank.request;

import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.knurld.alphabank.com.knurld.alphabank.request.util.KnurldRequestHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class CustomRequest extends JsonObjectRequest {

    private Listener<JSONObject> listener;
    private Map<String, String> params;

    private final String TAG = "KNURLD_" + CustomRequest.class.getName();


    public CustomRequest(int method, String url, String requestBody, Listener<JSONObject> listener, ErrorListener errorListener) {
        super(method, url, requestBody, listener, errorListener);
        Log.i(TAG, "Sending" + method + " request to server url:" + url + " body:" + requestBody + " Headers: Authorization->" + KnurldRequestHelper.authHeader + " Developer-Id:" + KnurldRequestHelper.developerId);
    }

    @Override
    protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers));
            return Response.success(new JSONObject(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        } catch (JSONException je) {
            return Response.error(new ParseError(je));
        }
    }

    //In your extended request class
    @Override
    protected VolleyError parseNetworkError(VolleyError volleyError) {
        if (volleyError.networkResponse != null && volleyError.networkResponse.data != null) {
            VolleyError error = new VolleyError(new String(volleyError.networkResponse.data));
            volleyError = error;
        }
        return volleyError;
    }


    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Authorization", KnurldRequestHelper.authHeader);
        headers.put("Developer-Id", KnurldRequestHelper.developerId);
        return headers;
    }
}
