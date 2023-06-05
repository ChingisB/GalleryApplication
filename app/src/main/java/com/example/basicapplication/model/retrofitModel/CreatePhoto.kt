package com.example.basicapplication.model.retrofitModel


import com.example.basicapplication.model.baseModel.CreatePhotoInterface
import com.google.gson.annotations.SerializedName

data class CreatePhoto(
    @SerializedName("dateCreate")
    override val dateCreate: String,
    @SerializedName("description")
    override val description: String,
    @SerializedName("image")
    override val image: Image,
    @SerializedName("name")
    override val name: String,
    @SerializedName("new")
    override val new: Boolean,
    @SerializedName("popular")
    override val popular: Boolean
): CreatePhotoInterface {
}