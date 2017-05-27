package com.bergereden.wardialerfree;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CallLog;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MainActivity extends Activity {

    private AdView mAdView;
    TextView textOutput;
    CheckBox checkBox;
    Button buttonStart, buttonOpen;
    AudioManager manager;
    EditText rangeStart, rangeEnd, areacodeStart, areacodeEnd;
    Intent call = new Intent(Intent.ACTION_CALL);
    NumberPicker secondsPicker;
    File directoryPath = new File(Environment.getExternalStoragePublicDirectory("Documents"), "WardialScans");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final InterstitialAd mInterstitialAd;
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getResources().getString(R.string.interstitial_ad_unit_id));
        AdRequest adRequestInter = new AdRequest.Builder().build();
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                mInterstitialAd.show();
            }
        });
        mInterstitialAd.loadAd(adRequestInter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            boolean hasPermissionPhoneState = (ContextCompat.checkSelfPermission(getApplicationContext(),
                    Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
            if (!hasPermissionPhoneState) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }

        final SharedPreferences sharedPreferences = this.getSharedPreferences("", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!directoryPath.exists()) directoryPath.mkdirs();

        if (!sharedPreferences.contains("areacodeStart")) {
            editor.putString("areacodeStart", "00");
        }
        if (!sharedPreferences.contains("areacodeEnd")) {
            editor.putString("areacodeEnd", "00");
        }
        if (!sharedPreferences.contains("rangeStart")) {
            editor.putString("rangeStart", "000000");
        }
        if (!sharedPreferences.contains("rangeEnd")) {
            editor.putString("rangeEnd", "000000");
        }
        if (!sharedPreferences.contains("seconds")) {
            editor.putInt("seconds", 9);
        }
        editor.apply();

        textOutput = (TextView) findViewById(R.id.output);
        buttonStart = (Button) findViewById(R.id.start);
        buttonOpen = (Button) findViewById(R.id.open);
        areacodeStart = (EditText) findViewById(R.id.areacodeStart);
        areacodeStart.requestFocus();
        areacodeEnd = (EditText) findViewById(R.id.areacodeEnd);
        rangeStart = (EditText) findViewById(R.id.rangeStart);
        rangeEnd = (EditText) findViewById(R.id.rangeEnd);
        checkBox = (CheckBox) findViewById(R.id.stop);
        secondsPicker = (NumberPicker) findViewById(R.id.secondsPicker);
        manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        areacodeStart.setText(sharedPreferences.getString("areacodeStart", null));
        areacodeEnd.setText(sharedPreferences.getString("areacodeEnd", null));
        rangeStart.setText(sharedPreferences.getString("rangeStart", null));
        rangeEnd.setText(sharedPreferences.getString("rangeEnd", null));
        textOutput.setText(readFromFile(areacodeStart.getText().toString() + rangeStart.getText().toString()));
        textOutput.setMovementMethod(new ScrollingMovementMethod());

        secondsPicker.setMinValue(4);
        secondsPicker.setMaxValue(30);
        secondsPicker.setValue(sharedPreferences.getInt("seconds", 9));

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        areacodeStart.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                areacodeEnd.setText(areacodeStart.getText().toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        rangeStart.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                rangeEnd.setText(rangeStart.getText().toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        buttonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    showError("Not enough permissions", "I know");
                } else if (TextUtils.isEmpty(rangeStart.getText().toString()) || TextUtils.isEmpty(areacodeStart.getText().toString())) {
                    showError("Please set range", "Ok");
                } else {
                    openFile(areacodeStart.getText().toString() + rangeStart.getText().toString());
                }
            }
        });
        checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    textOutput.append("Please wait for it to finish.\n");
                }
            }
        });
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    showError("Not enough permissions", "I know");
                } else if (TextUtils.isEmpty(rangeStart.getText().toString()) || TextUtils.isEmpty(areacodeStart.getText().toString())) {
                    showError("Please set range", "Ok");
                } else {
                    try {
                        new Thread(new Runnable() {
                            public void run() {
                                textOutput.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        textOutput.setText("Starting:\n");
                                    }
                                });
                                final String areaStart = areacodeStart.getText().toString();
                                final String numStart = areaStart + rangeStart.getText().toString();
                                final String numEnd = areacodeEnd.getText().toString() + rangeEnd.getText().toString();
                                long longStart = 0;
                                try {
                                    longStart = Long.valueOf(rangeStart.getText().toString());
                                } catch (Exception e) {
                                    Log.e("Oh... ", "Something is definitely wrong");
                                }
                                final int seconds = secondsPicker.getValue();
                                editor.putInt("seconds", seconds);
                                editor.putString("areacodeStart", areacodeStart.getText().toString());
                                editor.putString("areacodeEnd", areacodeEnd.getText().toString());
                                editor.putString("rangeStart", rangeStart.getText().toString());
                                editor.putString("rangeEnd", rangeEnd.getText().toString());
                                editor.apply();
                                long length = 0;
                                try {
                                    length = Long.valueOf(numEnd.replaceAll("[*+]", "0")) - Long.valueOf(numStart.replaceAll("[*+]", "0"));
                                } catch (Exception e) {
                                    Log.e("Oh... ", "Something went horribly wrong");
                                }

                                for (int i = 0; i <= length; i++) {
                                    if (checkBox.isChecked()) {
                                        textOutput.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                textOutput.append("Done." + "\n");
                                                checkBox.setChecked(false);
                                            }
                                        });
                                        break;
                                    }

                                    final long number = longStart + i;
                                    textOutput.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            textOutput.append("Trying number: " + areaStart + number + "\n");
                                        }
                                    });

                                    call.setData(Uri.parse("tel:" + areaStart + number));
                                    startActivity(call);
                                    try {
                                        Thread.sleep(seconds * 1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    if (manager.getMode() == AudioManager.MODE_IN_CALL) {
                                        try {
                                            hangup();
                                        } catch (NoSuchMethodException e) {
                                            e.printStackTrace();
                                        } catch (ClassNotFoundException e) {
                                            e.printStackTrace();
                                        } catch (InvocationTargetException e) {
                                            e.printStackTrace();
                                        } catch (IllegalAccessException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    try {
                                        Thread.sleep(3000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    if (LastCall() > 0) {
                                        writeToFile(areaStart + number + "\n", numStart);
                                        textOutput.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                textOutput.append(areaStart + number + " answered\n");
                                            }
                                        });
                                    }
                                }
                            }
                        }).start();
                    } catch (Exception e) {
                        Log.e("Oh... ", "Something went terribly wrong");
                    }
                }
            }
        });
    }

    private int LastCall() {
        int callDuration = 0;
        Uri contacts = CallLog.Calls.CONTENT_URI;
        Cursor managedCursor = this.getContentResolver().query(contacts, null, null, null, null);
        assert managedCursor != null;
        int duration1 = managedCursor.getColumnIndex( CallLog.Calls.DURATION);
        if( managedCursor.moveToLast() ) {
            callDuration = managedCursor.getInt( duration1 );
        }
        managedCursor.close();
        return callDuration;
    }
    private void hangup() throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
        TelephonyManager tm = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        Class<?> c = Class.forName(tm.getClass().getName());
        Method m = c.getDeclaredMethod("getITelephony");
        m.setAccessible(true);
        Object telephonyService = m.invoke(tm);
        Class<?> telephonyServiceClass = Class.forName(telephonyService.getClass().getName());
        Method endCallMethod = telephonyServiceClass.getDeclaredMethod("endCall");
        endCallMethod.invoke(telephonyService);
    }
    private void writeToFile(String data, String fileName) {
        File file;
        FileOutputStream outputStream;
        try {
            file = new File(directoryPath, fileName);
            outputStream = new FileOutputStream(file, true);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String readFromFile(String fileName) {
        final File file = new File(directoryPath, fileName);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        }
        catch (IOException e) {
            return "";
        }
        return text.toString();
    }
    private void openFile(String fileName) {
        File file = new File(directoryPath, fileName);
        if (!file.exists()) {
            textOutput.setText("No such file - " + fileName + "\n");
            return;
        }
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/plain");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
    private void showError(String message, String button) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message);

        builder.setPositiveButton(
                button,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

}