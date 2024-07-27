package com.interpreters.lox;

public class Return extends RuntimeException {
    private Object value;

    public Return(Object value) {
        super(null, null, false, false);
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
