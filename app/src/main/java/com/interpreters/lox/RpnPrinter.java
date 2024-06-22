package com.interpreters.lox;

public class RpnPrinter implements Expr.Visitor<String> {
    @Override
    public String visit(Expr.Binary expr) {
        return "%s %s %s".formatted(eval(expr.left()), eval(expr.right()), expr.operator().lexeme);
    }

    @Override
    public String visit(Expr.Grouping expr) {
        return "(%s)".formatted(eval(expr.expression()));
    }

    @Override
    public String visit(Expr.Literal expr) {
        if (expr.value() == null) return "nil";
        return expr.value().toString();
    }

    @Override
    public String visit(Expr.Unary expr) {
        return "%s %s".formatted(eval(expr.right()), expr.operator().lexeme);
    }

    private String eval(Expr e) {
        return e.accept(this);
    }

    public static void main(String... args) {

        var expr2 = new Expr.Binary(
                new Expr.Binary(new Expr.Literal(1) , new Token(TokenType.PLUS, "+", null, 1), new Expr.Literal(2)),
                new Token(TokenType.STAR, "*", null, 1),
                new Expr.Binary(new Expr.Literal(4), new Token(TokenType.MINUS,"-", null, 1), new Expr.Literal(3))
        );

        System.out.println(expr2.accept(new RpnPrinter()));
    }
}
