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
package io.stanwood.framework.network.core.store

import com.nytimes.android.external.store3.base.Parser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import okio.BufferedSource
import java.io.Reader

object SerializationParserFactory {
    private val stable = Json {
        allowStructuredMapKeys = true
    }
    fun <T> createReaderParser(
        deserializer: KSerializer<T>,
        json: Json = stable
    ): Parser<Reader, T> =
        SerializationReaderParser(deserializer, json)

    fun <T> createSourceParser(
        deserializer: KSerializer<T>,
        json: Json = stable
    ): Parser<BufferedSource, T> = SerializationSourceParser(deserializer, json)

    fun <T> createStringParser(
        deserializer: KSerializer<T>,
        json: Json = stable
    ): Parser<String, T> =
        SerializationStringParser(deserializer, json)
}