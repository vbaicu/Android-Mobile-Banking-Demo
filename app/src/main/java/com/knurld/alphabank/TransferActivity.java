/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.knurld.alphabank.com.knurld.alphabank.request.util.AlphaBankUtil;

public class TransferActivity extends AppCompatActivity {

    private Button transferButton = null;
    private EditText etAmount = null;
    private Spinner fromAccount = null;
    private Spinner toAccount = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setupViews();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    private void setupViews() {
        transferButton = (Button) findViewById(R.id.btnTransferAmount);
        etAmount = (EditText) findViewById(R.id.amount);
        fromAccount = (Spinner) findViewById(R.id.fromAccount);
        toAccount = (Spinner) findViewById(R.id.toAccount);

        transferButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String _amount = etAmount.getText().toString();
                if (_amount == null) {
                    if (AlphaBankUtil.debug)
                        Toast.makeText(TransferActivity.this, "Select add amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                TextView textView = (TextView) fromAccount.getSelectedView();
                String fromAccountNumber = textView.getText().toString();

                textView = (TextView) toAccount.getSelectedView();
                String toAccountNumber = textView.getText().toString();

                if ("Choose account".equals(fromAccountNumber)) {
                    Toast.makeText(TransferActivity.this, "Please select from account", Toast.LENGTH_SHORT).show();
                    return;
                }
                if ("Choose account".equals(toAccountNumber)) {
                    Toast.makeText(TransferActivity.this, "Please select to account", Toast.LENGTH_SHORT).show();
                    return;
                }
                int amount = Integer.parseInt(_amount);
                if (amount < 10000) {
                    if (AlphaBankUtil.debug) {
                        Toast.makeText(TransferActivity.this, "Successfully transfered amount $" + amount, Toast.LENGTH_SHORT).show();
                    }
                    Intent intent = new Intent(TransferActivity.this, DashboardActivity.class);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(TransferActivity.this, VerificationActivity.class);
                    startActivity(intent);
                }
            }
        });
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
