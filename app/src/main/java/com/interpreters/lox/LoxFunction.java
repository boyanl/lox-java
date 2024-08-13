package com.interpreters.lox;

import java.util.List;

public class LoxFunction implements LoxCallable {

    private final String name;
    private final Expr.Function function;
    private final Environment environment;
    private final boolean isInitializer;

    public LoxFunction(String name, Expr.Function function, Environment environment, boolean isInitializer) {
        this.name = name;
        this.function = function;
        this.environment = environment;
        this.isInitializer = isInitializer;
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
            if (isInitializer) {
                return environment.getAt(0, "this");
            }

            return ret.getValue();
        }

        if (isInitializer) {
            return environment.getAt(0, "this");
        }

        return null;
    }

    public LoxFunction bind(LoxInstance thisInstance) {
        var env = new Environment(environment);
        env.define("this", thisInstance);
        return new LoxFunction(name, function, env, isInitializer);
    }

    @Override
    public String toString() {
        if (name != null) {
            return "<fn %s>".formatted(name);
        }
        return "<lambda fn>";
    }
}
