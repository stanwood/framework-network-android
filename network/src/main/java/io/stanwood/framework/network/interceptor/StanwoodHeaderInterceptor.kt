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
package io.stanwood.framework.network.interceptor

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