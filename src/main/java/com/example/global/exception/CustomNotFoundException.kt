package com.example.global.exception;

import lombok.Getter;

//엔티티가 존재하지않을때 발생
@Getter
public class CustomNotFoundException extends RuntimeException{
    String status;
    String message;

    public CustomNotFoundException(String status, String message) {
        super(message);
        this.message=message;
        this.status=status;
    }
    public CustomNotFoundException(String message){
        super(message);
        this.status="404";
        this.message=message;
    }

    public String getStatus() {
        return status;
    }

}
