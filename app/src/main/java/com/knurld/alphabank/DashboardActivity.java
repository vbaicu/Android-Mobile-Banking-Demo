/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank;

import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.knurld.alphabank.com.knurld.alphabank.request.util.KnurldRequestHelper;

public class DashboardActivity extends AppCompatActivity {

    private Button btnAccounts = null;
    private Button btnTransfer = null;
    private Button btnBillPay = null;
    private Button btnDeposit = null;
    private TextView welcomeMessage = null;
    private ImageButton re_enroll = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupViews();
    }

    private void setupViews() {

        btnAccounts = (Button) findViewById(R.id.btnAccount);
        btnTransfer = (Button) findViewById(R.id.btnTransfer);
        btnBillPay = (Button) findViewById(R.id.btnBillPay);
        btnDeposit = (Button) findViewById(R.id.btnDeposit);
        re_enroll = (ImageButton) findViewById(R.id.re_enroll);

        welcomeMessage = (TextView) findViewById(R.id.welcomeMessage);

        re_enroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, EnrollmentActivity.class);
                startActivity(intent);
            }
        });

        btnAccounts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, AccountsActivity.class);
                startActivity(intent);
            }
        });

        btnTransfer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this, TransferActivity.class);
                startActivity(intent);
            }
        });

        btnBillPay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotImplementedDialog();
            }
        });

        btnDeposit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showNotImplementedDialog();
            }
        });
        welcomeMessage.setText("Welcome, " + KnurldRequestHelper.USERNAME);

    }

    public void showNotImplementedDialog() {
        String message = "This function is not available as this is demo app";
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(DashboardActivity.this);
        alertDialog.setTitle("Sonr demo")
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
