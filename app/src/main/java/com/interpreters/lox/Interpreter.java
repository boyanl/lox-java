package com.interpreters.lox;

import java.util.*;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    private final Environment globals = new Environment();
    private Environment env = globals;
    private final Map<Expr, Integer> resolutions = new HashMap<>();

    public Interpreter() {
        this.globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }

            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                return System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() {
                return "<native fn clock()>";
            }
        });
    }

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

        var distance = resolutions.get(expr);
        if (distance != null) {
            env.assignAt(distance, expr.name(), val);
        } else {
            globals.assign(expr.name(), val);
        }

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
                } else if (left instanceof String s && right instanceof Double d) { // TODO: For debugging purposes, for now (to be able to do `print "X is: " + x`)
                    yield s + stringify(d);
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
        return lookUpVariable(expr, expr.name());
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

    @Override
    public Object visit(Expr.Logical expr) {
        var leftVal = eval(expr.left());
        if ((expr.operator().type == TokenType.OR && isTruthy(leftVal)) ||
                (expr.operator().type == TokenType.AND && !isTruthy(leftVal))) {
            return leftVal;
        }

        return eval(expr.right());
    }

    @Override
    public Object visit(Expr.Call expr) {
        var target = eval(expr.target());
        var args = new ArrayList<>();

        for (var arg : expr.args()) {
            args.add(eval(arg));
        }

        if (!(target instanceof LoxCallable callable)) {
            throw new RuntimeError(expr.paren(), "Can only call functions and classes");
        }

        if (callable.arity() != args.size()) {
            throw new RuntimeError(expr.paren(), String.format("Wrong number of arguments: %d, required: %d", args.size(), callable.arity()));
        }

        return callable.call(this, args);
    }

    @Override
    public Object visit(Expr.Function expr) {
        return new LoxFunction(null, expr, env, false);
    }

    @Override
    public Object visit(Expr.Get expr) {
        var object = eval(expr.target());
        if (!(object instanceof LoxInstance inst)) {
            throw new RuntimeError(expr.name(), "Only instances have properties");
        }

        return inst.get(expr.name());
    }

    @Override
    public Object visit(Expr.Set expr) {
        var object = eval(expr.target());
        if (!(object instanceof LoxInstance inst)) {
            throw new RuntimeError(expr.name(), "Only instances have properties");
        }

        var value = eval(expr.value());
        inst.set(expr.name(), value);
        return null;
    }

    @Override
    public Object visit(Expr.This expr) {
        return lookUpVariable(expr, expr.keyword());
    }

    @Override
    public Object visit(Expr.Super expr) {
        var depth = resolutions.get(expr);
        var superclass = (LoxClass) env.getAt(depth, "super");
        var method = superclass.findMethod(expr.method().lexeme);

        if (method == null) {
            throw new RuntimeError(expr.method(), "Can't find super method '%s'".formatted(expr.method().lexeme));
        }
        var object = (LoxInstance) env.getAt(depth - 1, "this");

        return method.bind(object);
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

    @Override
    public Void visit(Stmt.If stmt) {
        if (isTruthy(eval(stmt.condition()))) {
            execute(stmt.thenClause());
        } else if (stmt.elseClause() != null){
            execute(stmt.elseClause());
        }
        return null;
    }

    @Override
    public Void visit(Stmt.While stmt) {
        try {
            while(isTruthy(eval(stmt.condition()))) {
                execute(stmt.body());
            }
        } catch (Break ignored) {}
        return null;
    }

    @Override
    public Void visit(Stmt.Function stmt) {
        globals.define(stmt.name().lexeme, new LoxFunction(stmt.name().lexeme, stmt.function(), env, false));
        return null;
    }

    @Override
    public Void visit(Stmt.Class stmt) {

        LoxClass superclass = null;
        if (stmt.superclass() != null) {
            Object superVal = eval(stmt.superclass());
            if (!(superVal instanceof LoxClass)) {
                throw new RuntimeError(stmt.superclass().name(), "Must have a class as a superclass");
            }

            superclass = (LoxClass) superVal;
        }
        env.define(stmt.name().lexeme, null);

        if (stmt.superclass() != null) {
            env = new Environment(env);
            env.define("super", superclass);
        }

        var functions = new HashMap<String, LoxFunction>();
        for (var method : stmt.methods()) {
            var fn = new LoxFunction(method.name().lexeme, method.function(), env, method.name().lexeme.equals("init"));
            functions.put(method.name().lexeme, fn);
        }

        if (stmt.superclass() != null) {
            env = env.getParent();
        }

        var klass = new LoxClass(stmt.name().lexeme, superclass, functions);
        env.define(stmt.name().lexeme, klass);
        return null;
    }

    @Override
    public Void visit(Stmt.Break stmt) {
        throw new Break();
    }

    @Override
    public Void visit(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value() != null) {
            value = eval(stmt.value());
        }

        throw new Return(value);
    }

    public void resolve(Expr expr, int level) {
        resolutions.put(expr, level);
    }


    private void execute(Stmt statement) {
        statement.accept(this);
    }

    public void executeBlock(Stmt.Block blockStmt, Environment environment) {
        executeBlock(blockStmt.statements(), environment);
    }

    public void executeBlock(List<Stmt> statements, Environment environment) {
        var originalEnv = this.env;
        try {
            this.env = environment;
            for (var statement : statements) {
                execute(statement);
            }
        } finally {
            this.env = originalEnv;
        }
    }

    private Object lookUpVariable(Expr variable, Token name) {
        var depth = resolutions.get(variable);
        if (depth != null) {
            return env.getAt(depth, name);
        }
        return globals.getValue(name);
    }
}
