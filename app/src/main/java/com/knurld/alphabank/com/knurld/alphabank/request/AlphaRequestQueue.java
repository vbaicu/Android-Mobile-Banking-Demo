/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank.com.knurld.alphabank.request;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.knurld.alphabank.EnrollmentActivity;
import com.knurld.alphabank.LoginActivity;
import com.knurld.alphabank.com.knurld.alphabank.request.util.KnurldRequestHelper;

/**
 * Created by ndarade on 4/29/16.
 */
public class AlphaRequestQueue {
    private static final String TAG = "KnulrdRequests";
    private static AlphaRequestQueue singletonAlphaRequestQueue = null;
    private RequestQueue mRequestQueue;
    private static Context mCtx;

    public static AlphaRequestQueue getInstance(Context context) {
        if (singletonAlphaRequestQueue == null) {
            singletonAlphaRequestQueue = new AlphaRequestQueue(context);
        }
        return singletonAlphaRequestQueue;
    }

    private AlphaRequestQueue(Context context) {
        mCtx = context;
        mRequestQueue = getRequestQueue();
    }

    public RequestQueue getRequestQueue() {
        if (mRequestQueue == null) {
            // getApplicationContext() is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
        }
        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        req.addMarker(TAG);
        getRequestQueue().add(req);
    }

    public void cancleAllRequest() {
        getRequestQueue().cancelAll(TAG);
    }


    public static void submit(Context context, CustomRequest jsObjRequest, boolean checkLogin) {
        if (!KnurldRequestHelper.isUserLoggedIn() && checkLogin) {
            KnurldRequestHelper.Login(context, jsObjRequest);
        } else {
            AlphaRequestQueue.getInstance(context).addToRequestQueue(jsObjRequest);
        }
    }

    public static void submitMultipartRequest(Context context, MultipartRequest multipartRequest, boolean checkLogin) {
        AlphaRequestQueue.getInstance(context).addToRequestQueue(multipartRequest);
    }

    public static void submitAdminLogin(Context context, AdminLoginRequest adminLoginRequest) {
        AlphaRequestQueue.getInstance(context).addToRequestQueue(adminLoginRequest);

    }
}
