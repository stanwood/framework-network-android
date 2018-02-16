package io.stanwood.framework.network.auth;

import java.io.IOException;

/**
 * Main class to provide authentication information and means to sign out the user. Used by the
 * various Authenticators and Interceptors.
 * <br><br>
 * Implement one for authenticated and one for anonymous authentication!
 */
public abstract class AuthenticationProvider {

    /**
     * Lock used by Authenticator / Auth Interceptor when requesting tokens. Provide a
     * final static Object here.
     */
    public abstract Object getLock();

    /**
     * Retrieves a token for authenticated access
     *
     * @param forceRefresh whether a new token shall be retrieved from the server and not from cache
     * @return token
     */
    public abstract String getToken(boolean forceRefresh) throws IOException;

    /**
     * Checks whether the user is logged in (not anonymously!).
     *
     * @return whether the user is logged in, by default {@code false}
     */
    public boolean isUserSignedIn() {
        return false;
    }
}
