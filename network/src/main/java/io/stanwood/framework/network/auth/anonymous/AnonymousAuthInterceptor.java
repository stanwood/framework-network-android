package io.stanwood.framework.network.auth.anonymous;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import io.stanwood.framework.network.auth.AuthHeaderKeys;
import io.stanwood.framework.network.auth.AuthenticationService;
import io.stanwood.framework.network.auth.TokenReaderWriter;
import io.stanwood.framework.network.auth.authenticated.AuthenticatedAuthInterceptor;
import io.stanwood.framework.network.util.ConnectionState;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class is used by okhttp to anonymously authenticate requests.
 */
public class AnonymousAuthInterceptor implements Interceptor {

    @NonNull
    private final ConnectionState connectionState;
    @NonNull
    private final AuthenticationService authenticationService;
    @Nullable
    private final AuthenticatedAuthInterceptor authenticatedAuthInterceptor;
    @NonNull
    private final TokenReaderWriter tokenReaderWriter;

    public AnonymousAuthInterceptor(
            @NonNull Context applicationContext,
            @NonNull AuthenticationService authenticationService,
            @Nullable AuthenticatedAuthInterceptor authenticatedAuthInterceptor,
            @NonNull TokenReaderWriter tokenReaderWriter
    ) {
        this.connectionState = new ConnectionState(applicationContext);
        this.authenticationService = authenticationService;
        this.authenticatedAuthInterceptor = authenticatedAuthInterceptor;
        this.tokenReaderWriter = tokenReaderWriter;
    }

    Request getRequest(
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
        if (authenticatedAuthInterceptor != null
                && authenticationService.isUserSignedIn()
                && connectionState.isConnected()) {
            return authenticatedAuthInterceptor.intercept(chain);
        }

        Request request = getRequest(chain.request(), authenticationService);

        return chain.proceed(request);
    }
}
