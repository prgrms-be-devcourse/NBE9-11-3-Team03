package com.example.global.response

class ApiRes<T>(
    val status: Int,
    val message: String,
    val data: T?
)
