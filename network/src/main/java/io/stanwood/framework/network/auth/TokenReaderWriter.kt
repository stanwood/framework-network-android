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

package io.stanwood.framework.network.auth

import okhttp3.Request

/**
 * Reads and writes tokens from/to okhttp requests.
 *
 * @see Authenticator
 *
 * @see AuthInterceptor
 */
interface TokenReaderWriter {

    /**
     * Reads a token from a given request.
     *
     * This is mainly used to compare the token we sent with the
     * one we have available via the [AuthenticationProvider] to check whether it has changed
     * since we issued the request or whether we should try to get a new token.
     *
     * Note, that this is NOT to read the token the server returns you when requesting a new one!
     *
     * @param request okhttp Request
     * @return the token found in the request, `null` if there is no token
     */
    fun read(request: Request): String?

    /**
     * Writes a token to the request (most probably either to a header or as an URL parameter.
     *
     * You don't need to explicitly remove possibly existing tokens as [removeToken]
     * will be called for you before.
     *
     * @param request okhttp Request
     * @param token the token to write
     * @return the Request with token
     */
    fun write(request: Request, token: String?): Request

    /**
     * Removes the token from the given request.
     * @param request okhttp Request
     * @return the Request without token
     */
    fun removeToken(request: Request): Request
}
