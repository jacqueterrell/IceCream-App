# Android: "Unknown calling package name 'com.google.android.gms'" / "Failed to get service from broker"

This `SecurityException` usually means your **Maps SDK for Android** (or Play Services) API key is restricted to **Android apps**, but the **SHA-1 fingerprint** of the build you’re running is **not** listed for that key.

## Fix in Google Cloud Console

1. Get the **SHA-1** of the keystore you use to run the app.
   - **Debug builds** (run from Android Studio):
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
   - Copy the **SHA1** line (e.g. `AA:BB:CC:...`).

2. Open **[APIs & Services → Credentials](https://console.cloud.google.com/apis/credentials)** (same project as your Firebase / app).

3. Open the **API key** used by the Android app for Maps (the one in `res/values/strings.xml` as `google_maps_api_key`).

4. Under **Application restrictions**:
   - If it’s **Android apps**, click **Add an item** (or edit the existing one).
   - **Package name**: `com.icecreamapp.sweethearts` (must match `applicationId` in `app/build.gradle.kts`).
   - **SHA-1 certificate fingerprint**: paste the SHA-1 from step 1.
   - Save.

5. If you use a **release** keystore for signed builds, add that keystore’s SHA-1 as well (same key, add another “Android app” item with the same package and the release SHA-1).

6. Wait a minute, then **uninstall the app** from the device/emulator and **reinstall** (or clear app data), so the new key restrictions are used.

## Optional: Temporarily confirm it’s the key

- Set that key’s **Application restrictions** to **None** and run the app again. If the error goes away, the problem was the missing/wrong SHA-1 or package name. Then set restrictions back and add the correct SHA-1 and package as above.
