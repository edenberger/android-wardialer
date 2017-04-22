package com.tiger.wardialer;

import android.Manifest;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.CallLog;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class MainActivity extends Activity {

    TextView textOutput;
    Button buttonStart, buttonOpen;
    AudioManager manager;
    EditText rangeStart, rangeEnd, areacodeStart, areacodeEnd;
    Intent call = new Intent(Intent.ACTION_CALL);
    File directoryPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "WardialScans");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        boolean hasPermissionPhoneState = (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermissionPhoneState) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        final SharedPreferences sharedPreferences = this.getSharedPreferences("", Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();

        if (!directoryPath.exists()) {
            directoryPath.mkdirs();
        }
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
        manager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        areacodeStart.setText(sharedPreferences.getString("areacodeStart", null));
        areacodeEnd.setText(sharedPreferences.getString("areacodeEnd", null));
        rangeStart.setText(sharedPreferences.getString("rangeStart", null));
        rangeEnd.setText(sharedPreferences.getString("rangeEnd", null));
        textOutput.setText(readFromFile(areacodeStart.getText().toString() + rangeStart.getText().toString()));
        final NumberPicker secondsPicker = (NumberPicker) findViewById(R.id.secondsPicker);

        secondsPicker.setMinValue(4);
        secondsPicker.setMaxValue(30);
        secondsPicker.setValue(sharedPreferences.getInt("seconds", 9));

        buttonOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFile(areacodeStart.getText().toString() + rangeStart.getText().toString());
            }
        });

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textOutput.setText("Starting:\n");
                final String areaStart = areacodeStart.getText().toString();
                final String numStart = areaStart + rangeStart.getText().toString();
                final int intStart = Integer.valueOf(rangeStart.getText().toString());
                final int numEnd = Integer.parseInt(areacodeEnd.getText().toString() + rangeEnd.getText().toString());
                final int seconds = secondsPicker.getValue();
                editor.putInt("seconds", seconds);
                editor.putString("areacodeStart", areacodeStart.getText().toString());
                editor.putString("areacodeEnd", areacodeEnd.getText().toString());
                editor.putString("rangeStart", rangeStart.getText().toString());
                editor.putString("rangeEnd", rangeEnd.getText().toString());
                editor.apply();

                int length = numEnd - Integer.parseInt(numStart);
                for (int i = 0; i <= length; i++) {
                    int number = intStart + i;
                    textOutput.append("Trying number: " + areaStart + number + "\n");
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
                        writeToFile(areaStart + number + " answered\n", numStart);
                        textOutput.append(areaStart + number + " Answered.\n");
                    }
                }
            }
        });
    }

    public int LastCall() {
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
    public void hangup() throws NoSuchMethodException, ClassNotFoundException, InvocationTargetException, IllegalAccessException {
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
    public void openFile(String fileName) {
        File file = new File(directoryPath, fileName);
        Uri uri = Uri.fromFile(file);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "text/plain");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }
}
