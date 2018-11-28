package io.stanwood.framework.network.util.stanwood

import okhttp3.Interceptor
import okhttp3.Response

/**
 * This class is used for stanwood apps only. It contains our default headers.
 *
 * Add it as an app-interceptor like so:
 *
 * ```
 * okHttpClientBuilder.addInterceptor(StanwoodHeaderInterceptor(...))
 * ```
 *
 * @param appName a **language independent** app identifier, it will be snail cased in the User-Agent header. Usually it is supplied via
 * either `application.getString(R.string.app_name)` (if language independent) or the flavor or a static `BuildConfig` String
 * @param versionName your app's version, usually as supplied by `BuildConfig.VERSION_NAME`
 * @param buildType your app's build type (like _debug_ or _release_) as supplied by `BuildConfig.BUILD_TYPE`
 *
 */
class StanwoodHeaderInterceptor(
    private val appName: String,
    private val versionName: String,
    private val buildType: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response =
        chain.request()
            .newBuilder()
            .header("User-Agent", "android-${appName.toSnailCase()}-$versionName-$buildType")
            .build()
            .let {
                chain.proceed(it)
            }

    private fun String.toSnailCase() = replace(" ", "_").toLowerCase()
}