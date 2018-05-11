[![Release](https://jitpack.io/v/stanwood/framework-network-android.svg?style=flat-square)](https://jitpack.io/#stanwood/framework-network-android)
[![Build Status](https://www.bitrise.io/app/983e6342cc5e0e24/status.svg?token=QtXUf2lbVhJrANROaTkluQ)](https://www.bitrise.io/app/983e6342cc5e0e24)
[![Maintainability](https://api.codeclimate.com/v1/badges/eeed4b740670c5753217/maintainability)](https://codeclimate.com/github/stanwood/framework-network-android/maintainability)

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

#### A note on authentication exception handling

The library provides an own exception class called `AuthenticationException`.
You can listen for this class in your Retrofit
`Callback.onFailure(Call, Throwable)` to check for authentication related
errors.

However up until now we are not able to fire this exception everywhere -
especially the `Authenticator` suffers from a likely
[okhttp bug](https://github.com/square/okhttp/issues/3872) which prevents us
from firing exceptions there. In case of errors you will receive a
`NullPointerException` instead which probably won't help you much for automated
handling as this exception is basically caused by every
interceptor/authenticator returning `null` for the expected Request/Response.

For the time being we recommend to take a best effort approach here and
additionally check for 401 response code in Retrofit's `Callback.onSuccess()`
for when an issue in the Authenticator occured (if there was no issue you won't
get a 401 propagated to `Callback.onSuccess()` as in case of successful
authentication after receiving a 401 for the initial request your callback will
get the response code of the following originally intended request).

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
