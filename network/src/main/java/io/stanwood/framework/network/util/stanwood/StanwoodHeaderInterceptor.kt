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