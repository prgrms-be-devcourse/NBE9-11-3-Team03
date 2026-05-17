package com.example.global.response;

import lombok.Getter;

@Getter
public class ApiRes<T> {

    private final int status;
    private final String message;
    private final T data;

    public ApiRes(int status, String message, T data){
        this.status = status;
        this.message = message;
        this.data = data;
    }



}
