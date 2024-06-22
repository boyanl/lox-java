package com.interpreters.lox;

import java.util.List;

import static com.interpreters.lox.TokenType.*;

public class Parser {

    private static final class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }


    public Expr parse() {
        try {
            return expression();
        } catch (ParseError error) {
            return null;
        }
    }

    private Expr expression() {
        return equality();
    }

    private Expr equality() {
        var expr = comparison();

        while (match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token op = previous();
            var right = comparison();

            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr comparison() {
        var expr = term();

        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token op = previous();
            var right = term();

            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr term() {
        var expr = factor();

        while (match(PLUS, MINUS)) {
            Token op = previous();
            var right = factor();

            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr factor() {
        var expr = unary();

        while (match(SLASH, STAR)) {
            Token op = previous();
            var right = unary();

            expr = new Expr.Binary(expr, op, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(SLASH, MINUS)) {
            var op = previous();
            var right = unary();
            return new Expr.Unary(op, right);
        }

        return primary();
    }

    private Expr primary() {
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER, STRING)) return new Expr.Literal(previous().literal);

        if (match(LEFT_PAREN)) {
            var expr = expression();
            consume(RIGHT_PAREN, "Expected ')' after expression");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expression expected");
    }

    private Token consume(TokenType type, String errorMsg) {
        if (check(type)) return advance();

        throw error(peek(), errorMsg);
    }


    private boolean match(TokenType... types) {
        for (var type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }

        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return current >= tokens.size() - 1;
//        return peek().type == TokenType.EOF;
    }

    private ParseError error(Token token, String errorMsg) {
        Lox.error(token, errorMsg);

        return new ParseError();
    }
}
