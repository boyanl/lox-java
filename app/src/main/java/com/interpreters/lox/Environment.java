package com.interpreters.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> variableMappings = new HashMap<>();
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

    public Object getAt(int depth, Token name) {
        return ancestor(depth).getValue(name);
    }

    public Object getAt(int depth, String name) {
        return ancestor(depth).variableMappings.get(name);
    }

    public void assignAt(int depth, Token token, Object value) {
        ancestor(depth).assign(token, value);
    }

    public Environment getParent() {
        return parent;
    }

    private Environment ancestor(int distance) {
        var environment = this;
        for (int i = 0; i < distance; i++) {
            environment = environment.parent;
        }
        return environment;
    }

    @Override
    public String toString() {
        return "Environment[vars=%s, parent=%s]".formatted(variableMappings, parent);
    }
}
