package com.interpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {

    private String name;
    private final Expr.Function function;
    private final Environment environment;

    public LoxFunction(String name, Expr.Function function, Environment environment) {
        this.function = function;
        this.environment = environment;
    }

    @Override
    public int arity() {
        return function.params().size();
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        var env = new Environment(environment);

        for (int i = 0; i < args.size(); i++) {
            var name = function.params().get(i).lexeme;
            env.define(name, args.get(i));
        }

        try {
            interpreter.executeBlock(function.body(), env);
        } catch (Return ret) {
            return ret.getValue();
        }

        return null;
    }

    @Override
    public String toString() {
        if (name != null) {
            return "<fn %s>".formatted(name);
        }
        return "<lambda fn>";
    }
}
