/*
 * Copyright (c) 2018 stanwood GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.stanwood.framework.network.auth;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import io.stanwood.framework.network.util.ConnectionState;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is used by okhttp to authenticate requests.
 */
public class AuthInterceptor implements Interceptor {

    @NonNull
    private final ConnectionState connectionState;
    @NonNull
    private final AuthenticationProvider authenticationProvider;
    @NonNull
    private final TokenReaderWriter tokenReaderWriter;
    @Nullable
    private final OnAuthenticationFailedListener onAuthenticationFailedListener;

    public AuthInterceptor(
            @NonNull Context applicationContext,
            @NonNull AuthenticationProvider authenticationProvider,
            @NonNull TokenReaderWriter tokenReaderWriter,
            @Nullable OnAuthenticationFailedListener onAuthenticationFailedListener

    ) {
        this.connectionState = new ConnectionState(applicationContext);
        this.authenticationProvider = authenticationProvider;
        this.tokenReaderWriter = tokenReaderWriter;
        this.onAuthenticationFailedListener = onAuthenticationFailedListener;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        request = tokenReaderWriter.removeToken(request);
        final Request.Builder requestBuilder = request.newBuilder();

        if (connectionState.isConnected()) {
            String token;
            synchronized (authenticationProvider.getLock()) {
                try {
                    token = authenticationProvider.getToken(false);
                } catch (AuthenticationException e) {
                    onAuthenticationFailed();
                    throw e;
                }
            }

            if (token == null) {
                onAuthenticationFailed();
                throw new AuthenticationException();
            }

            requestBuilder.header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY, "true");
            request = tokenReaderWriter.write(requestBuilder.build(), token);
        } else {
            // we're offline, clean up headers for cache handling
            request = requestBuilder.removeHeader(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY).build();
        }

        return chain.proceed(request);
    }

    private void onAuthenticationFailed() {
        if (onAuthenticationFailedListener != null) {
            onAuthenticationFailedListener.onAuthenticationFailed(null);
        }
    }
}
