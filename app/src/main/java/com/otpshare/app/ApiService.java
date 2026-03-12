/*
 * ============================================================
 * OTP Share — Android App
 * ApiService.java
 *
 * Handles sending SMS data to the OTP Share server via HTTP.
 * Runs on a background thread to avoid blocking the main thread.
 * ============================================================
 */

package com.otpshare.app;

import android.util.Log;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;

public class ApiService {

    private static final String TAG = "OTPShare_API";

    /**
     * Send a captured SMS message to the OTP Share server.
     * This runs in a background thread automatically.
     *
     * @param serverUrl Base URL of the server (e.g., http://192.168.1.5:3000)
     * @param deviceId  The registered device ID
     * @param password  The device password
     * @param sender    The SMS sender number/name
     * @param message   The SMS message content
     */
    private static Timer heartbeatTimer = null;

    public static void startHeartbeat(final String serverUrl) {
        if (heartbeatTimer != null) return;
        
        heartbeatTimer = new Timer();
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                sendHeartbeat(serverUrl);
            }
        }, 0, 60000); // Every 60 seconds
    }

    private static void sendHeartbeat(final String serverUrl) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(serverUrl.endsWith("/") ? serverUrl + "api/heartbeat" : serverUrl + "/api/heartbeat");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("bypass-tunnel-reminder", "true");
                    conn.setConnectTimeout(5000);
                    conn.getResponseCode(); // Just trigger the request
                    conn.disconnect();
                    Log.d(TAG, "💓 Heartbeat sent");
                } catch (Exception e) {
                    Log.e(TAG, "💔 Heartbeat failed: " + e.getMessage());
                }
            }
        }).start();
    }

    public static void sendMessage(final String serverUrl, final String deviceId,
                                   final String password, final String sender,
                                   final String message) {
        // Run network call on a background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Build the API URL
                    URL url = new URL(serverUrl + "/api/message");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    // Configure the request
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("bypass-tunnel-reminder", "true");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(10000); // 10 seconds
                    conn.setReadTimeout(10000);

                    // Build JSON payload
                    // We manually build JSON to avoid needing external libraries
                    String json = "{"
                            + "\"deviceId\":\"" + escapeJson(deviceId) + "\","
                            + "\"password\":\"" + escapeJson(password) + "\","
                            + "\"sender\":\"" + escapeJson(sender) + "\","
                            + "\"message\":\"" + escapeJson(message) + "\""
                            + "}";

                    // Send the request
                    OutputStream os = conn.getOutputStream();
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    os.close();

                    // Check response
                    int responseCode = conn.getResponseCode();
                    MainActivity main = MainActivity.getInstance();
                    if (responseCode == 200) {
                        Log.d(TAG, "✅ OTP forwarded successfully!");
                        if (main != null) main.addLog("✅ Forwarded to PC!");
                    } else {
                        Log.e(TAG, "❌ Server returned: " + responseCode);
                        if (main != null) main.addLog("❌ Server Error: " + responseCode);
                    }

                    conn.disconnect();

                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to send OTP: " + e.getMessage());
                    MainActivity main = MainActivity.getInstance();
                    if (main != null) main.addLog("❌ Failed: " + e.getMessage());
                }
            }
        }).start();
    }

    /**
     * Escape special JSON characters in a string
     */
    private static String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
