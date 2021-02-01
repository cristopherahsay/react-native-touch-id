package com.rnfingerprint;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import android.content.Context;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

public class SimplePromptCallback extends BiometricPrompt.AuthenticationCallback {
    private Callback errorCallback;
    private Callback successCallback;
    private ReactApplicationContext reactContext;

    public SimplePromptCallback(Callback reactErrorCallback, Callback reactSuccessCallback, final ReactApplicationContext reactContext) {
        super();
        this.errorCallback = reactErrorCallback;
        this.successCallback = reactSuccessCallback;
        this.reactContext = reactContext;
    }

    @Override
    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        WritableMap params = Arguments.createMap();
        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
            // WritableMap resultMap = new WritableNativeMap();
            // resultMap.putBoolean("success", false);
            // resultMap.putString("error", "User cancellation");
            // this.errorCallback.invoke(resultMap);

            params.putBoolean("result", false); 
            params.putString("message", "User cancellation");
            this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("authResult", params);

        } else {
            // this.errorCallback.invoke(errString.toString(), errString.toString());
            params.putBoolean("result", false); 
            params.putString("message", errString.toString());
            this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("authResult", params);
        }
    }

    @Override
    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        // WritableMap resultMap = new WritableNativeMap();
        // resultMap.putBoolean("success", true);
        // this.successCallback.invoke(resultMap);
        WritableMap params = Arguments.createMap();
        params.putBoolean("result", true); 
        params.putString("message", "success");
        this.reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("authResult", params);
    }
}