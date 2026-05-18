package com.example.global.rsData

data class RsData<T>(
    val status: String,
    val message: String?,
    val data: T?
) {
    //data 없이 사용하는 생성자
    constructor(status: String, message: String?) : this(status, message, null)

    companion object {
        //성공 응답 (data 포함)
        @JvmStatic
        fun <T> success(message: String?, data: T?): RsData<T> =
            RsData("200", message, data)


        //성공 응답 (data 없음)
        @JvmStatic
        fun <T> success(message: String?): RsData<T> =
            RsData("200", message, null)


        //실패 응답 (data 없음)
        @JvmStatic
        fun <T> fail(message: String?): RsData<T> =
            RsData("400", message, null)


        //실패 응답 (data 포함)
        @JvmStatic
        fun <T> fail(message: String?, data: T?): RsData<T> =
            RsData("400", message, data)

    }
}
