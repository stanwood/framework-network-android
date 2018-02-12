package io.stanwood.framework.network.auth;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import io.stanwood.framework.network.util.ConnectionState;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * This class will be called by okhttp upon receiving a 401 from the server which means we should
 * usually retry the request with a fresh token.
 *
 * It is NOT called for during initially making a request. For that refer to
 * {@link AnonymousAuthInterceptor}.
 */
public abstract class AnonymousAuthenticator implements Authenticator {

    private final AuthenticationService authenticationService;
    private final ConnectionState connectionState;
    private final TokenReaderWriter tokenReaderWriter;

    public AnonymousAuthenticator(
            @NonNull Context applicationContext,
            @NonNull AuthenticationService authenticationService,
            @NonNull TokenReaderWriter tokenReaderWriter
    ) {
       this.authenticationService = authenticationService;
       this.connectionState = new ConnectionState(applicationContext);
       this.tokenReaderWriter = tokenReaderWriter;
    }

    @Override
    public Request authenticate(@NonNull Route route, @NonNull Response response) throws IOException {
        Request request = response.request();

        String oldToken = tokenReaderWriter.read(request);
        if (oldToken != null) {
            if (request.header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY) != null) {
                synchronized (AuthenticationService.ANONYMOUS_AUTH_REFRESH_TOKEN_LOCK) {
                    String token;
                    try {
                        token = authenticationService.getAnonymousToken(false);
                    } catch (Exception e) {
                        throw new IOException("Error while trying to retrieve auth token: " + e.getMessage(), e);
                    }

                    if (oldToken.equals(token)) {
                        /*
                        if the token we receive from the AuthenticationService hasn't changed in
                        the meantime (e.g. due to another request having triggered a 401 and
                        re-authenticating before us getting here), try to get a new one
                        */
                        try {
                            token = authenticationService.getAnonymousToken(true);
                        } catch (Exception e) {
                            throw new IOException("Error while trying to retrieve auth token: " + e.getMessage(), e);
                        }
                    }

                    return tokenReaderWriter.write(
                            request.newBuilder()
                                    .removeHeader(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY)
                                    .build(),
                            token);
                }
            } else {
                return onAuthenticationFailed(request);
            }
        }

        return request;
    }

    /**
     * Called upon ultimately failed authentication.
     * <br><br>
     * The default implementation just returns {@code null} and thus cancels the request.
     * @return
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    protected Request onAuthenticationFailed(@NonNull Request request) {
        return null; // Give up, we've already failed to authenticate even after refreshing the token.
    }
}
