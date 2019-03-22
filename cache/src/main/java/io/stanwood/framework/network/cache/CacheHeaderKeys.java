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

/**
 * A collection of header keys used to configure requests handled by OkHttpClients which have the
 * {@link CacheInterceptor} AND the {@link CacheNetworkInterceptor} added.
 *
 * Add any combination of these headers to your requests and the interceptors will pick them up.
 */
@SuppressWarnings("WeakerAccess")
public class CacheHeaderKeys {

    private CacheHeaderKeys() {
        throw new IllegalStateException("Utility class, not meant to be instantiated");
    }

    /**
     * Allows caching this request for offline usage. That doesn't mean it is used as online cache.
     * That depends on the setting of the {@link #APPLY_RESPONSE_CACHE} header.
     */
    public final static String APPLY_OFFLINE_CACHE = "ApplyOfflineCache";

    /**
     * Allows caching this request for online usage. That doesn't mean it is used as offline cache.
     * That depends on the setting of the {@link #APPLY_OFFLINE_CACHE} header.
     */
    public final static String APPLY_RESPONSE_CACHE = "ApplyResponseCache";

    /**
     * Setting this header to {@code true} tells the interceptors to always try to fulfill the
     * request via network and disregard any cached response - only if that fails or if we are
     * offline and offline caching is allowed by means of the APPLY_OFFLINE_CACHE header the cache
     * is used.
     */
    public final static String REFRESH = "ForceNetwork";
}
