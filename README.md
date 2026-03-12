# 📱 OTP Share — Mobile Setup Guide

This guide covers how to set up your phone to automatically send OTPs to your PC.

---

## 🚀 Option 1: Using Macrodroid (Easiest & Fastest)
*No coding or Android Studio required. Setup takes 2 minutes.*

### Step 1: Install Macrodroid
1. Download **MacroDroid** from the Google Play Store.

### Step 2: Create the Forwarding Macro
1. Open MacroDroid and tap **Add Macro**.
2. **Triggers (Red (+))**: Search for **SMS Received** → Select **Any Number** → OK.
3. **Actions (Blue (+))**: Search for **HTTP Request** → Select **POST**.
   - **URL**: `http://YOUR_PC_IP:3000/api/message` 
     *(See "How to find your IP" below)*
   - **Content Type**: Select `application/json`.
   - **Body**: Copy and paste this exactly:
     ```json
     {
       "deviceId": "myphone",
       "password": "test",
       "sender": "{sms_number}",
       "message": "{sms_message}"
     }
     ```
4. **Name your macro**: e.g., "OTP Forwarder" and tap **Save**.

---

## 🛠️ Option 2: Building the APK (Native App)
*Use this if you want a dedicated app. Requires Android Studio on your PC.*

### Step 1: Open the Project
1. Open **Android Studio**.
2. Select **Open an Existing Project** and navigate to:
   `c:\Users\user\Desktop\app\android-app`

### Step 2: Build the APK
1. In the top menu, click **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**.
2. Wait for it to finish. A popup will appear in the bottom right; click **locate** to find the `app-debug.apk` file.

### Step 3: Install on Phone
1. Transfer the `.apk` file to your phone (via USB, Drive, or WhatsApp).
2. Tap the file on your phone to install it. (Allow "Install from Unknown Sources" if asked).
3. Open **OTP Share** on your phone.
4. Enter your **Server URL**, **Device ID**, and **Password**.
5. Tap **Save Settings** then **Start Forwarding**.

---

## 🌍 How to Access From Anywhere?
*Your server normally only works on your local WiFi. To make it work anywhere:*

1. Download **ngrok** on your PC.
2. Open a terminal and run: `ngrok http 3000`
3. It will give you a "Forwarding" link like `https://abc-123.ngrok-free.app`.
4. **Important**: Use THIS link in your phone setup (Step 3 or Macrodroid URL) instead of the IP address.

---

## 🏠 How to find your PC's IP Address?
1. On your PC, press `Win + R`, type `cmd`, and press Enter.
2. Type `ipconfig` and press Enter.
3. Look for **IPv4 Address** (usually something like `192.168.1.5`).

---

## 📱 Adding More Devices
To add a second or third phone:
1. Go to your PC browser (`localhost:3000`).
2. Click the **Device Switcher** dropdown (top right) → Click **+**.
3. **Register** a new Device ID (e.g., `phone2`).
4. On the second phone, use `phone2` as the Device ID in the setup.
5. You can now toggle between `myphone` and `phone2` on your PC!
