package com.interpreters.lox;

import java.util.Objects;

public class Interpreter implements Expr.Visitor<Object>{

    void interpret(Expr expr) {
        try {
            var value = eval(expr);
            System.out.println(stringify(value));
        } catch (RuntimeError err) {
            Lox.runtimeError(err);
        }
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
}
