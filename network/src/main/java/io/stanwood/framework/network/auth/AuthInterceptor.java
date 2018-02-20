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
