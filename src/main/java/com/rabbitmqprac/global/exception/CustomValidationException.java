package com.rabbitmqprac.global.exception;

import org.springframework.validation.BindingResult;

public class CustomValidationException extends RuntimeException {
    private final BindingResult bindingResult;

    public CustomValidationException(BindingResult bindingResult) {
        this.bindingResult = bindingResult;
    }

    public BindingResult getBindingResult() {
        return bindingResult;
    }
}