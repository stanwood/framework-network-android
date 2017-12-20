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