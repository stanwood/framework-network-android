package io.stanwood.framework.network.auth;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import okhttp3.Request;

/**
 * Reads and writes tokens from/to okhttp requests.
 *
 * @see Authenticator
 * @see AuthInterceptor
 */
public interface TokenReaderWriter {

    /**
     * Reads a token from a given request.
     * <br><br>
     * This is mainly used to compare the token we sent with the
     * one we have available via the {@link AuthenticationProvider} to check whether it has changed
     * since we issued the request or whether we should try to get a new token.
     * <br><br>
     * Note, that this is NOT to read the token the server returns you when requesting a new one!
     *
     * @param request okhttp Request
     * @return the token found in the request, {@code null} if there is no token
     */
    @Nullable
    String read(@NonNull Request request);

    /**
     * Writes a token to the request (most probably either to a header or as an URL parameter.
     * <br><br>
     * You don't need to explicitly remove possibly existing tokens as {@link #removeToken(Request)}
     * will be called for you before.
     *
     * @param request okhttp Request
     * @param token the token to write
     * @return the Request with token
     */
    @NonNull
    Request write(@NonNull Request request, @Nullable String token);

    /**
     * Removes the token from the given request.
     * @param request okhttp Request
     * @return the Request without token
     */
    @NonNull
    Request removeToken(@NonNull Request request);
}
