package io.stanwood.framework.network.auth;

import java.io.IOException;

/**
 * Exception class used by {@link AuthInterceptor} and in the future by {@link Authenticator} to
 * indicate issues during token retrieval.
 * <br><br>
 * Right now we cannot use this for the Authenticator due to a likely
 * <a href="https://github.com/square/okhttp/issues/3872">bug in okhttp</a> which keeps the
 * connection open when throwing exceptions in Authenticators.
 */
public class AuthenticationException extends IOException {

    public static final String DEFAULT_MESSAGE = "Error while trying to retrieve auth token";

    public AuthenticationException() {
        super(DEFAULT_MESSAGE);
    }

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public AuthenticationException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }
}
