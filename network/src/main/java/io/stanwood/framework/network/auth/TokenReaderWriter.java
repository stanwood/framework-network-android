/*
 * Copyright (c) 2018 stanwood GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
