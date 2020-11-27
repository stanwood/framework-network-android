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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * This class works closely together with {@link CacheInterceptor} to bring offline and regular
 * caching to you no matter what the server tells you about caching (that is, all relevant caching
 * headers returned by the server or set by you before the interceptors run are overridden!).
 *
 * <p>Especially useful when trying to cache responses of APIs which discourage caching for one or the
 * other reason, but you absolutely need it (if only to handle offline cases without setting up a
 * more sophisticated persistence layer - that being said always prefer a proper persistence
 * layer instead of just relying on an HTTP cache which just isn't build for this and thus not
 * without flaws, but it still may serve in smaller use cases).
 *
 * <p>It does so by removing any auth query parameters from the response before caching so that the
 * Cache is hit when offline even if we don't have an auth token or another auth token at the moment.
 *
 * <p>This also means that you are responsible for clearing the Cache once the user logs out if your
 * app contains such a mechanic.
 *
 * <p>Keep in mind that you are also responsible to remove any other query parameters which are not
 * stable over time by means of adding more app- and network-interceptors before the ones provided by
 * this library. In the future this might be added to the provided interceptors by means of
 * accepting a list of parameter keys instead of just the one of the auth parameter.
 *
 * <p>Configuration of the interceptor classes is mainly done by means of request headers. Check
 * out {@link CacheHeaderKeys} for more details on what the different headers do.
 *
 * <p>Add it as a network interceptor to your OkHttpClient Builder like so:
 * <pre>
 * {@code okHttpClientBuilder.addNetworkInterceptor(new CacheNetworkInterceptor("auth", 3600))}
 * </pre>
 */
public class CacheNetworkInterceptor implements Interceptor {

    @Nullable
    private final String queryAuthParameterKey;
    private final long cacheForSeconds;

    /**
     * Constructs a new CacheNetworkInterceptor instance.
     *
     * @param queryAuthParameterKey the name of any auth query parameter to be removed before caching
     *                             or {@code null} if there is none
     * @param cacheForSeconds seconds for which we should generally cache in case the
     *                       {@link CacheHeaderKeys#APPLY_RESPONSE_CACHE} is set
     */
    public CacheNetworkInterceptor(@Nullable String queryAuthParameterKey, long cacheForSeconds) {
        this.queryAuthParameterKey = queryAuthParameterKey;
        this.cacheForSeconds = cacheForSeconds;
    }

    @NotNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        String responseCacheHeader = request.header(CacheHeaderKeys.APPLY_RESPONSE_CACHE);
        String offlineCacheHeader = request.header(CacheHeaderKeys.APPLY_OFFLINE_CACHE);
        boolean isGeneralCache = Boolean.parseBoolean(responseCacheHeader);
        boolean isOfflineCache = Boolean.parseBoolean(offlineCacheHeader);
        if (isGeneralCache || isOfflineCache) {
            Response originalResponse = chain.proceed(request);
            Response.Builder builder = originalResponse.newBuilder();
            if (queryAuthParameterKey != null) {
                builder.request(originalResponse
                        .request()
                        .newBuilder()
                        .url(request
                                .url()
                                .newBuilder()
                                .removeAllQueryParameters(queryAuthParameterKey)
                                .build())
                        .build());
            }
            return builder
                    .header("Cache-Control", getCacheControl(isGeneralCache))
                    .build();
        } else {
            return chain.proceed(request);
        }
    }

    @NonNull
    private String getCacheControl(boolean isGeneralCache) {
        if (isGeneralCache) {
            // cache data for an hour
            return "public, max-age=" + cacheForSeconds;
        } else {
            // only put into cache for offline cache, other than that cache will not be used
            return "public, max-age=" + 0;
        }
    }
}
