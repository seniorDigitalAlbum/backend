package com.chimaenono.dearmind.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class KakaoPermissionException extends RuntimeException {
    public KakaoPermissionException(String message) {
        super(message);
    }
}