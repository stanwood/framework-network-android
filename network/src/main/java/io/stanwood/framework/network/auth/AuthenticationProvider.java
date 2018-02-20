package io.stanwood.framework.network.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

/**
 * Main class to provide authentication information and locks. Used by
 * Authenticators and Interceptors.
 * <br><br>
 * Implement one for each authentication method!
 */
public interface AuthenticationProvider {

    /**
     * Lock used by Authenticator / Auth Interceptor when requesting tokens. Provide a
     * final static Object here.
     *
     * @return lock
     */
    @NonNull
    Object getLock();

    /**
     * Retrieves a token for authenticated access
     *
     * @param forceRefresh whether a new token shall be retrieved from the server and not from cache
     * @return token
     *
     * @throws IOException if the token cannot be retrieved
     */
    @Nullable
    String getToken(boolean forceRefresh) throws IOException;
}
