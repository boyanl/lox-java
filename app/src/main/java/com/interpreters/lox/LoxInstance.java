package com.interpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class LoxInstance {
    private final LoxClass klass;
    private final Map<String, Object> properties = new HashMap<>();

    public LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return STR."\{klass.toString()} instance";
    }

    public Object get(Token name) {
        if (properties.containsKey(name.lexeme)) {
            return properties.get(name.lexeme);
        }

        var method = klass.findMethod(name.lexeme);
        if (method != null) return method.bind(this);

        throw new RuntimeError(name, "Unknown property '%s'".formatted(name.lexeme));
    }

    public void set(Token name, Object value) {
        properties.put(name.lexeme, value);
    }
}
