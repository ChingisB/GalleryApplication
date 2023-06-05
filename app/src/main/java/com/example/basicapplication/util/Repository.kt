package com.example.basicapplication.util

import com.example.basicapplication.model.baseModel.BaseCreateEntity
import com.example.basicapplication.model.baseModel.BaseEntity
import io.reactivex.Completable
import io.reactivex.Single

interface Repository<T: BaseEntity, K, R: BaseCreateEntity> {

    fun getByID(id: K): Single<T>

    fun create(item: R): Single<T>

    fun delete(id: K): Completable
}