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
import okhttp3.Response;
import okhttp3.Route;

/**
 * This class will be called by okhttp upon receiving a 401 from the server which means we should
 * usually retry the request with a fresh token.
 * <p>
 * It is NOT called during initially making a request. For that refer to
 * {@link AuthInterceptor}.
 */
public class Authenticator implements okhttp3.Authenticator {

    @NonNull
    private final AuthenticationProvider authenticationProvider;
    @NonNull
    private final TokenReaderWriter tokenReaderWriter;
    @Nullable
    private final OnAuthenticationFailedListener onAuthenticationFailedListener;

    public Authenticator(
            @NonNull AuthenticationProvider authenticationProvider,
            @NonNull TokenReaderWriter tokenReaderWriter,
            @Nullable OnAuthenticationFailedListener onAuthenticationFailedListener
    ) {
        this.authenticationProvider = authenticationProvider;
        this.tokenReaderWriter = tokenReaderWriter;
        this.onAuthenticationFailedListener = onAuthenticationFailedListener;
    }

    @Override
    public Request authenticate(@Nullable Route route, @NonNull Response response) {
        Request request = response.request();

        String oldToken = tokenReaderWriter.read(request);
        if (oldToken != null) {
            synchronized (authenticationProvider.getLock()) {
                if (request.header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY) != null) {
                        // Authentication failed. Try to re-authenticate with fresh token
                        String token;
                        try {
                            /*
                            do not force refresh the token as we might already have gotten a new
                            one due to another request having triggered a 401 and re-authenticating
                            before us getting here
                            */
                            token = authenticationProvider.getToken(false);
                        } catch (AuthenticationException e) {
                            /*
                            TODO as soon as the bug in okhttp as described in AuthenticationException
                            has been resolved we're going to rethrow the exception here if retryOrFail()
                            returns null
                            */
                            return retryOrFail(route, response);
                        }

                        if (oldToken.equals(token)) {
                            /*
                            if the token we receive from the AuthenticationProvider hasn't changed in
                            the meantime, try to get a new one
                            */
                            try {
                                token = authenticationProvider.getToken(true);
                            } catch (AuthenticationException e) {
                                /*
                                TODO as soon as the bug in okhttp as described in AuthenticationException
                                has been resolved we're going to rethrow the exception here if retryOrFail()
                                returns null
                                */
                                return retryOrFail(route, response);
                            }

                            if (token == null || oldToken.equals(token)) {
                                return retryOrFail(route, response);
                            }

                        }

                        return tokenReaderWriter.write(
                                tokenReaderWriter.removeToken(
                                        request.newBuilder()
                                                .removeHeader(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY)
                                                .build()),
                                token);
                } else {
                    // Give up, we've already failed to authenticate even after refreshing the token.
                    return retryOrFail(route, response);
                }
            }
        }

        return request;
    }

    private Request retryOrFail(@Nullable Route route, @NonNull Response response) {
        Request request = onAuthenticationFailed(
                route,
                /*
                make sure the RetryWithRefresh header is set so that subclasses overriding
                onAuthenticationFailed() can call through to authenticate() after e.g. clearing
                all tokens
                */
                response.newBuilder().request(
                        response.request().newBuilder()
                                .header(AuthHeaderKeys.RETRY_WITH_REFRESH_HEADER_KEY, "true")
                                .build()
                ).build()
        );
        if (request == null && onAuthenticationFailedListener != null) {
            onAuthenticationFailedListener.onAuthenticationFailed(response);
        }
        return request;
    }

    /**
     * Called upon ultimately failed authentication.
     * <br><br>
     * Usually that means that we couldn't satisfy the challenge the server asked us for.
     * <br><br>
     * The default implementation just returns {@code null} and thus cancels the request. You can
     * return another Request here to attempt another try.
     * <br><br>
     * When overriding make sure to provide measurements against endless loops, this is usually
     * done by adding headers to the request - by no means try to store information in the
     * Authenticator as the same instance is used throughout ALL requests.
     *
     * @return Response containing the failed Request
     */
    @SuppressWarnings("WeakerAccess")
    @Nullable
    protected Request onAuthenticationFailed(
            @SuppressWarnings("unused") @Nullable Route route,
            @SuppressWarnings("unused") @NonNull Response response
    ) {
        return null;
    }

}
