package com.example.domain.resource_provider

interface ResourceProvider {
    fun getMessage(messageKey: String): String

    fun getMessage(messageKey: Int): String
}