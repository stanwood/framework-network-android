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
