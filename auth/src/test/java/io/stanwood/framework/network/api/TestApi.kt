package io.stanwood.framework.network.api

import retrofit2.Call
import retrofit2.http.GET

interface TestApi {

    @GET("/user")
    fun getUser(): Call<User>
}