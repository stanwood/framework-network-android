[![Release](https://jitpack.io/v/stanwood/Network_android.svg?style=flat-square)](https://jitpack.io/#stanwood/Network_android)

# stanwood Network Utilities (Android)

A set of hopefully useful classes for common networking use cases.

## Import

The stanwood Network Utilities are hosted on JitPack. Therefore you can simply import them by adding

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
    implementation 'com.github.stanwood:Network_android:<insert latest version here>' // aar version available as well
}
```

## Usage

Refer to the extensive javadoc of the provided classes for details on how to use them. Right now there are solutions for the following use cases:

- handle offline situations and caching when using OkHttp (`cache` package)
- generic token based authentication handling with OkHttp (`auth` package)

### cache

TODO

### auth

The `auth` package contains classes for handling token based authentication with OkHttp. Generally this is done via Authenticators and Interceptors.

Integration into both existing means of token retrieval as well as from scratch is simple.

First you need to implement both `TokenReaderWriter` and `AuthenticationProvider`.
The first is used for reading and writing tokens from/to requests.
The second provides means to get authentication data (such as tokens and sign-in status).
Refer to the javadoc for more details on how to implement these interfaces.
In the future optional modules will provide implementations for common use cases.

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

That's it. When using this `OkHttpClient` instance you'll benefit from fully transparent token handling.

*If your app uses multiple authentication methods make sure to implement an own subclass of `AuthenticationProvider` for each method and use an own `OkHttpClient` for each!*
