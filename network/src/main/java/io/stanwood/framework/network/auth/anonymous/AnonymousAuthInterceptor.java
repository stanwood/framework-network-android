package io.stanwood.framework.network.auth.anonymous;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import io.stanwood.framework.network.auth.AuthHeaderKeys;
import io.stanwood.framework.network.auth.AuthenticationProvider;
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
    private final AuthenticationProvider authenticationProvider;
    @Nullable
    private final AuthenticatedAuthInterceptor authenticatedAuthInterceptor;
    @NonNull
    private final TokenReaderWriter tokenReaderWriter;

    public AnonymousAuthInterceptor(
            @NonNull Context applicationContext,
            @NonNull AuthenticationProvider authenticationProvider,
            @Nullable AuthenticatedAuthInterceptor authenticatedAuthInterceptor,
            @NonNull TokenReaderWriter tokenReaderWriter
    ) {
        this.connectionState = new ConnectionState(applicationContext);
        this.authenticationProvider = authenticationProvider;
        this.authenticatedAuthInterceptor = authenticatedAuthInterceptor;
        this.tokenReaderWriter = tokenReaderWriter;
    }

    Request getRequest(
            @NonNull Request request,
            @NonNull AuthenticationProvider authenticationProvider
    ) throws IOException {
        final Request.Builder requestBuilder = request.newBuilder();
        request = tokenReaderWriter.removeToken(request);

        if (connectionState.isConnected()) {
            String token;
            synchronized (authenticationProvider.getLock()) {
                try {
                    token = authenticationProvider.getToken(false);
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
                && authenticationProvider.isUserSignedIn()
                && connectionState.isConnected()) {
            return authenticatedAuthInterceptor.intercept(chain);
        }

        Request request = getRequest(chain.request(), authenticationProvider);

        return chain.proceed(request);
    }
}
