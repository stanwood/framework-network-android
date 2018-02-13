package io.stanwood.framework.network.auth;

import io.stanwood.framework.network.auth.anonymous.AnonymousAuthInterceptor;
import io.stanwood.framework.network.auth.anonymous.AnonymousAuthenticator;

/**
 * A collection of header keys used by {@link AnonymousAuthInterceptor} AND {@link AnonymousAuthenticator}
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
