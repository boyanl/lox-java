package com.interpreters.lox;

public class Token {
    TokenType type;
    String lexeme;
    Object literal;
    int line;

    public Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return "(type = %s, lexeme = \"%s\", value = %s, line = %s)".formatted(type.name(), lexeme, literal, line);
    }
}
