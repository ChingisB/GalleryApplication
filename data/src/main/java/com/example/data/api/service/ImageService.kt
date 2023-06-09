package com.example.data.api.service

import com.example.data.api.model.Image
import io.reactivex.Single
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImageService {
    @Multipart
    @POST("api/media_objects")
    fun createImage(@Part file: MultipartBody.Part): Single<Image>
}