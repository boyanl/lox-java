package com.interpreters.lox;

import java.util.List;
import java.util.Objects;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private Environment env = new Environment();

    public void interpret(List<Stmt> statements) {
        try {
            for (var stmt : statements) {
                execute(stmt);
            }
        } catch (RuntimeError e) {
            Lox.runtimeError(e);
        }
    }

    public void evaluteAndPrint(Expr expression) {
        try {
            var result = eval(expression);
            System.out.println(stringify(result));
        } catch (RuntimeError e) {
            Lox.runtimeError(e);
        }
    }

    @Override
    public Object visit(Expr.Assign expr) {
        var val = eval(expr.value());
        env.assign(expr.name(), val);
        return val;
    }

    @Override
    public Object visit(Expr.Ternary expr) {
        var condition = eval(expr.condition());

        if (isTruthy(condition)) {
            return eval(expr.first());
        }
        return eval(expr.second());
    }

    @Override
    public Object visit(Expr.Binary expr) {
        var left = eval(expr.left());
        var right = eval(expr.right());

        return switch (expr.operator().type) {
            case PLUS -> {
                if (left instanceof Double l && right instanceof Double r) {
                    yield l + r;
                } else if (left instanceof String s1 && right instanceof String s2) {
                    yield s1 + s2;
                }

                throw new RuntimeError(expr.operator(), "Operands should either be numbers or strings");
            }
            case MINUS -> {
                checkNumberOperands(expr.operator(), left, right);
                yield (double) left - (double) right;
            }
            case STAR -> {
                checkNumberOperands(expr.operator(), left, right);
                yield (double) left * (double) right;
            }
            case SLASH -> {
                checkNumberOperands(expr.operator(), left, right);
                yield (double) left / (double) right;
            }
            case EQUAL_EQUAL -> isEqual(left, right);
            case BANG_EQUAL -> !isEqual(left, right);
            case GREATER -> {
                checkNumberOperands(expr.operator(), left, right);
                yield (double) left > (double) right;
            }
            case LESS -> {
                checkNumberOperands(expr.operator(), left, right);
                yield (double) left < (double) right;
            }
            case GREATER_EQUAL -> {
                checkNumberOperands(expr.operator(), left, right);
                yield (double) left >= (double) right;
            }
            case LESS_EQUAL -> {
                checkNumberOperands(expr.operator(), left, right);
                yield (double) left <= (double) right;
            }
            default -> null;
        };
    }

    @Override
    public Object visit(Expr.Grouping expr) {
        return eval(expr.expression());
    }

    @Override
    public Object visit(Expr.Literal expr) {
        return expr.value();
    }

    @Override
    public Object visit(Expr.Variable expr) {
        return env.getValue(expr.name());
    }

    @Override
    public Object visit(Expr.Unary expr) {
        var right = eval(expr.right());

        return switch (expr.operator().type) {
            case MINUS -> {
                checkNumberOperand(expr.operator(), right);
                yield -(double) right;
            }
            case BANG -> !isTruthy(right);
            default -> null;
        };
    }

    private Object eval(Expr expr) {
        return expr.accept(this);
    }

    private boolean isTruthy(Object val) {
        if (val == null) return false;
        if (val instanceof Boolean b) return b;
        return true;
    }

    private boolean isEqual(Object left, Object right) {
        return Objects.equals(left, right);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand should be number");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands should be numbers");
    }

    private String stringify(Object val) {
        if (val == null) return "nil";

        if (val instanceof Double) {
            String str = val.toString();

            if (str.endsWith(".0")) {
                return str.substring(0, str.length() - 2);
            }
            return str;
        }

        return val.toString();
    }

    @Override
    public Void visit(Stmt.Expression stmt) {
        eval(stmt.expr());
        return null;
    }

    @Override
    public Void visit(Stmt.VarDeclaration stmt) {
        Object val = null;
        if (stmt.initializer() != null) {
            val = eval(stmt.initializer());
        }

        env.define(stmt.name().lexeme, val);
        return null;
    }

    @Override
    public Void visit(Stmt.Block stmt) {
        executeBlock(stmt, new Environment(env));
        return null;
    }

    @Override
    public Void visit(Stmt.Print stmt) {
        var val = eval(stmt.expr());
        System.out.println(stringify(val));
        return null;
    }

    private void execute(Stmt statement) {
        statement.accept(this);
    }

    private void executeBlock(Stmt.Block blockStmt, Environment environment) {
        var originalEnv = this.env;
        try {
            this.env = environment;
            for (var statement : blockStmt.statements()) {
                execute(statement);
            }
        } finally {
            this.env = originalEnv;
        }
    }
}
