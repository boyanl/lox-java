package com.interpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {

    private final Stack<Map<String, Boolean>> scopes = new Stack<>();
    private final Interpreter interpreter;
    private FunctionType currentFunction = FunctionType.NONE;
    private ClassType currentClass = ClassType.NONE;

    private enum FunctionType {
        FUNCTION, METHOD, NONE
    }

    private enum ClassType {
        CLASS, NONE
    }

    public Resolver(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    @Override
    public Void visit(Expr.Assign expr) {
        resolve(expr.value());
        resolveLocal(expr, expr.name());
        return null;
    }

    @Override
    public Void visit(Expr.Ternary expr) {
        resolve(expr.condition());
        resolve(expr.first());
        resolve(expr.second());
        return null;
    }

    @Override
    public Void visit(Expr.Binary expr) {
        resolve(expr.left());
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visit(Expr.Grouping expr) {
        resolve(expr.expression());
        return null;
    }

    @Override
    public Void visit(Expr.Literal expr) {
        return null;
    }

    @Override
    public Void visit(Expr.Variable expr) {
        var name = expr.name().lexeme;
        if (!scopes.isEmpty() && scopes.peek().get(name) == Boolean.FALSE) {
            Lox.error(expr.name(), "Variable %s is accessed in its initializer".formatted(name));
        }

        resolveLocal(expr, expr.name());
        return null;
    }

    @Override
    public Void visit(Expr.Unary expr) {
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visit(Expr.Logical expr) {
        resolve(expr.left());
        resolve(expr.right());
        return null;
    }

    @Override
    public Void visit(Expr.Call expr) {
        resolve(expr.target());
        for (var arg : expr.args()) {
            resolve(arg);
        }
        return null;
    }

    @Override
    public Void visit(Expr.Function expr) {
        resolveFunction(expr, FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visit(Expr.Get expr) {
        resolve(expr.target());
        return null;
    }

    @Override
    public Void visit(Expr.Set expr) {
        resolve(expr.target());
        resolve(expr.value());
        return null;
    }

    @Override
    public Void visit(Expr.This expr) {
        if (currentClass == ClassType.NONE) {
            Lox.error(expr.keyword(), "'this' can only be used inside a class method");
        }
        resolveLocal(expr, expr.keyword());
        return null;
    }

    @Override
    public Void visit(Stmt.Expression stmt) {
        resolve(stmt.expr());
        return null;
    }

    @Override
    public Void visit(Stmt.VarDeclaration stmt) {
        declare(stmt.name());
        resolve(stmt.initializer());
        define(stmt.name());
        return null;
    }

    @Override
    public Void visit(Stmt.Block stmt) {
        beginScope();
        resolve(stmt.statements());
        endScope();
        return null;
    }

    @Override
    public Void visit(Stmt.Print stmt) {
        resolve(stmt.expr());
        return null;
    }

    @Override
    public Void visit(Stmt.If stmt) {
        resolve(stmt.condition());

        resolve(stmt.thenClause());
        if (stmt.elseClause() != null) {
            resolve(stmt.elseClause());
        }
        return null;
    }

    @Override
    public Void visit(Stmt.While stmt) {
        resolve(stmt.condition());
        resolve(stmt.body());
        return null;
    }

    @Override
    public Void visit(Stmt.Function stmt) {
        declare(stmt.name());
        define(stmt.name());
        resolveFunction(stmt.function(), FunctionType.FUNCTION);
        return null;
    }

    @Override
    public Void visit(Stmt.Class stmt) {
        declare(stmt.name());
        define(stmt.name());

        var enclosingClass = currentClass;
        currentClass = ClassType.CLASS;
        beginScope();
        scopes.peek().put("this", true);

        for (var method : stmt.methods()) {
            resolveFunction(method.function(), FunctionType.METHOD);
        }
        endScope();
        currentClass = enclosingClass;

        return null;
    }

    @Override
    public Void visit(Stmt.Break stmt) {
        return null;
    }

    @Override
    public Void visit(Stmt.Return stmt) {
        if (currentFunction == FunctionType.NONE) {
            Lox.error(stmt.keyword(), "Can't return from top-level code");
        }
        resolve(stmt.value());
        return null;
    }

    public void resolve(List<Stmt> stmts) {
        for (var stmt : stmts)  {
            resolve(stmt);
        }
    }

    private void beginScope() {
        scopes.push(new HashMap<>());
    }

    private void endScope() {
        scopes.pop();
    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveLocal(Expr expr, Token name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            var env = scopes.get(i);
            if (env.containsKey(name.lexeme)) {
                interpreter.resolve(expr, scopes.size() - i - 1);
            }
        }
    }

    private void declare(Token name) {
        if (scopes.isEmpty()) return;

        var scope = scopes.peek();
        if (scope.containsKey(name.lexeme)) {
            Lox.error(name, "Already have a variable named '%s' in this scope".formatted(name.lexeme));
        }
        scope.put(name.lexeme, false); // not initialized yet
    }

    private void define(Token name) {
        if (scopes.isEmpty()) return;
        scopes.peek().put(name.lexeme, true);
    }

    private void resolveFunction(Expr.Function function, FunctionType type) {
        var enclosingFunction = currentFunction;
        currentFunction = type;
        beginScope();
        for (var arg : function.params()) {
            declare(arg);
            define(arg);
        }
        resolve(function.body());
        endScope();
        currentFunction = enclosingFunction;
    }
}
