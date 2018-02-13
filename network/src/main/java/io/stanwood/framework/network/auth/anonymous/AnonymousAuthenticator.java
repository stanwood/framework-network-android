package io.stanwood.framework.network.auth.anonymous;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import io.stanwood.framework.network.auth.AuthHeaderKeys;
import io.stanwood.framework.network.auth.AuthenticationService;
import io.stanwood.framework.network.auth.TokenReaderWriter;
import io.stanwood.framework.network.auth.authenticated.AuthenticatedAuthenticator;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * This class will be called by okhttp upon receiving a 401 from the server which means we should
 * usually retry the request with a fresh token.
 * <p>
 * It is NOT called during initially making a request. For that refer to
 * {@link AnonymousAuthInterceptor}.
 */
public class AnonymousAuthenticator implements Authenticator {

    @NonNull
    private final AuthenticationService authenticationService;
    @NonNull
    private final AnonymousAuthInterceptor anonymousAuthInterceptor;
    @Nullable
    private final AuthenticatedAuthenticator authenticatedAuthenticator;
    @NonNull
    private final TokenReaderWriter tokenReaderWriter;

    public AnonymousAuthenticator(
            @NonNull AuthenticationService authenticationService,
            @NonNull AnonymousAuthInterceptor anonymousAuthInterceptor,
            @Nullable AuthenticatedAuthenticator authenticatedAuthenticator,
            @NonNull TokenReaderWriter tokenReaderWriter
    ) {
        this.authenticationService = authenticationService;
        this.anonymousAuthInterceptor = anonymousAuthInterceptor;
        this.authenticatedAuthenticator = authenticatedAuthenticator;
        this.tokenReaderWriter = tokenReaderWriter;
    }

    @Override
    public Request authenticate(@NonNull Route route, @NonNull Response response) throws IOException {
        Request request;
        if (authenticatedAuthenticator != null && authenticationService.isUserSignedIn()) {
            // as we are already signed in we'll go ahead and try to log in with authentication
            final Request authenticatedRequest = authenticatedAuthenticator.authenticate(route, response);
            if (authenticatedRequest != null) {
                return authenticatedRequest;
            } else {
                /*
                at this point we've been signed out by the AuthenticatedAuthenticator due to some
                unrecoverable error and we're trying again anonymously
                */
                request = anonymousAuthInterceptor.getRequest(response.request(), authenticationService);
            }
        } else {
            request = response.request();
        }

        String oldToken = tokenReaderWriter.read(request);
        if (oldToken != null) {
            if (request.header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY) != null) {
                synchronized (authenticationService.getAnonymousLock()) {
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
                            tokenReaderWriter.removeToken(
                                    request.newBuilder()
                                            .removeHeader(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY)
                                            .build()),
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
     *
     * @return Request to be passed forward to okhttp
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    protected Request onAuthenticationFailed(@NonNull Request request) {
        return null; // Give up, we've already failed to authenticate even after refreshing the token.
    }
}
