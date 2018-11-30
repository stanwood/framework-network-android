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

import java.io.IOException

/**
 * Exception class used by [AuthInterceptor] and in the future by [Authenticator] to
 * indicate issues during token retrieval.
 *
 * Right now we cannot use this for the Authenticator due to a likely
 * [bug in okhttp](https://github.com/square/okhttp/issues/3872) which keeps the
 * connection open when throwing exceptions in Authenticators.
 */
class AuthenticationException(message: String = DEFAULT_MESSAGE, cause: Throwable? = null) :
    IOException(message, cause) {

    companion object {

        private const val DEFAULT_MESSAGE = "Error while trying to retrieve auth token"
    }
}
