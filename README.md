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
- generic token based authentication handling (both anonymous and authenticated) with OkHttp (`auth` package)

### cache

TODO

### auth

The `auth` package contains classes for handling token based authentication with OkHttp. Generally this is done via Authenticators and Interceptors.

Integration into both existing means of token retrieval as well as from scratch is simple.

#### Authenticated

First you need to implement both `TokenReaderWriter` and `AuthenticationProvider`.
The first is used for reading and writing tokens from/to requests.
The second provides means to get authentication data (such as tokens and sign-in status).
Refer to the javadoc for more details on how to implement these interfaces.
In the future optional modules will provide implementations for common use cases.

Then create an instance of `AuthenticatedAuthenticator`:
```java
AuthenticatedAuthenticator authenticatedAuthenticator = new AuthenticatedAuthenticator(
    authenticationProvider,
    tokenReaderWriter,
    request -> Log.e("Authentication failed permanently!"
);
```

And an instance of `AuthenticatedAuthInterceptor`:
```java
AuthenticatedAuthInterceptor authenticatedAuthInterceptor = new AuthenticatedAuthInterceptor(
    appContext,
    authenticationProvider,
    tokenReaderWriter
);
```

Construct an `OkHttpClient` and pass the interceptor and the authenticator:
```java
new OkHttpClient.Builder()
        .authenticator(authenticatedAuthenticator)
        .addInterceptor(authenticatedAuthInterceptor)
        .build();
```

That's it. When using this `OkHttpClient` instance you'll benefit from fully transparent token handling.

### Anonymous

The implementation for anonymous authentication is very similar to the authenticated one.
We'll just point out the differences here.

Instead of the `Authenticated*` classes use the `Anonymous*` ones for authenticators and interceptors.

The constructors are nearly the same, but the `AnonymousAuthenticator` also accepts an optional instance of `AuthenticatedAuthenticator`.
You can pass an instance here and the `AnonymousAuthenticator` will attempt to use the authenticated token if the user is signed in (determined via `AuthenticationProvider.isUserSignedIn()`).
You can pass `null` here if you don't have means for authenticated authentication or your API doesn't support accessing certain endpoints with authenticated tokens.

### Tips and tricks

If your API provides both anonymous and authenticated access you will want to use one `OkHttpClient` instances per case and pass the according authenticators and interceptors during creation.
