package com.interpreters.lox;

public class RuntimeError extends RuntimeException {
    private Token token;
    private String message;

    public RuntimeError(Token token, String message) {
        this.token = token;
        this.message = message;
    }

    public Token getToken() {
        return token;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
