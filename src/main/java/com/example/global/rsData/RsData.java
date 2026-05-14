package com.example.global.rsData;

public record RsData<T>(
        String status,
        String message,
        T data
) {

    //data 없이 사용하는 생성자
    public RsData(String status, String message) {
        this(status, message, null);
    }

    //성공 응답 (data 포함)
    public static <T> RsData<T> success(String message, T data) {
        return new RsData<>("200", message, data);
    }

    //성공 응답 (data 없음)
    public static <T> RsData<T> success(String message) {
        return new RsData<>("200", message, null);
    }

     //실패 응답 (data 없음)
    public static <T> RsData<T> fail(String message) {
        return new RsData<>("400", message, null);
    }

     //실패 응답 (data 포함)
    public static <T> RsData<T> fail(String message, T data) {
        return new RsData<>("400", message, data);
    }
}
