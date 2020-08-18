/*
 * Copyright (c) 2019 stanwood GmbH
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
import okhttp3.Response
import okhttp3.Route

/**
 * This class will be called by okhttp upon receiving a 401 from the server which means we should
 * usually retry the request with a fresh token.
 *
 * It is NOT called during initially making a request. For that refer to
 * [AuthInterceptor].
 */
open class Authenticator(
    private val authenticationProvider: AuthenticationProvider,
    private val tokenReaderWriter: TokenReaderWriter,
    private val onAuthenticationFailedListener: OnAuthenticationFailedListener?
) : okhttp3.Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? = response.request.let { request ->
        tokenReaderWriter.read(request)?.let { oldToken ->
            synchronized(authenticationProvider.lock) {
                if (request.header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY) != null) {
                    // Authentication failed. Try to re-authenticate with fresh token
                    try {
                        /*
                        do not force refresh the token as we might already have gotten a new
                        one due to another request having triggered a 401 and re-authenticating
                        before us getting here
                        */
                        authenticationProvider.getToken(false)
                    } catch (e: AuthenticationException) {
                        retryOrFail(route, response)?.let {
                            return it
                        } ?: throw e
                    }.let {
                        when (it) {
                            oldToken -> try {
                                /*
                                if the token we receive from the AuthenticationProvider hasn't changed in
                                the meantime, try to get a fresh one
                                */
                                authenticationProvider
                                        .getToken(true)
                                        .takeUnless { newToken -> oldToken == newToken }
                                        ?: return retryOrFail(route, response)
                            } catch (e: AuthenticationException) {
                                retryOrFail(route, response)?.let { request ->
                                    return request
                                } ?: throw e
                            }
                            else -> it
                        }
                    }.let { newToken ->
                        tokenReaderWriter.write(
                            tokenReaderWriter.removeToken(
                                request.newBuilder()
                                    .removeHeader(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY)
                                    .build()
                            ),
                            newToken
                        )
                    }
                } else {
                    // Give up, we've already failed to authenticate even after refreshing the token.
                    retryOrFail(route, response)
                }
            }
        }
    }

    private fun retryOrFail(route: Route?, response: Response): Request? = onAuthenticationFailed(
        route,
        /*
        make sure the RetryWithRefresh header is set so that subclasses overriding
        onAuthenticationFailed() can call through to authenticate() after e.g. clearing
        all tokens
        */
        response.newBuilder().request(
            response.request.newBuilder()
                .header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY, "true")
                .build()
        ).build()
    )?.apply {
        onAuthenticationFailedListener?.onAuthenticationFailed(response)
    }

    /**
     * Called upon ultimately failed authentication.
     *
     * Usually that means that we couldn't satisfy the challenge the server asked us for.
     *
     * The default implementation just returns `null` and thus cancels the request. You can
     * return another Request here to attempt another try.
     *
     * When overriding make sure to provide measurements against endless loops, this is usually
     * done by adding headers to the request - by no means try to store information in the
     * Authenticator as the same instance is used throughout ALL requests.
     *
     * @param route the [Route] for the request that failed
     * @param response the [Response] we received from the server
     *
     * @return Response containing the failed Request
     */
    protected open fun onAuthenticationFailed(
        route: Route?,
        response: Response
    ): Request? = null
}
