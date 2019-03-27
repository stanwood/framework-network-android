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

import io.stanwood.framework.network.core.util.ConnectionState
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * This class is used by okhttp to authenticate requests.
 */
class AuthInterceptor(
        private val connectionState: ConnectionState,
        private val authenticationProvider: AuthenticationProvider,
        private val tokenReaderWriter: TokenReaderWriter,
        private val onAuthenticationFailedListener: OnAuthenticationFailedListener?

) : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response =
        tokenReaderWriter.removeToken(chain.request()).newBuilder().let {
            if (connectionState.isConnected) {
                val token = synchronized(authenticationProvider.lock) {
                    try {
                        authenticationProvider.getToken(false)
                    } catch (e: AuthenticationException) {
                        onAuthenticationFailed()
                        throw e
                    }
                }

                it.header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY, "true")
                tokenReaderWriter.write(it.build(), token)
            } else {
                // we're offline, clean up headers for cache handling
                it.removeHeader(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY).build()
            }
        }.let {
            chain.proceed(it)
        }

    private fun onAuthenticationFailed() {
        onAuthenticationFailedListener?.onAuthenticationFailed(null)
    }
}
