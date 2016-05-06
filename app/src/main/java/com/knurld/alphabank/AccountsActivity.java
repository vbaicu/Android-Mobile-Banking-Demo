/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class AccountsActivity extends AppCompatActivity {

    private ListView transactions = null;
//    private ArrayList<Transaction> transactionsList;

    private final String TAG = "KNURLD_" + AccountsActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //fillDummyData();

        setupViews();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

    }

    private void setupViews() {

//        transactions = (ListView) findViewById(R.id.lvTransactions);
//        CustomAdapter adapter = new CustomAdapter(transactionsList);
//
//        transactions.setAdapter(adapter);

    }

//    private void fillDummyData() {
//
//        transactionsList = new ArrayList<>();
//
//        Transaction tr1 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_CREDIT, 100);
//        Transaction tr2 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_DEBIT, 1321);
//        Transaction tr3 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_CREDIT, 4324);
//        Transaction tr4 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_DEBIT, 543);
//        Transaction tr5 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_CREDIT, 75);
//        Transaction tr6 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_DEBIT, 678);
//        Transaction tr8 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_CREDIT, 5425);
//        Transaction tr9 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_DEBIT, 5425);
//        Transaction tr10 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_DEBIT, 5425);
//        Transaction tr11 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_CREDIT, 5425);
//        Transaction tr12 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_DEBIT, 5425);
//        Transaction tr13 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_DEBIT, 5425);
//        Transaction tr14 = new Transaction(TRANSACTION_TYPE.TRANSACTION_TYPE_CREDIT, 5425);
//
//        transactionsList.add(tr1);
//        transactionsList.add(tr2);
//        transactionsList.add(tr3);
//        transactionsList.add(tr4);
//        transactionsList.add(tr5);
//        transactionsList.add(tr6);
//        transactionsList.add(tr8);
//        transactionsList.add(tr9);
//        transactionsList.add(tr10);
//        transactionsList.add(tr11);
//        transactionsList.add(tr12);
//        transactionsList.add(tr13);
//        transactionsList.add(tr14);
//
//    }
//
//    public enum TRANSACTION_TYPE {
//        TRANSACTION_TYPE_CREDIT,
//        TRANSACTION_TYPE_DEBIT
//
//    }
//
//    class Transaction {
//        private TRANSACTION_TYPE transactionType;
//
//        public Transaction(TRANSACTION_TYPE transactionType, int amount) {
//            this.transactionType = transactionType;
//            this.amount = amount;
//        }
//
//        public int getAmount() {
//            return amount;
//        }
//
//        public void setAmount(int amount) {
//            this.amount = amount;
//        }
//
//        private int amount;
//
//        public TRANSACTION_TYPE getTransactionType() {
//            return transactionType;
//        }
//
//        public void setTransactionType(TRANSACTION_TYPE transactionType) {
//            this.transactionType = transactionType;
//        }
//
//    }
//
//    class CustomAdapter extends BaseAdapter {
//
//        private List<Transaction> transactionList;
//
//        public CustomAdapter(List<Transaction> transactionList) {
//            this.transactionList = transactionList;
//        }
//
//        @Override
//        public int getCount() {
//            return transactionList.size();
//        }
//
//        @Override
//        public Object getItem(int i) {
//            return transactionsList.get(i);
//        }
//
//        @Override
//        public long getItemId(int i) {
//            return i;
//        }
//
//        @Override
//        public View getView(int i, View view, ViewGroup viewGroup) {
//            LayoutInflater layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            TextView v = (TextView) layoutInflater.inflate(android.R.layout.simple_list_item_1, null, false);
//            Transaction tr = transactionList.get(i);
//            switch (tr.getTransactionType()) {
//                case TRANSACTION_TYPE_CREDIT:
//                    v.setTextColor(Color.GREEN);
//                    v.setText("Cr. $" + tr.getAmount());
//                    break;
//                case TRANSACTION_TYPE_DEBIT:
//                    v.setTextColor(Color.RED);
//                    v.setText("Db. $" + tr.getAmount());
//                    break;
//            }
//            return v;
//        }
//    }

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
