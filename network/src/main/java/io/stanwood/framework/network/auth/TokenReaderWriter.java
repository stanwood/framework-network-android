package io.stanwood.framework.network.auth;

import okhttp3.Request;

/**
 * Reads and writes tokens from/to okhttp requests.
 *
 * @see AnonymousAuthenticator
 */
public interface TokenReaderWriter {

    /**
     * Reads a token from a given request.
     * <br><br>
     * This is mainly used to compare the token we sent with the
     * one we have available via the {@link AuthenticationService} to check whether it has changed
     * since we issued the request or whether we should try to get a new token.
     * <br><br>
     * Note, that this is NOT to read the token the server returns you when requesting a new one!
     *
     * @param request okhttp Request
     * @return the token found in the request
     */
    String read(Request request);

    /**
     * Writes a token to the request (most probably either to a header or as an URL parameter.
     * <br><br>
     * Ensure to delete/override any existing tokens!
     *
     * @param request okhttp Request
     * @param token the token to write
     * @return
     */
    Request write(Request request, String token);
}
