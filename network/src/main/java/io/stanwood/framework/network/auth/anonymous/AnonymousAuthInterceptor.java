package io.stanwood.framework.network.auth.anonymous;

import android.content.Context;
import android.support.annotation.NonNull;

import java.io.IOException;

import io.stanwood.framework.network.auth.AuthHeaderKeys;
import io.stanwood.framework.network.auth.AuthenticationService;
import io.stanwood.framework.network.auth.TokenReaderWriter;
import io.stanwood.framework.network.util.ConnectionState;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AnonymousAuthInterceptor implements Interceptor {

    private final ConnectionState connectionState;
    @NonNull
    private final AuthenticationService authenticationService;
    @NonNull
    private final TokenReaderWriter tokenReaderWriter;

    public AnonymousAuthInterceptor(
            @NonNull Context applicationContext,
            @NonNull AuthenticationService authenticationService,
            @NonNull TokenReaderWriter tokenReaderWriter
    ) {
        this.connectionState = new ConnectionState(applicationContext);
        this.authenticationService = authenticationService;
        this.tokenReaderWriter = tokenReaderWriter;
    }

    private Request getRequest(
            @NonNull Request request,
            @NonNull AuthenticationService authenticationService
    ) throws IOException {
        final Request.Builder requestBuilder = request.newBuilder();
        request = tokenReaderWriter.removeToken(request);

        if (connectionState.isConnected()) {
            String token;
            synchronized (AuthenticationService.ANONYMOUS_AUTH_REFRESH_TOKEN_LOCK) {
                try {
                    token = authenticationService.getAnonymousToken(false);
                } catch (Exception e) {
                    throw new IOException("Error while trying to retrieve Firebase auth token: " + e.getMessage(), e);
                }
            }
            requestBuilder.header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY, "true");
            request = tokenReaderWriter.write(request, token);
        } else {
            // we're offline, clean up headers for cache handling
            requestBuilder.removeHeader(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY);
        }
        return request;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = getRequest(chain.request(), authenticationService);

        return chain.proceed(request);
    }
}
