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

package io.stanwood.framework.network.cache;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import io.stanwood.framework.network.core.util.ConnectionState;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class works closely together with {@link CacheNetworkInterceptor} to bring offline and regular
 * caching to you no matter what the server tells you about caching (that is, all relevant caching
 * headers returned by the server or set by you before the interceptors run are overridden!).
 *
 * <p>Add it as an (app-)interceptor to your OkHttpClient Builder like so:
 * <pre>
 * {@code okHttpClientBuilder.addInterceptor(new CacheInterceptor(context, "auth", null))}
 * </pre>
 *
 * <p>Check out the documentation of {@link CacheNetworkInterceptor} for more information on when to
 * use these classes.
 *
 * @see CacheNetworkInterceptor
 */
public class CacheInterceptor implements Interceptor {

    @NonNull
    private ConnectionState connectionState;
    @Nullable
    private final String queryAuthParameterKey;
    @Nullable
    private final ErrorCallback errorCallback;

    /**
     * Constructs a new CacheInterceptor instance.
     *
     * @param applicationContext the app context for checking the network state
     * @param queryAuthParameterKey @param queryAuthParameterKey the name of any auth query parameter to be removed before caching
     *                             or {@code null} if there is none
     * @param errorCallback an optional callback which is called in case we receive a connection issue
     *                      and have to fall back to fetching from the response cache (even if it is
     *                      stale) - useful for logging, but you can also modify the request before
     *                      it is used to check the cache.
     */
    public CacheInterceptor(
            @NonNull Context applicationContext,
            @Nullable String queryAuthParameterKey,
            @Nullable ErrorCallback errorCallback) {
        connectionState = new ConnectionState(applicationContext);
        this.queryAuthParameterKey = queryAuthParameterKey;
        this.errorCallback = errorCallback;
    }

    @NotNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        String offlineCacheHeader = request.header(CacheHeaderKeys.APPLY_OFFLINE_CACHE);
        if (Boolean.parseBoolean(offlineCacheHeader) && !connectionState.isConnected()) {
            Request.Builder builder = request.newBuilder();
            if (queryAuthParameterKey != null) {
                builder.url(request
                        .url()
                        .newBuilder()
                        .removeAllQueryParameters(queryAuthParameterKey)
                        .build());
            }
            request = builder
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .removeHeader(CacheHeaderKeys.APPLY_OFFLINE_CACHE)
                    .removeHeader(CacheHeaderKeys.APPLY_RESPONSE_CACHE)
                    .build();
            return chain.proceed(request);
        }

        String responseCacheHeader = request.header(CacheHeaderKeys.REFRESH);
        if (Boolean.parseBoolean(responseCacheHeader)) {
            try {
                return chain.proceed(request.newBuilder().cacheControl(CacheControl.FORCE_NETWORK).build());
            } catch (IOException e) {
                if (errorCallback != null) {
                    request = errorCallback.onError(e, request);
                }
                // connection issue, try to get data from cache (even if it's stale)
                Request.Builder builder = request.newBuilder();
                if (queryAuthParameterKey != null) {
                    builder.url(request
                            .url()
                            .newBuilder()
                            .removeAllQueryParameters(queryAuthParameterKey)
                            .build());
                }
                return chain.proceed(builder
                        .cacheControl(CacheControl.FORCE_CACHE)
                        .removeHeader(CacheHeaderKeys.APPLY_OFFLINE_CACHE)
                        .removeHeader(CacheHeaderKeys.APPLY_RESPONSE_CACHE)
                        .build());
            }
        } else {
            return chain.proceed(request);
        }
    }

    public interface ErrorCallback {
        Request onError(IOException e,  Request request);
    }
}
