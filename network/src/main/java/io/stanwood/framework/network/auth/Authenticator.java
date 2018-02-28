package io.stanwood.framework.network.auth;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

/**
 * This class will be called by okhttp upon receiving a 401 from the server which means we should
 * usually retry the request with a fresh token.
 * <p>
 * It is NOT called during initially making a request. For that refer to
 * {@link AuthInterceptor}.
 */
public class Authenticator implements okhttp3.Authenticator {

    @NonNull
    private final AuthenticationProvider authenticationProvider;
    @NonNull
    private final TokenReaderWriter tokenReaderWriter;
    @Nullable
    private final OnAuthenticationFailedListener onAuthenticationFailedListener;

    public Authenticator(
            @NonNull AuthenticationProvider authenticationProvider,
            @NonNull TokenReaderWriter tokenReaderWriter,
            @Nullable OnAuthenticationFailedListener onAuthenticationFailedListener
    ) {
        this.authenticationProvider = authenticationProvider;
        this.tokenReaderWriter = tokenReaderWriter;
        this.onAuthenticationFailedListener = onAuthenticationFailedListener;
    }

    @Override
    public Request authenticate(@NonNull Route route, @NonNull Response response) {
        Request request = response.request();

        String oldToken = tokenReaderWriter.read(request);
        if (oldToken != null) {
            synchronized (authenticationProvider.getLock()) {
                if (request.header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY) != null) {
                        // Authentication failed. Try to re-authenticate with fresh token
                        String token;
                        try {
                            token = authenticationProvider.getToken(false);
                        } catch (AuthenticationException e) {
                            /*
                            TODO as soon as the bug in okhttp as described in AuthenticationException
                            has been resolved we're going to rethrow the exception here if retryOrFail()
                            returns null
                            */
                            return retryOrFail(response);
                        }

                        if (oldToken.equals(token)) {
                            /*
                            if the token we receive from the AuthenticationProvider hasn't changed in
                            the meantime (e.g. due to another request having triggered a 401 and
                            re-authenticating before us getting here), try to get a new one
                            */
                            try {
                                token = authenticationProvider.getToken(true);
                            } catch (AuthenticationException e) {
                                /*
                                TODO as soon as the bug in okhttp as described in AuthenticationException
                                has been resolved we're going to rethrow the exception here if retryOrFail()
                                returns null
                                */
                                return retryOrFail(response);
                            }

                            if (token == null) {
                                return retryOrFail(response);
                            }

                        }

                        return tokenReaderWriter.write(
                                tokenReaderWriter.removeToken(
                                        request.newBuilder()
                                                .removeHeader(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY)
                                                .build()),
                                token);
                } else {
                    // Give up, we've already failed to authenticate even after refreshing the token.
                    return retryOrFail(response);
                }
            }
        }

        return request;
    }

    private Request retryOrFail(@NonNull Response response) {
        Request request = onAuthenticationFailed(response);
        if (request == null) {
            if (onAuthenticationFailedListener != null) {
                onAuthenticationFailedListener.onAuthenticationFailed(response);
            }
        }
        return request;
    }

    /**
     * Called upon ultimately failed authentication.
     * <br><br>
     * Usually that means that we couldn't satisfy the challenge the server asked us for.
     * <br><br>
     * The default implementation just returns {@code null} and thus cancels the request. You can
     * return another Request here to attempt another try.
     * <br><br>
     * When overriding make sure to provide measurements against endless loops, this is usually
     * done by adding headers to the request - by no means try to store information in the
     * Authenticator as the same instance is used throughout ALL requests.
     *
     * @return Response containing the failed Request
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    protected Request onAuthenticationFailed(@SuppressWarnings("unused") @NonNull Response response) {
        return null;
    }

}
