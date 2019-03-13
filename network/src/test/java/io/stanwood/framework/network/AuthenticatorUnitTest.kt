package io.stanwood.framework.network

import io.appflate.restmock.JVMFileParser
import io.appflate.restmock.RESTMockServer
import io.appflate.restmock.RESTMockServer.whenGET
import io.appflate.restmock.RESTMockServerStarter
import io.appflate.restmock.utils.RequestMatchers.pathEndsWith
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.stanwood.framework.network.api.TestApi
import io.stanwood.framework.network.auth.AuthInterceptor
import io.stanwood.framework.network.auth.AuthenticationException
import io.stanwood.framework.network.auth.AuthenticationProvider
import io.stanwood.framework.network.auth.Authenticator
import io.stanwood.framework.network.auth.TokenReaderWriter
import okhttp3.OkHttpClient
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

    @RelaxedMockK
    private lateinit var tokenReaderWriter: TokenReaderWriter

    @RelaxedMockK
    private lateinit var authenticationProvider: AuthenticationProvider

    // FIXME to test properly we need to use a real instance of AuthInterceptor (setup as per documentation)
    // that one needs a context though - so it might be good practice to remove that context (which we only
    // need for connection checking anyway) and pass a ConnectionState instead
    private lateinit var authInterceptor: AuthInterceptor

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
        whenGET(pathEndsWith("/user"))
                .thenReturnEmpty(401)
                .thenReturnEmpty(401)
        val authenticator = Authenticator(authenticationProvider, tokenReaderWriter, null)
        every { tokenReaderWriter.read(any()) } returns "some_token" andThen "some_other_token"
        every { authenticationProvider.getToken(any()) } returns "some_token" andThen "some_other_token"
        getApi(authenticator).getUser().execute()
    }

    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        assertEquals(4, (2 + 2).toLong())
    }

    private fun getApi(authenticator: Authenticator) =
            Retrofit.Builder()
                    .baseUrl(RESTMockServer.getUrl())
                    .client(
                            OkHttpClient.Builder()
                                    .readTimeout(10, TimeUnit.SECONDS)
                                    .connectTimeout(10, TimeUnit.SECONDS)
                                    .addInterceptor(HttpLoggingInterceptor())
                                    .authenticator(authenticator)
//                                    .addInterceptor(authInterceptor)
                                    .build()
                    )
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(TestApi::class.java)
}