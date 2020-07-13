[![Release](https://jitpack.io/v/stanwood/framework-network-android.svg?style=flat-square)](https://jitpack.io/#stanwood/framework-network-android)
[![Build Status](https://app.bitrise.io/app/983e6342cc5e0e24/status.svg?token=QtXUf2lbVhJrANROaTkluQ&branch=develop)](https://app.bitrise.io/app/983e6342cc5e0e24)
[![API](https://img.shields.io/badge/API-16%2B-blue.svg?style=flat)](https://android-arsenal.com/api?level=16)

# stanwood Network Utilities (Android)

A set of hopefully useful classes for common networking use cases.

## Import

The stanwood Network Utilities are hosted on JitPack. Therefore you can simply
import them by adding

```groovy
allprojects {
    repositories {
        ...
        maven { url "https://jitpack.io" }
    }
}
```

to your project's `build.gradle`.

Then add this to you app's `build.gradle`:

```groovy
dependencies {
    implementation 'com.github.stanwood.framework-network-android:auth:<insert latest version here>' // aar version available as well
    implementation 'com.github.stanwood.framework-network-android:cache:<insert latest version here>' // aar version available as well
    implementation 'com.github.stanwood.framework-network-android:core:<insert latest version here>' // aar version available as well, automatically included when pulling in one of the other libraries
}
```

## Usage

Refer to the extensive javadoc of the provided classes for details on how to
use them. Right now there are solutions for the following use cases:

- handle offline situations and caching when using OkHttp (`cache` library)
- generic token based authentication handling with OkHttp (`auth` library)
- generic network related utility classes (some specific to apps developed over here at _stanwood_) (`core`library)

### cache

See javadoc of the classes in the library.

### auth

The `auth` library contains classes for handling token based authentication with OkHttp. Generally this is done via Authenticators and Interceptors.

If you are using plain username/password authentication you obviously won't need the following classes as there will never be a need to refresh the data and authentication is straightforward.

Integration into both existing means of token retrieval as well as from scratch is simple.

First you need to implement both `TokenReaderWriter` and `AuthenticationProvider`. The first is used for reading (to find out whether we are still using an old token) and writing (for authentication) tokens from/to requests. The second provides means to get authentication data (such as tokens and sign-in status) and should also be used by you directly during initial authentication (i.e. right after user log-in/registration) for consistency reasons. The `AuthenticationProvider` usually makes use of an `AuthManager` to store retrieved login data. You can get a sample implementation of such an `AuthManager` by using the _stanwood Android Studio Plugin_ (see **AuthManager** section further below). Refer to the javadoc of the aforementioned classes for more details on how to implement these interfaces.

In the future optional modules will provide implementations for common use cases.

Then create an instance of `io.stanwood.framework.network.auth.Authenticator`:

```java
Authenticator authenticator = new Authenticator(
    authenticationProvider,
    tokenReaderWriter,
    null
);
```

And an instance of `AuthInterceptor`:

```java
AuthInterceptor authInterceptor = new AuthInterceptor(
    new ConnectionState(appContext),
    authenticationProvider,
    tokenReaderWriter,
    null
);
```

Construct an `OkHttpClient` and pass the interceptor and the authenticator:

```java
new OkHttpClient.Builder()
        .authenticator(authenticator) // takes care of 401 (invalid token, get fresh one and retry)
        .addInterceptor(authInterceptor) // adds authentication data to every request
        .build();
```

That's it. When using this `OkHttpClient` instance you'll benefit from fully transparent token handling.

__Hint 1:__ *If your app uses multiple authentication methods make sure to
implement an own subclass of `AuthenticationProvider` for each method and use
an own `OkHttpClient` for each!*

__Hint 2:__ *In case you are using OkHttp/Retrofit to retrieve tokens: As the
`Authenticator` locks all other `Authenticators` and `AuthInterceptors` with
the same `AuthenticationProvider` it makes sense to provide an own
`OkHttpClient` instance (and thus also an own Retrofit instance if you use it)
for all calls you need to receive new tokens. If try to serve all calls with
the same client instance, it may happen that you run out of connections/threads
while trying to get a token because there might be a whole slew of requests
already waiting for that token call to succeed. Alternatively you can also try
to increase the executor pool size, but this is not recommended as you never
know for sure how many requests are executed at a given point in time - and you
definitely always want a thread ready for your token request.*

__Hint 3:__ *Rules of thumb (exceptions apply):* 
1. one `AuthenticationProvider` per authentication method (however you can also try to wrap multiple ones in one `AuthenticationProvider` which can ease DI quite a bit - we recommend to define each method in one provider and then pass those providers to the wrapper provider to have clean separate implementations)
2. one `TokenReaderWriter` and `AuthManager` per Api

#### A note on authentication exception handling

The library provides an own exception class called `AuthenticationException`.
You can listen for this class in your Retrofit
`Callback.onFailure(Call, Throwable)` to check for authentication related
errors.

If you're having problems retrieving a token in the `AuthenticationProvider`
(e.g. due to an invalid refresh token) always try to resolve those issues there
as well if possible. Throwing an `AuthenticationException` should just be a
last resort.

Alternatively you can also subclass the `Authenticator` class and override
`onAuthenticationFailed()` if you want to trigger special handling for failed
authentication from which we couldn't recover with our default handling. This
usually tends to be a bit harder to get right as it expects you to modify the
failed request directly without any means of intercepting the response. Thus
handling token retrieval issues should preferably handled in the
`AuthenticationProvider` as explained above.

#### AuthManager

The [stanwood Android Plugin](https://github.com/stanwood/android-studio-plugin/) provides a template for an `AuthManager` which takes care of storing and providing authentication info like tokens and usernames. It will automatically be generated for you when running the _NetworkModule_ assistant in the _New -> Stanwood_ menu.

You can extend it with authentication types for all your authentication providers for easy and streamlined authentication state handling.

### core

The `util` package contains generic network related classes. The `util.stanwood`
package contains classes for apps developed over here at _stanwood_.

## Quick start for simple networking in our own stanwood apps

You can find an example [over here](https://github.com/stanwood/architecture_sample_github_android) (private for now).

Add the following dependencies (replace versions here with current versions):

```groovy
def retrofit_version = '2.5.0'
implementation "com.squareup.retrofit2:retrofit:$retrofit_version"
implementation "com.squareup.retrofit2:converter-gson:$retrofit_version"
implementation "com.squareup.retrofit2:adapter-rxjava2:$retrofit_version"

implementation 'com.google.code.gson:gson:2.8.5'

implementation 'com.squareup.okhttp3:logging-interceptor:3.13.1'
```

Write an interface class with Retrofit API definition and put it in the `datasources.net.<api>` package:

```kotlin
interface GithubApi {

    @Headers(
        "Accept: application/vnd.github.mercy-preview+json"
    )
    @GET("search/repositories")
    fun getMostPopularAndroidRepositories(
        @Query("q") query: String = "topic:Android",
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc"
    ): Single<SearchRepositoriesResponse>  // this is for RX
}
```

To define models classes use curl or postman or insomnia to execute the request and get a sample response. Then use the _JSON to Kotlin Class_ Android Studio plugin and paste the response there. We use GSON for de-/serialization of JSON so use the proper annotation settings in the plugin. Configure the plugin so that it uses `val`s and try the _Auto determine Nullable or not from JSON Value_ setting (make sure to revisit the generated classes later to check whether the properties are fine). Additionally switch on _Enable Map Type when JSON field key is primitive type_ in _other_ settings.

Put the generated classes in the `datasources.net.<api>.model` package.

Then define a `NetworkModule` similar to the following one:

```kotlin
@Module
class NetworkModule {

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setDateFormat("YYYY-MM-DDTHH:MM:SSZ")
        .create()

    @Provides
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

    @Provides
    fun provideGithubOkHttpClient(
        httpLoggingInterceptor: HttpLoggingInterceptor,
        application: Application
    ): OkHttpClient {
        val client = OkHttpClient.Builder()
            .readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .cache(Cache(application.cacheDir, CACHE_SIZE))
            .addInterceptor(TestfairyHttpInterceptor()) // only if you use the stanwood analytics library
            .addInterceptor(StanwoodHeaderInterceptor(
                // make sure that app name is language independent, maybe use a flavor or a static BuildConfig String
                application.getString(R.string.app_name),
                BuildConfig.VERSION_NAME,
                BuildConfig.BUILD_TYPE
            )

        if (BuildConfig.DEBUG) {
            client.addInterceptor(httpLoggingInterceptor)
        }

        return client.build()
    }

    @Provides
    @Singleton
    fun provideGithubRetrofit(
        okHttpClient: OkHttpClient,
        gson: Gson
    ): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.GITHUB_API)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        
    @Provides
    fun provideGithubApi(retrofit: Retrofit): GithubApi = retrofit.create(GithubApi::class.java)
}
```

Add the `NetworkModule` as an _include_ to your `AppModule` and you can start injecting your API class to perform network requests.

## Contribute

This project follows the [Android Kotlin Code Style](https://android.github.io/kotlin-guides/style.html)
for all Kotlin classes. We use
[ktlint](https://github.com/shyiko/ktlint) for automated checks.

The [ktlint-gradle-plugin](https://github.com/JLLeitschuh/ktlint-gradle) is already integrated into this project which means you need nearly no setup on your end.

Just execute those two calls to configure your IDE *for this project* and add a pre-commit hook to do the checking (and if necessary formatting for you).

```bash
./gradlew ktlintApplyToIdea
./gradlew addKtlintFormatGitPreCommitHook
```

Make sure to check the _Run Git hooks_ checkbox if you commit via Android Studio (should be set by default).

Last thing to do is setting line length to 140 (Preferences -> Editor -> Code Style) - unfortunately `ktlintApplyToIdea` can't do that for you.

The CI will check formatting as well and won't allow non style conform commits to be merged.
