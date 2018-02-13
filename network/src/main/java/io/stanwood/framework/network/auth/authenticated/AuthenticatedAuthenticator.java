package io.stanwood.framework.network.auth.authenticated;

import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import io.stanwood.framework.network.auth.AuthHeaderKeys;
import io.stanwood.framework.network.auth.AuthenticationProvider;
import io.stanwood.framework.network.auth.TokenReaderWriter;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;

public class AuthenticatedAuthenticator implements Authenticator {

    @NonNull
    private final AuthenticationProvider authenticationProvider;
    @NonNull
    private final TokenReaderWriter tokenReaderWriter;
    @Nullable
    private final OnAuthenticationFailedListener onAuthenticationFailedListener;

    public AuthenticatedAuthenticator(
            @NonNull AuthenticationProvider authenticationProvider,
            @NonNull TokenReaderWriter tokenReaderWriter,
            @Nullable OnAuthenticationFailedListener onAuthenticationFailedListener
    ) {
        this.authenticationProvider = authenticationProvider;
        this.tokenReaderWriter = tokenReaderWriter;
        this.onAuthenticationFailedListener = onAuthenticationFailedListener;
    }

    @Override
    public Request authenticate(@NonNull Route route, @NonNull Response response) throws IOException {
        Request request = response.request();

        String oldToken = tokenReaderWriter.read(request);
        if (oldToken != null) {
            if (request.header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY) != null) {
                synchronized (authenticationProvider.getAuthenticatedLock()) {
                    String token;
                    try {
                        token = authenticationProvider.getAuthenticatedToken(false);
                    } catch (Exception e) {
                        // TODO should we sign out in this case as well?
                        throw new IOException("Error while trying to retrieve auth token: " + e.getMessage(), e);
                    }

                    if (oldToken.equals(token)) {
                        /*
                        if the token we receive from the AuthenticationProvider hasn't changed in
                        the meantime (e.g. due to another request having triggered a 401 and
                        re-authenticating before us getting here), try to get a new one
                        */
                        try {
                            token = authenticationProvider.getAuthenticatedToken(true);
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
                if (authenticationProvider.isUserSignedIn()) {
                    authenticationProvider.signOut();
                }
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
     * @return Request to be passed forward to okhttp, can be {@code null}
     */
    @SuppressWarnings("WeakerAccess")
    @CallSuper
    @Nullable
    protected Request onAuthenticationFailed(@NonNull Request request) {
        // Give up, we've already failed to authenticate even after refreshing the token.
        if (onAuthenticationFailedListener != null) {
            onAuthenticationFailedListener.onAuthenticationFailed(request);
        }
        return null;
    }

    public interface OnAuthenticationFailedListener {
        void onAuthenticationFailed(@NonNull Request request);
    }
}
