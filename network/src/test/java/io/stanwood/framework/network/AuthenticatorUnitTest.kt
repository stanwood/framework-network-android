package io.stanwood.framework.network

import io.appflate.restmock.JVMFileParser
import io.appflate.restmock.RESTMockServer
import io.appflate.restmock.RESTMockServer.whenGET
import io.appflate.restmock.RESTMockServerStarter
import io.appflate.restmock.utils.RequestMatchers.pathEndsWith
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.slot
import io.stanwood.framework.network.api.TestApi
import io.stanwood.framework.network.auth.AuthInterceptor
import io.stanwood.framework.network.auth.AuthenticationException
import io.stanwood.framework.network.auth.AuthenticationProvider
import io.stanwood.framework.network.auth.Authenticator
import io.stanwood.framework.network.auth.TokenReaderWriter
import io.stanwood.framework.network.util.ConnectionState
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class AuthenticatorUnitTest {

    @MockK
    private lateinit var tokenReaderWriter: TokenReaderWriter

    @MockK
    private lateinit var authenticationProvider: AuthenticationProvider

    @MockK
    private lateinit var connectionState: ConnectionState

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        RESTMockServerStarter.startSync(JVMFileParser())
    }

    @After
    fun tearDown() {
        RESTMockServer.shutdown()
    }

    @Test(expected = AuthenticationException::class)
    fun `When request fails with 401 and reauth fails with 401 as well we get an AuthenticationException`() {
        whenGET(pathEndsWith("/user")).thenReturnEmpty(401)
        every { connectionState.isConnected } returns true
        every { tokenReaderWriter.read(any()) } returns "some_token"
        slot<Request>().let { request ->
            every { tokenReaderWriter.removeToken(capture(request)) } answers { request.captured }
        }
        slot<Request>().let { request ->
            every { tokenReaderWriter.write(capture(request), any()) } answers { request.captured }
        }
        every { authenticationProvider.getToken(any()) } returns "some_token" andThenThrows AuthenticationException()
        every { authenticationProvider.lock } returns Unit
        val authenticator = Authenticator(authenticationProvider, tokenReaderWriter, null)
        val authInterceptor =
                AuthInterceptor(connectionState, authenticationProvider, tokenReaderWriter, null)
        getApi(authenticator, authInterceptor).getUser().execute()
    }

    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        assertEquals(4, (2 + 2).toLong())
    }

    private fun getApi(authenticator: Authenticator, authInterceptor: AuthInterceptor) =
            Retrofit.Builder()
                    .baseUrl(RESTMockServer.getUrl())
                    .client(
                            OkHttpClient.Builder()
                                    .readTimeout(10, TimeUnit.SECONDS)
                                    .connectTimeout(10, TimeUnit.SECONDS)
                                    .addInterceptor(HttpLoggingInterceptor())
                                    .authenticator(authenticator)
                                    .addInterceptor(authInterceptor)
                                    .build()
                    )
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(TestApi::class.java)
}