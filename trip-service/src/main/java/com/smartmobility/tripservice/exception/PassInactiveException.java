package com.smartmobility.tripservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PassInactiveException extends RuntimeException {

    public PassInactiveException(String message) {
        super(message);
    }
}
