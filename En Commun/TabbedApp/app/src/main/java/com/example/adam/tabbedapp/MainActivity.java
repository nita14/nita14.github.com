package com.example.adam.tabbedapp;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.MenuItem;
import android.view.View;

import android.view.inputmethod.InputMethodManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Switch;

public class MainActivity extends AppCompatActivity {
    public static String userName;
    public static String userPhone;
    public static String age = "2";
    public static String sex = "male";
    public static String carModel;
    public static String carReg;
    public static int curCap = 0;
    public static int maxCap = 6;
    public static int rating = 4;
    public static final int REQUEST_IMAGE_CAPTURE = 1;
    public static Bitmap photo;
    public static boolean isDriver = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbarApp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                takePhoto(view);
            }
        });


        //driver
        Switch sw = (Switch) findViewById(R.id.isDriver);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    findViewById(R.id.rel_model).setVisibility(LinearLayout.VISIBLE);
                    findViewById(R.id.rel_reg_num).setVisibility(LinearLayout.VISIBLE);
                    findViewById(R.id.rel_places).setVisibility(LinearLayout.VISIBLE);
                    findViewById(R.id.rel_places_max).setVisibility(LinearLayout.VISIBLE);

                } else {
                    findViewById(R.id.rel_model).setVisibility(LinearLayout.GONE);
                    findViewById(R.id.rel_reg_num).setVisibility(LinearLayout.GONE);
                    findViewById(R.id.rel_places).setVisibility(LinearLayout.GONE);
                    findViewById(R.id.rel_places_max).setVisibility(LinearLayout.GONE);
                }
            }
        });

        //get owner name
        final AccountManager manager = AccountManager.get(this);
        final Account[] accounts = manager.getAccountsByType("com.google");
        final int size = accounts.length;
        String[] names = new String[size];
        for (int i = 0; i < size; i++) {
            names[i] = accounts[i].name;
        }

        EditText nameText = (EditText) findViewById(R.id.login);
        nameText.setText(names[0]);
        //fill phone number
        TelephonyManager tMgr = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        EditText phoneText = (EditText) findViewById(R.id.phone);

        phoneText.setText(tMgr.getLine1Number());
        //hide keyboard
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        //set default
        ((EditText) findViewById(R.id.phone)).setHint(R.string.string_phone);
        ((EditText) findViewById(R.id.regNum)).setHint(R.string.string_regNumber);
        ((EditText) findViewById(R.id.model)).setHint(R.string.edit_car);

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //send settings
    public void sendSettings(View view) {

        EditText editText = (EditText) findViewById(R.id.login);
        Switch sw = (Switch) findViewById(R.id.isDriver);
        userName = editText.getText().toString();
        EditText userPhoneText = (EditText) findViewById(R.id.phone);
        userPhone = userPhoneText.getText().toString();

        if (sw.isChecked()) {
            EditText carModelText = (EditText) findViewById(R.id.model);
            carModel = carModelText.getText().toString();

            EditText carRegText = (EditText) findViewById(R.id.regNum);
            carReg = carRegText.getText().toString();
            isDriver = true;
        }

        if(!checkData()){
            Intent intent = new Intent(this, DisplayMap.class);
            startActivity(intent);
        }
    }

    public void onRadioSexClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        switch (view.getId()) {
            case R.id.radio_female:
                if (checked)
                    sex = "Female";
                break;
            case R.id.radio_male:
                if (checked)
                    sex = "Male";
                break;
        }
    }

    public void onRadioAgeClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radio_ageTo25:
                if (checked)
                    age = "0";
                break;
            case R.id.radio_ageTo65:
                if (checked)
                    age = "1";
                break;
            case R.id.radio_ageOver65:
                if (checked)
                    age = "2";
                break;
        }
    }

    public void onRadioMaxCapClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.edit_maxCap2:
                if (checked)
                    maxCap = 2;
                break;
            case R.id.edit_maxCap3:
                if (checked)
                    maxCap = 3;
                break;
            case R.id.edit_maxCap4:
                if (checked)
                    maxCap = 4;
                break;
            case R.id.edit_maxCap5:
                if (checked)
                    maxCap = 5;
                break;
            case R.id.edit_maxCap6:
                if (checked)
                    maxCap = 6;
                break;

        }
    }

    public void onRadioCurCapClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch (view.getId()) {
            case R.id.edit_curCap0:
                if (checked)
                    curCap = 0;
                break;
            case R.id.edit_curCap1:
                if (checked)
                    curCap = 1;
                break;
            case R.id.edit_curCap2:
                if (checked)
                    curCap = 2;
                break;
            case R.id.edit_curCap3:
                if (checked)
                    curCap = 3;
                break;
            case R.id.edit_curCap4:
                if (checked)
                    curCap = 4;
                break;
        }
    }

    //photo
    public void takePhoto(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            photo = (Bitmap) extras.get("data");
            ImageView imgView = (ImageView) findViewById(R.id.photoView);
            imgView.setImageBitmap(photo);
        }
    }

    public boolean checkData(){
        if(userName==null || userPhone==null){
            showWarning(R.string.noUserData);
            return true;
        }

        if(photo==null){
            showWarning(R.string.noPhoto);
            return true;
        }

        if(isDriver && (carModel.isEmpty() || carReg.isEmpty())){
            showWarning(R.string.noCarData);
            return true;
        }
        return false;


    }

    public void showWarning(int messsage){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle(R.string.warning);
        alertDialog.setMessage(getResources().getString(messsage));
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        return;
                    }
                });
        alertDialog.show();

    }
}