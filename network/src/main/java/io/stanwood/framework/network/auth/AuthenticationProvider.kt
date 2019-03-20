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

/**
 * Main class to provide authentication information and locks. Used by
 * Authenticators and Interceptors. Also use this class during initial authentication
 * (i.e right after the user logs in or registers).
 *
 * Implement one for each authentication method!
 */
interface AuthenticationProvider {

    /**
     * Lock used by Authenticator / Auth Interceptor when requesting tokens. Provide a
     * final static Object here.
     *
     * @return lock
     */
    val lock: Any

    /**
     * Synchronously retrieves a token for authenticated access. MUST be run on a background thread
     * (which automatically is the case when this is called via the Authenticator or AuthInterceptor).
     *
     * A typical implementation usually looks like this:
     *
     * 1. check if we are the correct AuthenticationProvider for the currently used Authentication method
     * 2. check if there is already valid authentication data stored somewhere and use it if `forceRefresh`
     * isn't set
     * 3. otherwise get a fresh token synchronously and store it in the token storage.
     *
     * You can get a simple (non-encrypted!) token storage implementation when using the
     * [stanwood Android Plugin](https://github.com/stanwood/android-studio-plugin/).
     * The plugin will provide you with a basic implementation of such `AuthManager` by running
     * the _NetworkModule_ assistant in the _New -> Stanwood_ menu.
     *
     * @param forceRefresh whether a new token shall be retrieved from the server and not from cache
     * @return token
     *
     * @throws AuthenticationException if the token cannot be retrieved
     */
    @Throws(AuthenticationException::class)
    fun getToken(forceRefresh: Boolean): String
}
