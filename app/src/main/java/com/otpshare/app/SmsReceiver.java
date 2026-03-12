/*
 * ============================================================
 * OTP Share — Android App
 * SmsReceiver.java
 *
 * This BroadcastReceiver automatically triggers whenever
 * a new SMS is received. It reads the message content and
 * sender, then forwards it to the OTP Share server via HTTP.
 * ============================================================
 */

package com.otpshare.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "OTPShare_SMS";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Check if this is an SMS received intent
        if (!"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction())) {
            return;
        }

        // Check if forwarding is active
        SharedPreferences prefs = context.getSharedPreferences(
                MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        boolean isActive = prefs.getBoolean(MainActivity.KEY_IS_ACTIVE, false);

        if (!isActive) {
            Log.d(TAG, "OTP forwarding is paused, ignoring SMS.");
            return;
        }

        // Get server settings
        String serverUrl = prefs.getString(MainActivity.KEY_SERVER_URL, "");
        String deviceId = prefs.getString(MainActivity.KEY_DEVICE_ID, "");
        String password = prefs.getString(MainActivity.KEY_PASSWORD, "");

        if (serverUrl.isEmpty() || deviceId.isEmpty() || password.isEmpty()) {
            Log.e(TAG, "Settings not configured. Cannot forward SMS.");
            return;
        }

        // Extract SMS data from the intent
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        String format = bundle.getString("format");

        // Process each SMS part (long messages are split into parts)
        StringBuilder fullMessage = new StringBuilder();
        String senderNumber = "";

        for (Object pdu : pdus) {
            SmsMessage smsMessage;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
            }

            senderNumber = smsMessage.getDisplayOriginatingAddress();
            fullMessage.append(smsMessage.getMessageBody());
        }

        String message = fullMessage.toString();
        Log.d(TAG, "SMS from: " + senderNumber + " | Message: " + message);

        // Log to UI if activity is running
        MainActivity main = MainActivity.getInstance();
        if (main != null) {
            main.addLog("📩 SMS from: " + senderNumber);
        }

        // Forward the SMS to the server in a background thread
        ApiService.sendMessage(serverUrl, deviceId, password, senderNumber, message);
    }
}
