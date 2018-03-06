package io.stanwood.framework.network.auth;

/**
 * A collection of header keys used by {@link AuthInterceptor} AND {@link Authenticator}
 * as well as their signed-in variants.
 */
@SuppressWarnings("WeakerAccess")
public abstract class AuthHeaderKeys {

    /**
     * This header is set by the Authenticators / Auth Interceptors to determine when to retry a
     * request with a fresh token.
     */
    public static final String RETRY_WITH_REFRESH_HEADER_KEY = "RetryWithRefresh";
}
