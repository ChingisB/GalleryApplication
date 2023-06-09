package com.example.data.api.service

import com.example.data.api.model.UpdatePassword
import com.example.data.api.model.UpdateUser
import com.example.data.api.model.User
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserService {
    @GET("api/users/current")
    fun getCurrentUser(): Single<User>

    @PUT("api/users/update_password/{id}")
    fun updatePassword(@Path("id") id: Int, @Body updatePassword: UpdatePassword): Single<User>

    @PUT("api/users/{id}")
    fun updateUser(@Path("id") id: Int, @Body updateUser: UpdateUser): Single<User>

    @DELETE("api/users/{id}")
    fun deleteUser(@Path("id") id: Int): Completable

    @GET("api/users/{id}")
    fun getUser(@Path("id") id: Int): Single<User>

}