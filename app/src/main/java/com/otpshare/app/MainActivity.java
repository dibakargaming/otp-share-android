/*
 * ============================================================
 * OTP Share — Android App
 * MainActivity.java
 *
 * This is the setup screen. The user enters:
 *   1. Server URL (e.g., http://192.168.1.5:3000)
 *   2. Device ID (e.g., "myphone")
 *   3. Password
 *
 * These credentials are saved in SharedPreferences and used
 * by SmsReceiver to forward incoming SMS to the server.
 * ============================================================
 */

package com.otpshare.app;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // SharedPreferences key names
    public static final String PREFS_NAME = "OTPSharePrefs";
    public static final String KEY_SERVER_URL = "server_url";
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_IS_ACTIVE = "is_active";

    private static final int SMS_PERMISSION_CODE = 100;

    private EditText etServerUrl, etDeviceId, etPassword;
    private Button btnSave, btnToggle, btnTest;
    private TextView tvStatus, tvLogs;
    private static MainActivity instance;

    public static MainActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance = this;

        // Find views
        etServerUrl = findViewById(R.id.et_server_url);
        etDeviceId = findViewById(R.id.et_device_id);
        etPassword = findViewById(R.id.et_password);
        btnSave = findViewById(R.id.btn_save);
        btnToggle = findViewById(R.id.btn_toggle);
        btnTest = findViewById(R.id.btn_test);
        tvStatus = findViewById(R.id.tv_status);
        tvLogs = findViewById(R.id.tv_logs);

        // Load saved settings
        loadSettings();

        // Request SMS permissions
        requestSmsPermission();

        // Save button click
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        // Toggle active/inactive
        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleActive();
            }
        });

        // Test connection button
        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testConnection();
            }
        });
    }

    private void testConnection() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String serverUrl = etServerUrl.getText().toString().trim();
        String deviceId = etDeviceId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (serverUrl.isEmpty() || deviceId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields first!", Toast.LENGTH_SHORT).show();
            return;
        }

        addLog("Testing connection to: " + serverUrl);
        ApiService.sendMessage(serverUrl, deviceId, password, "Test System", "This is a test message from your phone!");
        Toast.makeText(this, "Test message sent! Check your PC.", Toast.LENGTH_SHORT).show();
    }

    public void addLog(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvLogs != null) {
                    String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
                    String currentText = tvLogs.getText().toString();
                    String newLog = "[" + time + "] " + text + "\n";
                    tvLogs.setText(newLog + currentText);
                }
            }
        });
    }

    /**
     * Load saved settings from SharedPreferences
     */
    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        etServerUrl.setText(prefs.getString(KEY_SERVER_URL, "http://"));
        etDeviceId.setText(prefs.getString(KEY_DEVICE_ID, ""));
        etPassword.setText(prefs.getString(KEY_PASSWORD, ""));

        boolean isActive = prefs.getBoolean(KEY_IS_ACTIVE, false);
        updateStatusUI(isActive);
    }

    /**
     * Save settings to SharedPreferences
     */
    private void saveSettings() {
        String serverUrl = etServerUrl.getText().toString().trim();
        String deviceId = etDeviceId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate
        if (serverUrl.isEmpty() || deviceId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Remove trailing slash from URL
        if (serverUrl.endsWith("/")) {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }

        // Save
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_SERVER_URL, serverUrl);
        editor.putString(KEY_DEVICE_ID, deviceId);
        editor.putString(KEY_PASSWORD, password);
        editor.putBoolean(KEY_IS_ACTIVE, true);
        editor.apply();

        updateStatusUI(true);
        Toast.makeText(this, "Settings saved! OTP forwarding is active.", Toast.LENGTH_LONG).show();
    }

    /**
     * Toggle OTP forwarding on/off
     */
    private void toggleActive() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isActive = prefs.getBoolean(KEY_IS_ACTIVE, false);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_IS_ACTIVE, !isActive);
        editor.apply();

        updateStatusUI(!isActive);

        String msg = !isActive ? "OTP forwarding activated!" : "OTP forwarding paused.";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /**
     * Update the status text and button appearance
     */
    private void updateStatusUI(boolean isActive) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String serverUrl = prefs.getString(KEY_SERVER_URL, "");

        if (isActive) {
            btnToggle.setText("⏸ Pause Forwarding");
            tvStatus.setText("Status: Active (Listening for OTPs)");
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            if (!serverUrl.isEmpty()) {
                ApiService.startHeartbeat(serverUrl);
            }
        } else {
            tvStatus.setText("○ Paused — Not forwarding");
            tvStatus.setTextColor(0xFFEF4444); // Red
            btnToggle.setText("▶ Start Forwarding");
        }
    }

    /**
     * Request SMS read permission from the user
     */
    private void requestSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                        Manifest.permission.RECEIVE_SMS,
                        Manifest.permission.READ_SMS
                    },
                    SMS_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission is required for OTP forwarding.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
