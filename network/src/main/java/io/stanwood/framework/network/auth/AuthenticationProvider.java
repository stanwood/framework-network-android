package io.stanwood.framework.network.auth;

import java.io.IOException;

/**
 * Main class to provide authentication information and means to sign out the user. Used by the
 * various Authenticators and Interceptors.
 * <br><br>
 * Might be split up into three classes (anonymous -|> base <|- authenticated) in the future to
 * better separate implementations
 */
public interface AuthenticationProvider {

    /**
     * Lock used by anonymous Authenticator / Auth Interceptor when requesting tokens. Provide a
     * final static Object here.
     */
    Object getAnonymousLock();

    /**
     * Lock used by anonymous Authenticator / Auth Interceptor when requesting tokens. Provide a
     * final static Object here.
     */
    Object getAuthenticatedLock();

    /**
     * Retrieves a token for authenticated access
     *
     * @param forceRefresh whether a new token shall be retrieved from the server and not from cache
     * @return token
     */
    String getAuthenticatedToken(boolean forceRefresh) throws IOException;

    /**
     * Retrieves a token for unauthenticated access
     *
     * @param forceRefresh whether a new token shall be retrieved from the server and not from cache
     * @return token
     */
    String getAnonymousToken(boolean forceRefresh) throws IOException;

    /**
     * Checks whether the user is logged in (not anonymously!).
     *
     * @return whether the user is logged in
     */
    boolean isUserSignedIn();

    /**
     * Signs out the user.
     * <br><br>
     * Called when authentication fails permanently while being in authenticated mode.
     */
    void signOut();
}
