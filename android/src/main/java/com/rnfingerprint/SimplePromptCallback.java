package com.rnfingerprint;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

public class SimplePromptCallback extends BiometricPrompt.AuthenticationCallback {
    private Callback errorCallback;
    private Callback successCallback;

    public SimplePromptCallback(Callback reactErrorCallback, Callback reactSuccessCallback) {
        super();
        this.errorCallback = reactErrorCallback;
        this.successCallback = reactSuccessCallback;
    }

    @Override
    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
        super.onAuthenticationError(errorCode, errString);
        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
            WritableMap resultMap = new WritableNativeMap();
            resultMap.putBoolean("success", false);
            resultMap.putString("error", "User cancellation");
            this.errorCallback.invoke(resultMap);
        } else {
            this.errorCallback.invoke(errString.toString(), errString.toString());
        }
    }

    @Override
    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);

        WritableMap resultMap = new WritableNativeMap();
        resultMap.putBoolean("success", true);
        this.successCallback.invoke(resultMap);
    }
}