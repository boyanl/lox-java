package com.interpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private Map<String, Object> variableMappings = new HashMap<>();
    private Environment parent;

    public Environment() {
    }

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public void define(String name, Object value) {
        variableMappings.put(name, value);
    }

    public void assign(Token token, Object value) {
        var name = token.lexeme;
        if (variableMappings.containsKey(name)) {
            variableMappings.put(name, value);
            return;
        }

        if (parent != null) {
            parent.assign(token, value);
        } else {
            throw new RuntimeError(token, String.format("Undefined variable %s", name));
        }
    }

    public Object getValue(Token name) {

        Object val = variableMappings.get(name.lexeme);
        if (val == null) {
            if (parent != null) {
                return parent.getValue(name);
            }
            throw new RuntimeError(name, String.format("Undefined variable %s", name.lexeme));
        }

        return val;
    }
}
