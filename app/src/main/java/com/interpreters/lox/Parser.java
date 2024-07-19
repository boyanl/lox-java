package com.interpreters.lox;

import java.util.ArrayList;
import java.util.List;

import static com.interpreters.lox.TokenType.*;

public class Parser {

    private static final class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }


    public List<Stmt> parse() {
        var result = new ArrayList<Stmt>();
        while (!isAtEnd()) {
            var stmt = declaration();
            result.add(stmt);
        }

        return result;
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                return;
            }

            switch (peek().type) {
                case IF, PRINT, CLASS, FOR, RETURN, WHILE, FUN, VAR: return;
            }

            advance();
        }
    }

    private Stmt declaration() {
        try {
            if (match(VAR)) return varDeclaration();
            return statement();
        } catch (ParseError e) {
            synchronize();
            return null;
        }
    }

    private Stmt varDeclaration() {
        var name = advance();
        Expr value = null;

        if (match(EQUAL)) {
            value = expression();
        }

        consume(SEMICOLON, "; expected after variable declaration");
        return new Stmt.VarDeclaration(name, value);
    }

    private Stmt statement() {
        if (match(PRINT)) {
            return printStatement();
        }
        if (match(LEFT_BRACE)) {
            return blockStatement();
        }
        if (match(IF)) {
            return ifStatement();
        }
        if (match(WHILE)) {
            return whileStatement();
        }
        if (match(FOR)) {
            return forStatement();
        }

        return expressionStatement();
    }

    private Stmt blockStatement() {
        var statements = new ArrayList<Stmt>();
        while(!check(RIGHT_BRACE) && !isAtEnd()) {
            var stmt = declaration();
            statements.add(stmt);
        }

        consume(RIGHT_BRACE, "'}' expected after block");

        return new Stmt.Block(statements);
    }

    private Stmt printStatement() {
        var expr = expression();
        consume(SEMICOLON, "; expected after statement");

        return new Stmt.Print(expr);
    }

    private Stmt ifStatement() {
        consume(LEFT_PAREN, "'(' expected after if");
        var condition = expression();
        consume(RIGHT_PAREN, "')' expected");
        var thenStmt = statement();
        Stmt elseStmt = null;


        if (match(ELSE)) {
            elseStmt = statement();
        }

        return new Stmt.If(condition, thenStmt, elseStmt);
    }

    private Stmt whileStatement() {
        consume(LEFT_PAREN, "'(' expected after 'while'");
        var condition = expression();
        consume(RIGHT_PAREN, "')' expected after condition");
        var body = statement();

        return new Stmt.While(condition, body);
    }

    private Stmt forStatement() {
        consume(LEFT_PAREN, "'(' expected after 'for'");
        Stmt initializer;
        if (match(SEMICOLON)) {
            initializer = null;
        } else if (match(VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON, "Expected ';' after 'for' condition");

        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume(RIGHT_PAREN, "Expected ')' after the clauses");

        var body = statement();

        if (increment != null) {
            body = new Stmt.Block(List.of(body, new Stmt.Expression(increment)));
        }

        if (condition == null) {
            condition = new Expr.Literal(true);
        }
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(List.of(initializer, body));
        }

        return body;
    }

    private Stmt expressionStatement() {
        var expr = expression();
        consume(SEMICOLON, "; expected after statement");

        return new Stmt.Expression(expr);
    }

    private Expr expression() {
        return assignment();
    }

    private Expr assignment() {
        Expr expr = ternary();

        if (match(EQUAL)) {
            var equals = previous();
            var value = assignment();

            if (expr instanceof Expr.Variable var) {
                return new Expr.Assign(var.name(), value);
            }

            error(equals, "Invalid assignment target");
        }

        return expr;
    }


    private Expr ternary() {
        var expr = or();

        if (match(QUESTION_MARK)) {
            var first = ternary();
            consume(COLON, "':' expected");
            var second = ternary();
            return new Expr.Ternary(expr, first, second);
        }

        return expr;
    }

    private Expr or() {
        var expr = and();

        while (match(OR)) {
            var token = previous();
            var right = or();
            expr = new Expr.Logical(expr, token, right);
        }
        return expr;
    }

    private Expr and() {
        var expr = equality();

        while (match(AND)) {
            var token = previous();
            var right = and();
            expr = new Expr.Logical(expr, token, right);
        }
        return expr;
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

        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
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
