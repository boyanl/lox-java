package com.interpreters.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable{
    private final String name;
    private final Map<String, LoxFunction> methods;

    public LoxClass(String name, Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int arity() {
        var initializer = findMethod("init");
        return initializer != null ? initializer.arity() : 0;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        var instance = new LoxInstance(this);
        var initializer = findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, args);
        }

        return instance;
    }

    public LoxFunction findMethod(String name) {
        return methods.get(name);
    }
}
