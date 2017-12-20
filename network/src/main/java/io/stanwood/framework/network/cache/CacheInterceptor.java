package io.stanwood.framework.network.cache;

import android.Manifest;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresPermission;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class CacheInterceptor implements Interceptor {

    private final ConnectivityManager connectivityManager;
    @Nullable
    private final String queryAuthParameterKey;
    @Nullable
    private final ErrorCallback errorCallback;

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public CacheInterceptor(
            @NonNull Context applicationContext,
            @Nullable String queryAuthParameterKey,
            @Nullable ErrorCallback errorCallback) {
        connectivityManager = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        this.queryAuthParameterKey = queryAuthParameterKey;
        this.errorCallback = errorCallback;
    }

    @Override
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request request = chain.request();

        String offlineCacheHeader = request.header(RequestConstants.APPLY_OFFLINE_CACHE);
        if (offlineCacheHeader != null && Boolean.valueOf(offlineCacheHeader)) {
            if (!isConnected()) {
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
                        .removeHeader(RequestConstants.APPLY_OFFLINE_CACHE)
                        .removeHeader(RequestConstants.APPLY_RESPONSE_CACHE)
                        .build();
                return chain.proceed(request);
            }
        }

        String responseCacheHeader = request.header(RequestConstants.REFRESH);
        if (responseCacheHeader != null && Boolean.valueOf(responseCacheHeader)) {
            try {
                return chain.proceed(request.newBuilder().cacheControl(CacheControl.FORCE_NETWORK).build());
            } catch (IOException e) {
                if (errorCallback != null) {
                    errorCallback.onError(e);
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
                        .removeHeader(RequestConstants.APPLY_OFFLINE_CACHE)
                        .removeHeader(RequestConstants.APPLY_RESPONSE_CACHE)
                        .build());
            }
        } else {
            return chain.proceed(request);
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private boolean isConnected() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public interface ErrorCallback {
        void onError(IOException e);
    }
}
