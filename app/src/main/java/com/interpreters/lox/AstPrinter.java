package com.interpreters.lox;

public class AstPrinter implements Expr.Visitor<String>{

    @Override
    public String visit(Expr.Binary expr) {
        return parenthesize(expr.operator().lexeme, expr.left(), expr.right());
    }

    @Override
    public String visit(Expr.Grouping expr) {
        return parenthesize("group", expr.expression());
    }

    @Override
    public String visit(Expr.Literal expr) {
        if (expr.value() == null) return "nil";

        return expr.value().toString();
    }

    @Override
    public String visit(Expr.Unary expr) {
        return parenthesize(expr.operator().lexeme, expr.right());
    }

    @Override
    public String visit(Expr.Ternary expr) {
        return parenthesize("?",  expr.condition(), expr.first(), expr.second());
    }

    private String parenthesize(String op, Expr... expressions) {
        var sb = new StringBuilder();
        sb.append("(").append(op);

        for (var expr : expressions) {
            sb.append(" ").append(expr.accept(this));
        }

        sb.append(")");

        return sb.toString();
    }

    public String visit(Expr expr) {
        return expr.accept(this);
    }

    public static void main(String[] args) {
        var expr = new Expr.Binary(new Expr.Unary(new Token(TokenType.MINUS, "-", null, 1), new Expr.Literal(123)),
                new Token(TokenType.STAR,"*", null, 5),
                new Expr.Grouping(new Expr.Literal(45.123))
                );

        var expr2 = new Expr.Binary(
                new Expr.Binary(new Expr.Literal(1) , new Token(TokenType.PLUS, "+", null, 1), new Expr.Literal(2)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Binary(new Expr.Literal(4), new Token(TokenType.MINUS,"-", null, 1), new Expr.Literal(3))
                );

        System.out.println(expr2.accept(new AstPrinter()));
    }
}
