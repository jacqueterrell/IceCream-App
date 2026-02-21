# Android: Maps SDK setup (fix "Ensure that the Maps SDK for Android is enabled")

Do these steps in **[Google Cloud Console](https://console.cloud.google.com)** (same project as your Firebase app).

## 1. Enable Maps SDK for Android

1. Go to **[APIs & Services → Library](https://console.cloud.google.com/apis/library)**.
2. Search for **Maps SDK for Android**.
3. Open it and click **Enable**.

Or use: [Enable Maps SDK for Android](https://console.cloud.google.com/apis/library/maps-android-backend.googleapis.com)

## 2. Create or edit your API key with the Android restriction

1. Go to **[APIs & Services → Credentials](https://console.cloud.google.com/apis/credentials)**.
2. Either **create** an API key (+ Create credentials → API key) or **edit** the key you use in the app.
3. Under **Application restrictions**, choose **Android apps**.
4. Click **Add an item** and set:
   - **Package name:** `com.icecreamapp.sweethearts`
   - **SHA-1 certificate fingerprint:**  
     `68:20:A7:9E:E2:56:D3:15:92:29:E4:F0:29:91:1F:CC:4B:9B:09:C5`
5. Under **API restrictions**, choose **Restrict key** and enable **Maps SDK for Android** (and any other APIs this key uses).
6. Save.

## 3. Put the key in the app

1. Copy the API key value from the Credentials page.
2. In the Android project, open **`app/src/main/res/values/strings.xml`**.
3. Replace `YOUR_GOOGLE_MAPS_API_KEY` with your actual key:
   ```xml
   <string name="google_maps_api_key" translatable="false">AIzaSy...your_key...</string>
   ```
4. Rebuild and run the app.

If you use a **different** debug or release keystore later, the SHA-1 may change; add that new SHA-1 as another Android app restriction for the same key (same package name).

---

## "Unknown calling package name 'com.google.android.gms'" / "Failed to get service from broker"

This means the **SHA-1 of the build you’re running** is not listed on the API key. The key might have a different machine’s SHA-1, or you might be using a new/recreated debug keystore.

### Fix: Add your current SHA-1 to the key

1. **Get the SHA-1 of the keystore that signs the app you’re running.**

   **Debug builds** (running from Android Studio):
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
   Copy the **SHA1** line (e.g. `68:20:A7:...`).

   **Release builds:** Use the same command with your release keystore path and alias.

2. **Add this SHA-1 to the same API key** (Credentials → your key → Application restrictions → Android apps):
   - Click **Add an item** (or edit existing).
   - Package name: `com.icecreamapp.sweethearts`
   - SHA-1: paste the value from step 1.
   - Save. You can have **multiple** Android app entries (same package, different SHA-1) for debug and release.

3. Wait 1–2 minutes, then **uninstall the app** from the device/emulator and **install again** (or clear app data). Run the app again.

### Optional: Confirm it’s the key restriction

- Set the key’s **Application restrictions** to **None** and run the app. If the error goes away, the problem is the missing/wrong SHA-1. Then set **Android apps** again and add the correct SHA-1 from step 1.
