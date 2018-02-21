package io.stanwood.framework.network.auth;

import android.support.annotation.Nullable;

import okhttp3.Response;

/**
 * Listener for ultimately failed authentication.
 */
public interface OnAuthenticationFailedListener {
    /**
     * Called upon ultimately failed authentication.
     *
     * @param response the Response indicating failure or {@code null} if the issue arose before we
     *                 got a response
     */
    void onAuthenticationFailed(@Nullable Response response);
}
