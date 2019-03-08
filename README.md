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
    implementation 'com.github.stanwood:framework-network-android:<insert latest version here>' // aar version available as well
}
```

## Usage

Refer to the extensive javadoc of the provided classes for details on how to
use them. Right now there are solutions for the following use cases:

- handle offline situations and caching when using OkHttp (`cache` package)
- generic token based authentication handling with OkHttp (`auth` package)
- generic network related utility classes (some specific to apps developed over here at _stanwood_)

### cache

TODO

### auth

The `auth` package contains classes for handling token based authentication
with OkHttp. Generally this is done via Authenticators and Interceptors.

Integration into both existing means of token retrieval as well as from scratch
is simple.

First you need to implement both `TokenReaderWriter` and
`AuthenticationProvider`. The first is used for reading and writing tokens
from/to requests. The second provides means to get authentication data (such as
tokens and sign-in status). Refer to the javadoc for more details on how to
implement these interfaces.

In the future optional modules will provide implementations for common use
cases.

Then create an instance of `io.stanwood.framework.network.auth.Authenticator`:

```java
Authenticator authenticator = new Authenticator(
    authenticationProvider,
    tokenReaderWriter,
    response -> Log.e("Authentication failed permanently!")
);
```

And an instance of `AuthInterceptor`:

```java
AuthInterceptor authInterceptor = new AuthInterceptor(
    appContext,
    authenticationProvider,
    tokenReaderWriter
);
```

Construct an `OkHttpClient` and pass the interceptor and the authenticator:

```java
new OkHttpClient.Builder()
        .authenticator(authenticator)
        .addInterceptor(authInterceptor)
        .build();
```

That's it. When using this `OkHttpClient` instance you'll benefit from fully
transparent token handling.

Remember that you usually will have to retrieve the initial token (after user triggered login) on your own outside of the `AuthenticationProvider`. Usually you will store the token then in e.g. the `SharedPreferences` and later try to retrieve them in your `AuthenticationProvider`s `getToken()` method - unless `forceRefresh()` is set.

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
2. one `TokenReaderWriter` per Api

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

We're looking forward to streamlining this as soon as the okhttp bug has been
resolved.

## util

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
for all Kotlin classes (exception: line length = 140, please adapt your IDE settings accordingly). We use
[ktlint](https://github.com/shyiko/ktlint) for automated checks.

Install ktlint on your machine like described in their [manual](https://github.com/shyiko/ktlint#installation).

Then configure the pre-commit hook like so:

```
ktlint --install-git-pre-commit-hook
```

Make sure to check the _Run Git hooks_ checkbox if you commit via Android Studio.
The CI will test this as well in the future and won't allow non style conform commits to be merged
into develop.
