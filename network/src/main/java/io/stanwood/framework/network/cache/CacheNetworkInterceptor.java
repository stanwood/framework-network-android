package io.stanwood.framework.network.cache;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CacheNetworkInterceptor implements Interceptor {

    @Nullable
    private final String queryAuthParameterKey;

    public CacheNetworkInterceptor(@Nullable String queryAuthParameterKey) {
        this.queryAuthParameterKey = queryAuthParameterKey;
    }

    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();
        String responseCacheHeader = request.header(RequestConstants.APPLY_RESPONSE_CACHE);
        String offlineCacheHeader = request.header(RequestConstants.APPLY_OFFLINE_CACHE);
        boolean isGeneralCache = responseCacheHeader != null && Boolean.valueOf(responseCacheHeader);
        boolean isOfflineCache = offlineCacheHeader != null && Boolean.valueOf(offlineCacheHeader);
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
            return "public, max-age=" + 3600;
        } else {
            // only put into cache for offline cache, other than that cache will not be used
            return "public, max-age=" + 0;
        }
    }
}
