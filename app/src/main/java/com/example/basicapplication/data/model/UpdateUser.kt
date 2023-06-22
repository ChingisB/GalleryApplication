package com.example.basicapplication.data.model


import com.google.gson.annotations.SerializedName

data class UpdateUser(
    @SerializedName("email")
    val email: String,
    @SerializedName("username")
    val username: String,
    @SerializedName("birthday")
    val birthday: String
)