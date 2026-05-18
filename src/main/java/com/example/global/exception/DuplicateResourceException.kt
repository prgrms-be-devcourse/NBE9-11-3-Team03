package com.example.global.exception

//중복된것 사용
class DuplicateResourceException(
    val statusCode: String,
    message: String
) :  RuntimeException(message)
