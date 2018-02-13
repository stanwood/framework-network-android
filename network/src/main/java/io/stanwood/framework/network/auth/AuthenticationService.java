package io.stanwood.framework.network.auth;

import java.io.IOException;

public interface AuthenticationService {

    /**
     * Lock used by anonymous Authenticator / Auth Interceptor when requesting tokens
     */
    Object ANONYMOUS_AUTH_REFRESH_TOKEN_LOCK = new Object();

    /**
     * Retrieves a token for authenticated access
     *
     * @param forceRefresh whether a new token shall be retrieved from the server and not from cache
     * @return token
     */
    String getToken(boolean forceRefresh);

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
