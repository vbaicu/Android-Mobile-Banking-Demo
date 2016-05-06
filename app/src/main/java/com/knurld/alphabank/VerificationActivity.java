/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.knurld.alphabank.com.knurld.alphabank.request.AlphaRequestQueue;
import com.knurld.alphabank.com.knurld.alphabank.request.CustomRequest;
import com.knurld.alphabank.com.knurld.alphabank.request.MultipartRequest;
import com.knurld.alphabank.com.knurld.alphabank.request.util.AlphaBankUtil;
import com.knurld.alphabank.com.knurld.alphabank.request.util.DropBoxUtil;
import com.knurld.alphabank.com.knurld.alphabank.request.util.KnurldAudioHelper;
import com.knurld.alphabank.com.knurld.alphabank.request.util.KnurldRequestHelper;
import com.knurld.alphabank.com.knurld.vad.WordDetection;
import com.knurld.alphabank.com.knurld.vad.WordInterval;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;


public class VerificationActivity extends AppCompatActivity {


    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    ImageButton imageButton = null;


    private TextView txtProgress;
    private ProgressBar progressBar;
    private int pStatus = 0;
    private Handler handler = new Handler();


    ProgressDialog progressDialog;
    //ProgressDialog progressDialogForRepeat;

    private int retryCount = 0;
    private int verificationRetryCount = 0;
    Resources res = null;
    String[] vocabulary = new String[3];

    private boolean verificationStarted = false;
    private final String TAG = "KNURLD_" + VerificationActivity.class.getName();
    private String verification;

    private boolean retry = false;

    private GoogleApiClient client;

    public void setupViews() {
        res = getResources();
        imageButton = (ImageButton) findViewById(R.id.image_record);
        txtProgress = (TextView) findViewById(R.id.txtProgress);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        progressDialog = new ProgressDialog(VerificationActivity.this);
        progressDialog.setTitle("Verification");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        progressDialog.show();

        createVerificationObject();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        verificationRetryCount = 0;

        final ImageButton imageButton = (ImageButton) findViewById(R.id.image_record);
        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (retry) {
                    progressDialog.show();
                    createVerificationObject();
                    return;
                }
                retry = true;
                startRecording();

            }
        });
    }


    public void createVerificationObject() {
        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    KnurldRequestHelper.VERIFICAION_HREF = response.getString("href");
                    getVerificationObject(false);
                } catch (JSONException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();

                        }
                    });
                    if (AlphaBankUtil.debug)
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();

                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();

                    }
                });
                JSONObject response = KnurldRequestHelper.formatVolleyErrorResponse(error);
                int statusCode = KnurldRequestHelper.getErrorCode(response);
                if (statusCode == 400 || statusCode == 401) {
                    Intent intent = new Intent(VerificationActivity.this, EnrollmentActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(VerificationActivity.this, "Unknown error, please contact administrator statusCode:" + statusCode + " Message:" + KnurldRequestHelper.getErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }
        };

        JSONObject parameters = new JSONObject();
        try {
            parameters.accumulate("application", KnurldRequestHelper.APP_MODEL_URL);
            parameters.accumulate("consumer", KnurldRequestHelper.getFromPref(VerificationActivity.this, KnurldRequestHelper.CONSUMER_HREF_KEY));
        } catch (JSONException jse) {
            //progressDialog.dismiss();

        }
        String url = KnurldRequestHelper.buildUrl(KnurldRequestHelper.VERIFICAION_ENDPOINT);

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, url, parameters.toString(), responseListner, errorListener);
        AlphaRequestQueue.submit(this, jsObjRequest, true);

    }

    private void getVerificationObject(final boolean getTillFinished) {
        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.wtf(TAG, "Response for get verification ->" + response.toString());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
                try {
                    if (!getTillFinished) {
                        JSONArray jsonArray = response.getJSONObject("instructions").getJSONObject("data").getJSONArray("phrases");
                        int length = jsonArray.length();
                        if (jsonArray != null) {
                            for (int i = 0; i < length; i++) {
                                vocabulary[i] = jsonArray.getString(i);
                            }
                        }
                        if (retry) {
                            startRecording();
                        }
                    } else {
                        verificationRetryCount++;
                        if (verificationRetryCount == 10) {
                            if (AlphaBankUtil.debug)
                                Toast.makeText(VerificationActivity.this, "Please retry!", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            return;
                        }
                        String status = response.getString("status");
                        if ("completed".equals(status)) {

                            if (response.getBoolean("verified")) {
                                Toast.makeText(VerificationActivity.this, "Transaction successfull!", Toast.LENGTH_SHORT).show();
                                KnurldRequestHelper.VERIFICAION_HREF = "";
                                Intent intent = new Intent(VerificationActivity.this, DashboardActivity.class);
                                startActivity(intent);
                                progressDialog.dismiss();
                                return;
                            } else {
                                Toast.makeText(VerificationActivity.this, "Voice authentication failed!", Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                                return;
                            }
                        }
                        if ("failed".equals(status)) {
                            Toast.makeText(VerificationActivity.this, "Please retry again!", Toast.LENGTH_SHORT).show();
                            progressDialog.dismiss();
                            return;
                        }
                        getVerificationObject(getTillFinished);

                    }
                } catch (JSONException e) {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();

                        }
                    });
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();

                    }
                });
                JSONObject response = KnurldRequestHelper.formatVolleyErrorResponse(error);
                int statusCode = KnurldRequestHelper.getErrorCode(response);
                if (statusCode == 400 || statusCode == 401) {

                    // createVerificationObject(true);
                } else {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(VerificationActivity.this, "Unknown error, please contact administrator statusCode:" + statusCode + " Message:" + KnurldRequestHelper.getErrorMessage(response), Toast.LENGTH_SHORT).show();
                }
            }
        };

        JSONObject parameters = new JSONObject();

        CustomRequest jsObjRequest = new CustomRequest(Request.Method.GET, KnurldRequestHelper.VERIFICAION_HREF, parameters.toString(), responseListner, errorListener);
        AlphaRequestQueue.submit(this, jsObjRequest, true);
    }


    public void showProgress() {
        final ImageButton imageButton = (ImageButton) findViewById(R.id.image_record);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageButton.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                txtProgress.setVisibility(View.VISIBLE);

            }
        });
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (pStatus < 3) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(pStatus * 33);
                            //txtProgress.setText(pStatus + " %");
                            txtProgress.setText(vocabulary[pStatus]);
                        }
                    });
                    try {
                        Thread.sleep(1800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    pStatus++;
                }
                stopRecording();
            }
        }).start();
    }


    private void startRecording() {
        showProgress();
        pStatus = 0;
        verificationStarted = false;

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                KnurldAudioHelper.RECORDER_SAMPLERATE, KnurldAudioHelper.RECORDER_CHANNELS, KnurldAudioHelper.RECORDER_AUDIO_ENCODING, KnurldAudioHelper.bufferSize);

        if (recorder == null || recorder.getState() != 1)
            recorder = KnurldAudioHelper.findAudioRecord(this);

        if (recorder == null) {
            Toast.makeText(VerificationActivity.this, "Not able to open audio recorder!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (recorder.getState() == 1) {
            recorder.startRecording();
        }

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[KnurldAudioHelper.bufferSize];
        String filename = KnurldAudioHelper.getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            if (AlphaBankUtil.debug)
                Toast.makeText(VerificationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                }
            });
            return;
        }

        int read = 0;

        if (null != os) {
            while (isRecording) {
                read = recorder.read(data, 0, KnurldAudioHelper.bufferSize);

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                            }
                        });
                        return;
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
            }
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();
                }
            });
        }
    }


    private void stopRecording() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.show();
                imageButton.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
                txtProgress.setVisibility(View.GONE);

            }
        });

        if (null != recorder) {
            isRecording = false;

            int i = recorder.getState();
            if (i == 1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }
        verification = KnurldAudioHelper.getFilename("verification");
        KnurldAudioHelper.copyWaveFile(KnurldAudioHelper.getTempFilename(), verification);
        KnurldAudioHelper.deleteTempFile();


        retryCount = 0;
        if (KnurldRequestHelper.dropbox_access_token.isEmpty()) {
            uploadWavFileCloud(verification);
        } else {
            uploadWavFile(verification);
        }
    }


    private void uploadWavFile(final String verification) {


        try {
            String url = DropBoxUtil.uploadFile(new FileInputStream(new File(verification)));
            Log.d(TAG, "Dropbox url:" + url);
            KnurldRequestHelper.VERIFICATION_URL = KnurldAudioHelper.makeDownladableLink(url);
            Log.d(TAG, "Dropbox download url:" + KnurldRequestHelper.VERIFICATION_URL);

//            if (AlphaBankUtil.debug)
//                Toast.makeText(EnrollmentActivity.this, url, Toast.LENGTH_SHORT).show();
            //uploadForIntervals();

            List<WordInterval> wordList = WordDetection.detectWordsAutoSensitivity(verification, 3);

            if (wordList.size() == 0) {
                uploadForIntervals();
                return;
            }
            JSONObject jsonObject = new JSONObject();
            JSONArray intervals = new JSONArray();
            int count = 0;
            try {
                for (WordInterval words :
                        wordList) {
                    JSONObject interval = new JSONObject();
                    if (words.getStopTime() - words.getStartTime() < 600) {
                        interval.accumulate("stop", words.getStartTime() + 601);
                    } else {
                        interval.accumulate("stop", words.getStopTime());
                    }
                    interval.accumulate("start", words.getStartTime());
                    interval.accumulate("phrase", vocabulary[count++]);
                    intervals.put(interval);
                }
                jsonObject.accumulate("intervals", intervals);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            postVerification(jsonObject);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void uploadWavFileCloud(final String filePath) {

        Response.Listener<String> responseListner = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.wtf(TAG, "Dropbox url is :" + response);
                KnurldRequestHelper.verifyResponse(VerificationActivity.this, response);
                KnurldRequestHelper.VERIFICATION_URL = KnurldAudioHelper.makeDownladableLink(response);
                Log.wtf(TAG, "Dropbox url is :" + KnurldRequestHelper.VERIFICATION_URL);
                if (AlphaBankUtil.debug)
                    Toast.makeText(VerificationActivity.this, response, Toast.LENGTH_SHORT).show();
                List<WordInterval> wordList = WordDetection.detectWordsAutoSensitivity(filePath, 3);
                JSONObject jsonObject = new JSONObject();
                JSONArray intervals = new JSONArray();
                int count = 0;
                try {
                    for (WordInterval words :
                            wordList) {
                        JSONObject interval = new JSONObject();

                        if (words.getStopTime() - words.getStartTime() < 600) {
                            interval.accumulate("stop", words.getStartTime() + 601);
                        } else {
                            interval.accumulate("stop", words.getStopTime());
                        }

                        interval.accumulate("start", words.getStartTime());
                        interval.accumulate("phrase", vocabulary[count++]);
                        intervals.put(interval);

                    }
                    jsonObject.accumulate("intervals", intervals);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                postVerification(jsonObject);
                //uploadForIntervals();
            }

        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                retryCount++;
                if (error.getMessage() != null) {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(VerificationActivity.this, "File Uploading :" + error.getMessage() == null ? "File Uploading..." : error.getMessage(), Toast.LENGTH_SHORT).show();
                }
                uploadWavFile(filePath);
                if (retryCount == 5) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                        }
                    });
                    return;
                }
            }
        };
        MultipartRequest multipartRequest = new MultipartRequest(KnurldRequestHelper.UPLOAD_URL, responseListner, errorListener, new File(filePath), null, null, "file", null);
        AlphaRequestQueue.submitMultipartRequest(this, multipartRequest, false);
    }

    private void uploadForIntervals() {
        retryCount = 0;

        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (AlphaBankUtil.debug)
                    Toast.makeText(VerificationActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                try {

                    getIntervals(response.getString("taskName"));
                } catch (JSONException e) {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(VerificationActivity.this, "File Upload to knurld interval is failed", Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                if (AlphaBankUtil.debug)
                    Toast.makeText(VerificationActivity.this, "File Upload to knurld interval is failed", Toast.LENGTH_SHORT).show();
            }
        };
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.accumulate("audioUrl", KnurldRequestHelper.VERIFICATION_URL);
            jsonObject.accumulate("words", "3");
        } catch (JSONException e) {
            if (AlphaBankUtil.debug)
                Toast.makeText(VerificationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
        CustomRequest verificationRequest = new CustomRequest(Request.Method.POST, KnurldRequestHelper.ANALYTICS_ENDPOINT + "/url", jsonObject.toString(), responseListner, errorListener);
        AlphaRequestQueue.submit(this, verificationRequest, true);
    }

    private void getIntervals(final String taskName) {
        if (null == taskName || taskName.isEmpty()) {
            if (AlphaBankUtil.debug)
                Toast.makeText(VerificationActivity.this, "Invalid task...please retry!", Toast.LENGTH_SHORT).show();
            return;
        }

        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String status = null;
                try {
                    status = response.getString("taskStatus");
                } catch (JSONException e) {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(VerificationActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                if ("completed".equals(status)) {
                    if (!verificationStarted) {
                        verificationStarted = true;
                        Log.wtf(TAG, response.toString() + " ; Verification Started=" + verificationStarted);
                        postVerification(response);
                    }
                    return;
                } else {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(VerificationActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                    retryCount++;
                    if (retryCount == 10 || "failed".equals(status)) {
                        if (AlphaBankUtil.debug)
                            Toast.makeText(VerificationActivity.this, "Please retry!", Toast.LENGTH_SHORT).show();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.dismiss();
                            }
                        });
                        return;
                    } else {
                        getIntervals(taskName);
                    }
                }
            }

        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (AlphaBankUtil.debug)
                    Toast.makeText(VerificationActivity.this, "File Upload to knurld interval is failed:" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        CustomRequest getInterval = new CustomRequest(Request.Method.GET, KnurldRequestHelper.ANALYTICS_ENDPOINT + "/" + taskName, null, responseListner, errorListener);
        AlphaRequestQueue.submit(this, getInterval, true);
    }

    private void postVerification(JSONObject response) {

        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                getVerificationObject(true);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.wtf(TAG, "Updated verification failed:" + error.getMessage(), error);
                if (AlphaBankUtil.debug)
                    Toast.makeText(VerificationActivity.this, "Updated verification failed:" + error.getMessage(), Toast.LENGTH_SHORT).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();

                    }
                });
                //createVerificationObject(true);
            }
        };
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.accumulate("verification.wav", KnurldRequestHelper.VERIFICATION_URL);
            jsonObject.accumulate("intervals", response.getJSONArray("intervals"));

//            JSONArray intervals = new JSONArray();
//            JSONArray array = response.getJSONArray("intervals");
//            if (array.length() == vocabulary.length) {
//                for (int i = 0; i < vocabulary.length; i++) {
//                    JSONObject interval = array.getJSONObject(i);
//                    interval.accumulate("phrase", vocabulary[i]);
//                    intervals.put(interval);
//                }
//                jsonObject.accumulate("intervals", intervals);
//            } else {
//                Log.wtf(TAG, array.toString());
//                if (AlphaBankUtil.debug)
//                    Toast.makeText(VerificationActivity.this, "Something went wrong, please retry!", Toast.LENGTH_SHORT).show();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        progressDialog.dismiss();
//
//                    }
//                });
//                return;
//
//            }


        } catch (JSONException e) {
            Log.wtf(TAG, e.getMessage());
            if (AlphaBankUtil.debug)
                Toast.makeText(VerificationActivity.this, "Something went wrong, please retry!", Toast.LENGTH_SHORT).show();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.dismiss();

                }
            });
            return;

        }
        String url = KnurldRequestHelper.VERIFICAION_HREF;
        Log.wtf(TAG, url + " ->" + jsonObject.toString());
        CustomRequest verificationRequest = new CustomRequest(Request.Method.POST, url, jsonObject.toString(), responseListner, errorListener);
        AlphaRequestQueue.submit(this, verificationRequest, true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isRecording = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupViews();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AlphaRequestQueue.getInstance(this).cancleAllRequest();

    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Verification Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.knurld.alphabank/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Verification Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.knurld.alphabank/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_enrollment, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        if (item.getItemId() != android.R.id.home) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }
}
