/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
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


public class EnrollmentActivity extends AppCompatActivity {


    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;


    private TextView txtProgress;
    private ProgressBar progressBar;
    private int pStatus = 0;
    private Handler handler = new Handler();


    private RelativeLayout mRelativeLayout;
    private RelativeLayout mSaveRelativeLayout;

    private ProgressDialog progressDialog = null;

    private PopupWindow mPopupWindow;

    private Button button;

    private GoogleApiClient client;

    private int retryCount = 0;

    Resources res = null;
    String[] vocabulary = null;

    private boolean enrollmentStarted = false;

    private final String TAG = "KNURLD_" + EnrollmentActivity.class.getName();
    private String enrollment;

    public void setupViews() {

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ImageButton imageButton = (ImageButton) findViewById(R.id.image_record);


        mRelativeLayout = (RelativeLayout) findViewById(R.id.rl);
        mSaveRelativeLayout = (RelativeLayout) findViewById(R.id.rl_custom_layout);


        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();

            }
        });
        res = getResources();

        progressDialog = new ProgressDialog(EnrollmentActivity.this);
        progressDialog.setTitle("Enrolling");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);

        vocabulary = res.getStringArray(R.array.vocabulary);


    }

    public void showProgress() {
        final ImageButton imageButton = (ImageButton) findViewById(R.id.image_record);
        txtProgress = (TextView) findViewById(R.id.txtProgress);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageButton.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                txtProgress.setVisibility(View.VISIBLE);

            }
        });


        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        new Thread(new Runnable() {
            @Override
            public void run() {
                while (pStatus < 9) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setProgress(pStatus * 11);
                            //txtProgress.setText(pStatus + " %");
                            txtProgress.setText(vocabulary[pStatus % vocabulary.length]);
                        }
                    });
                    try {
                        Thread.sleep(1800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    pStatus++;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopRecording();
            }
        }).start();
    }


    // The method that displays the popup.
    private void showPopup() {

        // Get a reference for the custom view close button
        Button saveButton = (Button) findViewById(R.id.ibSave);

        // Set a click listener for the popup window close button
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Dismiss the popup window
                pStatus = 0;
                // mPopupWindow.dismiss();run

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        checkPassword();
                    }
                });
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRelativeLayout.setVisibility(View.GONE);
                mSaveRelativeLayout.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkPassword() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.show();
            }
        });
        EditText etReEnterPassword = (EditText) findViewById(R.id.etReEnterPassword);
        String password = etReEnterPassword.getText().toString().trim();

        if (password.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Enter password", Toast.LENGTH_SHORT).show();
            return;
        }

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                uploadWavFile(enrollment);
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String message = KnurldRequestHelper.getErrorMessage(KnurldRequestHelper.formatVolleyErrorResponse(error));
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(EnrollmentActivity.this);
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
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.hide();
                    }
                });
            }
        };

        JSONObject parameters = new JSONObject();
        try {
            parameters.accumulate("username", KnurldRequestHelper.USERNAME);
            parameters.accumulate("password", password);
        } catch (JSONException jse) {
        }
//        RequestQueue requestQueue = Volley.newRequestQueue(EnrollmentActivity.this);
        CustomRequest jsObjRequest = new CustomRequest(Request.Method.POST, KnurldRequestHelper.USER_LOGIN_URL, parameters.toString(), responseListner, errorListener);
//        requestQueue.add(jsObjRequest);

        AlphaRequestQueue.submit(this, jsObjRequest, true);

    }


    private void startRecording() {


        pStatus = 0;
        enrollmentStarted = false;

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                KnurldAudioHelper.RECORDER_SAMPLERATE, KnurldAudioHelper.RECORDER_CHANNELS, KnurldAudioHelper.RECORDER_AUDIO_ENCODING, KnurldAudioHelper.bufferSize);

        if (recorder == null || recorder.getState() != 1)
            recorder = KnurldAudioHelper.findAudioRecord(this);

        if (recorder == null) {
            Toast.makeText(EnrollmentActivity.this, "Not able to open audio recorder!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (recorder.getState() == 1) {
            recorder.startRecording();
        }

        showProgress();

        recorder.startRecording();

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
            e.printStackTrace();
            if (AlphaBankUtil.debug)
                Toast.makeText(EnrollmentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                        if (AlphaBankUtil.debug)
                            Toast.makeText(EnrollmentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
                if (AlphaBankUtil.debug)
                    Toast.makeText(EnrollmentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void stopRecording() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageButton imageButton = (ImageButton) findViewById(R.id.image_record);
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
        enrollment = KnurldAudioHelper.getFilename("enrollment");
        KnurldAudioHelper.copyWaveFile(KnurldAudioHelper.getTempFilename(), enrollment);
        KnurldAudioHelper.deleteTempFile();
        if(KnurldRequestHelper.dropbox_access_token.isEmpty()){
            uploadWavFileOnCloud(enrollment);
        }else{
            uploadWavFile(enrollment);
        }
        //showPopup();
        retryCount = 0;
    }

    private void uploadWavFile(String enrollment) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.show();
            }
        });
        try {
            String url = DropBoxUtil.uploadFile(new FileInputStream(new File(enrollment)));
            Log.d(TAG, "Dropbox url:" + url);
            KnurldRequestHelper.AUDIO_URL = KnurldAudioHelper.makeDownladableLink(url);
            Log.d(TAG, "Dropbox download url:" + KnurldRequestHelper.AUDIO_URL);

//            if (AlphaBankUtil.debug)
//                Toast.makeText(EnrollmentActivity.this, url, Toast.LENGTH_SHORT).show();
            //uploadForIntervals();

            List<WordInterval> wordList = WordDetection.detectWordsAutoSensitivity(enrollment, vocabulary.length * 3);
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
                    interval.accumulate("phrase", vocabulary[count++ % vocabulary.length]);
                    intervals.put(interval);
                }
                jsonObject.accumulate("intervals", intervals);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            //postVerification(jsonObject);
            startEnrollment(jsonObject);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

    }

    private void uploadWavFileOnCloud(final String enrollment) {
        //RequestQueue requestQueue = Volley.newRequestQueue(EnrollmentActivity.this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.show();
            }
        });
        Response.Listener<String> responseListner = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.wtf(TAG, "Dropbox url is :" + response);
                KnurldRequestHelper.verifyResponse(EnrollmentActivity.this, response);
                KnurldRequestHelper.AUDIO_URL = KnurldAudioHelper.makeDownladableLink(response);
                Log.wtf(TAG, "Dropbox url is :" + KnurldRequestHelper.AUDIO_URL);
                if (AlphaBankUtil.debug)
                    Toast.makeText(EnrollmentActivity.this, response, Toast.LENGTH_SHORT).show();
                //uploadForIntervals();

                List<WordInterval> wordList = WordDetection.detectWordsAutoSensitivity(enrollment, 9);
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
                //postVerification(jsonObject);
                startEnrollment(jsonObject);
            }

        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                retryCount++;
                if (error.getMessage() != null) {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(EnrollmentActivity.this, "File Uploading :" + error.getMessage() == null ? "File Uploading..." : error.getMessage(), Toast.LENGTH_SHORT).show();
                }
                uploadWavFile(enrollment);
                if (retryCount == 5) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                        }
                    });
                    return;
                }
            }
        };
        MultipartRequest multipartRequest = new MultipartRequest(KnurldRequestHelper.UPLOAD_URL, responseListner, errorListener, new File(enrollment), null, null, "file", null);
        //requestQueue.add(multipartRequest);

        AlphaRequestQueue.submitMultipartRequest(this, multipartRequest, true);

    }


    private void uploadForIntervals() {
//        RequestQueue requestQueue = Volley.newRequestQueue(EnrollmentActivity.this);

        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                if (AlphaBankUtil.debug)
                    Toast.makeText(EnrollmentActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                try {
                    retryCount = 0;
                    getIntervals(response.getString("taskName"));
                } catch (JSONException e) {
                    Toast.makeText(EnrollmentActivity.this, "File Upload to knurld interval is failed", Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                        }
                    });
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(EnrollmentActivity.this, "File Upload to knurld interval is failed", Toast.LENGTH_SHORT).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.hide();
                    }
                });
            }
        };
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.accumulate("audioUrl", KnurldRequestHelper.AUDIO_URL);
            jsonObject.accumulate("words", "9");
        } catch (JSONException e) {
            if (AlphaBankUtil.debug)
                Toast.makeText(EnrollmentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        CustomRequest enrollmentRequest = new CustomRequest(Request.Method.POST, KnurldRequestHelper.ANALYTICS_ENDPOINT + "/url", jsonObject.toString(), responseListner, errorListener);

        AlphaRequestQueue.submit(this, enrollmentRequest, true);

//        if (!KnurldRequestHelper.isUserLoggedIn()) {
//            KnurldRequestHelper.Login(EnrollmentActivity.this, enrollmentRequest);
//        } else {
//            requestQueue.add(enrollmentRequest);
//        }
    }

    private void getIntervals(final String taskName) {
        if (null == taskName || taskName.isEmpty()) {
            Toast.makeText(EnrollmentActivity.this, "Invalid task...please retry!", Toast.LENGTH_SHORT).show();
            return;
        }
//        RequestQueue requestQueue = Volley.newRequestQueue(EnrollmentActivity.this);

        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                String status = null;
                try {
                    status = response.getString("taskStatus");
                } catch (JSONException e) {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(EnrollmentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                if ("completed".equals(status)) {
                    if (!enrollmentStarted) {
                        enrollmentStarted = true;
                        Log.wtf(TAG, response.toString() + " ; Enrollment Started=" + enrollmentStarted);
                        startEnrollment(response);
                    }
                    return;
                } else {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(EnrollmentActivity.this, response.toString(), Toast.LENGTH_SHORT).show();
                    retryCount++;
                    if (retryCount == 10 || "failed".equals(status)) {
                        if (AlphaBankUtil.debug)
                            Toast.makeText(EnrollmentActivity.this, "Please retry!", Toast.LENGTH_SHORT).show();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ImageButton imageButton = (ImageButton) findViewById(R.id.image_record);
                                imageButton.setVisibility(View.VISIBLE);
                                progressBar.setVisibility(View.GONE);
                                txtProgress.setVisibility(View.GONE);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressDialog.hide();
                                    }
                                });
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
                    Toast.makeText(EnrollmentActivity.this, "File Upload to knurld interval is failed:" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        CustomRequest getInterval = new CustomRequest(Request.Method.GET, KnurldRequestHelper.ANALYTICS_ENDPOINT + "/" + taskName, null, responseListner, errorListener);

        AlphaRequestQueue.submit(this, getInterval, true);
//        if (!KnurldRequestHelper.isUserLoggedIn()) {
//            KnurldRequestHelper.Login(EnrollmentActivity.this, getInterval);
//        } else {
//            requestQueue.add(getInterval);
//        }
    }

    private void startEnrollment(final JSONObject _response) {
//        RequestQueue requestQueue = Volley.newRequestQueue(EnrollmentActivity.this);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.show();
            }
        });

        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String enrollmentUrl = response.getString("href");
                    if (AlphaBankUtil.debug)
                        Toast.makeText(EnrollmentActivity.this, "EnrollmentUrl:" + enrollmentUrl, Toast.LENGTH_SHORT).show();
                    postEnrollment(enrollmentUrl, _response);
                } catch (JSONException e) {
                    Toast.makeText(EnrollmentActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                        }
                    });
                }
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (AlphaBankUtil.debug)
                    Toast.makeText(EnrollmentActivity.this, "Create enrollment is failed:" + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        String url = KnurldRequestHelper.buildUrl(KnurldRequestHelper.ENROLLMENT_ENDPOINT);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.accumulate("application", KnurldRequestHelper.APP_MODEL_URL);
            jsonObject.accumulate("consumer", KnurldRequestHelper.getFromPref(EnrollmentActivity.this, KnurldRequestHelper.CONSUMER_HREF_KEY));
        } catch (JSONException e) {
            Toast.makeText(EnrollmentActivity.this, "In create enrollment:" + e.getMessage(), Toast.LENGTH_SHORT).show();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.hide();
                }
            });
        }
        CustomRequest enrollmentRequest = new CustomRequest(Request.Method.POST, url, jsonObject.toString(), responseListner, errorListener);

//        if (!KnurldRequestHelper.isUserLoggedIn()) {
//            KnurldRequestHelper.Login(EnrollmentActivity.this, enrollmentRequest);
//        } else {
//            requestQueue.add(enrollmentRequest);
//        }
        AlphaRequestQueue.submit(this, enrollmentRequest, true);
    }

    private void postEnrollment(final String enrollmentUrl, JSONObject response) {
//        RequestQueue requestQueue = Volley.newRequestQueue(EnrollmentActivity.this);

        Response.Listener<JSONObject> responseListner = new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.hide();
                    }
                });
                Toast.makeText(EnrollmentActivity.this, "Successfully enrolled voice...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(EnrollmentActivity.this, DashboardActivity.class);
                startActivity(intent);
                // doverify enrollment at back end
            }
        };

        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.wtf(TAG, "Updated enrollment failed:" + error.getMessage(), error);
                Toast.makeText(EnrollmentActivity.this, "Updated enrollment failed:" + error.getMessage(), Toast.LENGTH_SHORT).show();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.hide();
                    }
                });
            }
        };
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.accumulate("enrollment.wav", KnurldRequestHelper.AUDIO_URL);
//            JSONArray intervals = new JSONArray();
//            JSONArray array = response.getJSONArray("intervals");
//            int intervalCount = 0;
//            if (array.length() == vocabulary.length) {
//                for (String vocab : vocabulary) {
//                    JSONObject interval = array.getJSONObject(intervalCount++);
//                    interval.accumulate("phrase", vocab);
//                    intervals.put(interval);
//                }
//            } else {
//                Log.wtf(TAG, array.toString());
//                Toast.makeText(EnrollmentActivity.this, "Something went wrong, please retry!", Toast.LENGTH_SHORT).show();
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        progressDialog.hide();
//                    }
//                });
//                return;
//            }
            jsonObject.accumulate("intervals", response.getJSONArray("intervals"));

        } catch (JSONException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.show();
                }
            });
            return;
        }
        Log.wtf(TAG, enrollmentUrl + " ->" + jsonObject.toString());
        CustomRequest enrollmentRequest = new CustomRequest(Request.Method.POST, enrollmentUrl, jsonObject.toString(), responseListner, errorListener);

//        if (!KnurldRequestHelper.isUserLoggedIn()) {
//            KnurldRequestHelper.Login(EnrollmentActivity.this, enrollmentRequest);
//        } else {
//            requestQueue.add(enrollmentRequest);
//        }
        AlphaRequestQueue.submit(this, enrollmentRequest, true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        isRecording = false;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enrollment);
        setupViews();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPopupWindow != null && mPopupWindow.isShowing()) {
            mPopupWindow.dismiss();
        }
        AlphaRequestQueue.getInstance(this).cancleAllRequest();
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
