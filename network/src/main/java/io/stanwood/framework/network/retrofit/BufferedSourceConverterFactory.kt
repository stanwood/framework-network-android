package io.stanwood.framework.network.retrofit

import okhttp3.ResponseBody
import okio.BufferedSource
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class BufferedSourceConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *>? =
        if (BufferedSource::class.java.canonicalName != (type as Class<*>).canonicalName) {
            null
        } else {
            Converter<ResponseBody, BufferedSource> { value -> value.source() }
        }

    companion object {
        fun create() = BufferedSourceConverterFactory()
    }
}