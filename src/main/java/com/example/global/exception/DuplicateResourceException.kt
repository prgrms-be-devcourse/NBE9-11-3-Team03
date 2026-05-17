package com.example.global.exception;

import lombok.Getter;

//중복된것 사용
@Getter
public class DuplicateResourceException extends RuntimeException{
    String statusCode;
    String message;
    public DuplicateResourceException(String statusCode,String message) {
        super(message);
        this.statusCode=statusCode;
        this.message=message;
    }

    public String getStatusCode() {
        return statusCode;
    }
}
