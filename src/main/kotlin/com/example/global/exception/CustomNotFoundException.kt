package com.example.global.exception

//엔티티가 존재하지않을때 발생
class CustomNotFoundException(
    val status: String,
    message: String
) : RuntimeException(message) {
    constructor(message: String) : this("404", message)
}
