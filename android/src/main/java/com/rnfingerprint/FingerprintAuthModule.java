package com.rnfingerprint;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.UiThreadUtil;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricPrompt.AuthenticationCallback;
import androidx.biometric.BiometricPrompt.PromptInfo;
import androidx.fragment.app.FragmentActivity;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Arguments;

import android.content.Intent;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;

public class FingerprintAuthModule extends ReactContextBaseJavaModule implements LifecycleEventListener {

    private static final String FRAGMENT_TAG = "fingerprint_dialog";

    private KeyguardManager keyguardManager;
    private boolean isAppActive;

    public static boolean inProgress = false;
    private Callback reactNativeAuthSuccessCallback;
    private Callback reactNativeAuthErrorCallback;
    private ReactApplicationContext reactContext;

    // new
    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (requestCode == 10001) {                  
                if (resultCode == -1) { // success
                    callbackAuthSuccess();
                } else {
                    callbackAuthFail(resultCode);
                }                
            } 
        }
    };

    // new
    public void callbackAuthSuccess() {
        // this.reactNativeAuthSuccessCallback.invoke("Success");
        WritableMap params = Arguments.createMap();
        params.putBoolean("result", true); 
        params.putString("message", "success");
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("authResult", params);
    }

    // new
    public void callbackAuthFail(int resultCode) {
        // this.reactNativeAuthErrorCallback.invoke("Fail", resultCode);
        WritableMap params = Arguments.createMap();
        params.putBoolean("result", false); 
        params.putString("message", "Error code:" + resultCode);
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("authResult", params);
    }

    public FingerprintAuthModule(final ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addLifecycleEventListener(this);
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    private KeyguardManager getKeyguardManager() {
        if (keyguardManager != null) {
            return keyguardManager;
        }
        final Activity activity = getCurrentActivity();        
        if (activity == null) {
            return null;
        }

        keyguardManager = (KeyguardManager) activity.getSystemService(Context.KEYGUARD_SERVICE);      
                
        return keyguardManager;
    }

    // new
    private boolean isPasscodeAuthAvailable() {
        KeyguardManager keyguardManager = getKeyguardManager();
        if (keyguardManager != null && keyguardManager.isKeyguardSecure()) {
            return true;
        } else {
            return false;
        }
    }

    // new
    public void showPasscodeScreen() {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }

        KeyguardManager keyguardManager2 = getKeyguardManager();
        Intent intent = keyguardManager2.createConfirmDeviceCredentialIntent("Screen lock", "Screen lock");
        if (intent != null) {
            activity.startActivityForResult(intent, 10001);
        }
    }

    @Override
    public String getName() {
        return "FingerprintAuth";
    }

    @ReactMethod
    public void isSupported(final Callback reactErrorCallback, final Callback reactSuccessCallback) {
        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return;
        }

        int result = isFingerprintAuthAvailable();        
        if (result == FingerprintAuthConstants.IS_SUPPORTED) {
            // TODO: once this package supports Android's Face Unlock,
            // implement a method to find out which type of biometry
            // (not just fingerprint) is actually supported
            reactSuccessCallback.invoke("Fingerprint");
        } else if (isPasscodeAuthAvailable()) {
            reactSuccessCallback.invoke("Passcode");
        } else {
            reactErrorCallback.invoke("Not supported.", result);
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @ReactMethod
    public void authenticate(final String reason, final ReadableMap authConfig, final Callback reactErrorCallback, final Callback reactSuccessCallback) {        
        this.reactNativeAuthSuccessCallback = reactSuccessCallback;
        this.reactNativeAuthErrorCallback = reactErrorCallback;
        final Activity activity = getCurrentActivity();
        if (inProgress || !isAppActive || activity == null) {
            return;
        }
        inProgress = true;

        int canAuthenticate = this.supportBiometric();
        if (canAuthenticate !=  BiometricManager.BIOMETRIC_SUCCESS && !isPasscodeAuthAvailable()) {
            inProgress = false;
            // reactErrorCallback.invoke("Not supported", canAuthenticate);
            WritableMap params = Arguments.createMap();
            params.putBoolean("result", false); 
            params.putString("message", "Not supported");
            this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("authResult", params);
            return;
        }


        if (canAuthenticate !=  BiometricManager.BIOMETRIC_SUCCESS && isPasscodeAuthAvailable()) {
            showPasscodeScreen();
            inProgress = false;
            return;
        }

        

        /* FINGERPRINT ACTIVITY RELATED STUFF */
//        final Cipher cipher = new FingerprintCipher().getCipher();
//        if (cipher == null) {
//            inProgress = false;
//            reactErrorCallback.invoke("Not supported", FingerprintAuthConstants.NOT_AVAILABLE);
//            return;
//        }

        // We should call it only when we absolutely sure that API >= 23.
        // Otherwise we will get the crash on older versions.
        // TODO: migrate to FingerprintManagerCompat
//        final FingerprintManager.CryptoObject cryptoObject = new FingerprintManager.CryptoObject(cipher);
//
//        final DialogResultHandler drh = new DialogResultHandler(reactErrorCallback, reactSuccessCallback);
//
//        final FingerprintDialog fingerprintDialog = new FingerprintDialog();
//        fingerprintDialog.setCryptoObject(cryptoObject);
//        fingerprintDialog.setReasonForAuthentication(reason);
//        fingerprintDialog.setAuthConfig(authConfig);
//        fingerprintDialog.setDialogCallback(drh);
        this.simplePrompt(authConfig, reactErrorCallback, reactSuccessCallback);
        inProgress = false;
        return;

//        if (!isAppActive) {
//            inProgress = false;
//            return;
//        }
//
//        fingerprintDialog.show(activity.getFragmentManager(), FRAGMENT_TAG);
    }    

    private int isFingerprintAuthAvailable() {
        if (android.os.Build.VERSION.SDK_INT < 23) {
            return FingerprintAuthConstants.NOT_SUPPORTED;
        }

        final Activity activity = getCurrentActivity();
        if (activity == null) {
            return FingerprintAuthConstants.NOT_AVAILABLE; // we can't do the check
        }

        final KeyguardManager keyguardManager = getKeyguardManager();

        // We should call it only when we absolutely sure that API >= 23.
        // Otherwise we will get the crash on older versions.
        // TODO: migrate to FingerprintManagerCompat
        final FingerprintManager fingerprintManager = (FingerprintManager) activity.getSystemService(Context.FINGERPRINT_SERVICE);

        if (fingerprintManager == null || !fingerprintManager.isHardwareDetected()) {
            return FingerprintAuthConstants.NOT_PRESENT;
        }

        if (keyguardManager == null || !keyguardManager.isKeyguardSecure()) {
            return FingerprintAuthConstants.NOT_AVAILABLE;
        }

        if (!fingerprintManager.hasEnrolledFingerprints()) {
            return FingerprintAuthConstants.NOT_ENROLLED;
        }
        return FingerprintAuthConstants.IS_SUPPORTED;
    }



    public int supportBiometric() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ReactApplicationContext reactApplicationContext = getReactApplicationContext();
            BiometricManager biometricManager = BiometricManager.from(reactApplicationContext);
            int canAuthenticate = biometricManager.canAuthenticate();
            return canAuthenticate;
        }
        return BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE;
    }
    public void simplePrompt(final ReadableMap config, final Callback reactErrorCallback, final Callback reactSuccessCallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            final ReactApplicationContext reactApplicationContext = getReactApplicationContext();
            UiThreadUtil.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String promptMessage = "";
                                if (config.hasKey("title")) {
                                    promptMessage = config.getString("title");
                                }
                                String cancelButtonText = "";
                                if (config.hasKey("cancelText")) {
                                    cancelButtonText = config.getString("cancelText");
                                }

                                AuthenticationCallback authCallback = new SimplePromptCallback(reactErrorCallback, reactSuccessCallback, reactApplicationContext);
                                FragmentActivity fragmentActivity = (FragmentActivity) getCurrentActivity();
                                Executor executor = Executors.newSingleThreadExecutor();
                                BiometricPrompt biometricPrompt = new BiometricPrompt(fragmentActivity, executor, authCallback);
                                PromptInfo promptInfo = new PromptInfo.Builder()
                                        .setDeviceCredentialAllowed(true)
                                        .setTitle(promptMessage)
                                        .build();
                                biometricPrompt.authenticate(promptInfo);
                            } catch (Exception e) {
                                // reactErrorCallback.invoke("Error displaying local biometric prompt: " + e.getMessage(), "Error displaying local biometric prompt: " + e.getMessage());
                                WritableMap params = Arguments.createMap();
                                params.putBoolean("result", false); 
                                params.putString("message", "Error displaying local biometric prompt: " + e.getMessage());
                                reactApplicationContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("authResult", params);
                            }
                        }
                    });
        } else {
            // reactErrorCallback.invoke("Cannot display biometric prompt on android versions below 6.0", "Cannot display biometric prompt on android versions below 6.0");
            WritableMap params = Arguments.createMap();
            params.putBoolean("result", false); 
            params.putString("message", "Cannot display biometric prompt on android versions below 6.0");
            this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("authResult", params);
        }
    }

    @Override
    public void onHostResume() {
        isAppActive = true;
    }

    @Override
    public void onHostPause() {
        isAppActive = false;
    }

    @Override
    public void onHostDestroy() {
        isAppActive = false;
    }
}