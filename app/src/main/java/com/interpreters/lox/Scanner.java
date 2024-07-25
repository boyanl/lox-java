package com.interpreters.lox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.interpreters.lox.TokenType.*;
import static java.util.Map.entry;

public class Scanner {

    private final String source;
    private int start, current, line;
    private List<Token> tokens;

    private Map<String, TokenType> RESERVED_WORDS = reservedWordsMap();

    Scanner(String source) {
        this.source = source;
        this.start = 0;
        this.current = 0;
        this.line = 1;
    }

    public List<Token> scan() {
        tokens = new ArrayList<>();
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    // identifiers, reserved words


    private void scanToken() {
        char c = advance();

        switch (c) {
            case '{' -> addToken(LEFT_BRACE);
            case '}' -> addToken(RIGHT_BRACE);
            case '(' -> addToken(LEFT_PAREN);
            case ')' -> addToken(RIGHT_PAREN);
            case '+' -> addToken(PLUS);
            case '-' -> addToken(MINUS);
            case '*' -> addToken(STAR);
            case '/' -> {
                if (peek() == '/') {
                    while (peek() != '\n') {
                        advance();
                    }
                } else {
                    addToken(SLASH);
                }
            }
            case ';' -> addToken(SEMICOLON);
            case '.' -> addToken(DOT);
            case '?' -> addToken(QUESTION_MARK);
            case ':' -> addToken(COLON);
            case ',' -> addToken(COMMA);
            case '!' -> addToken(match('=') ? BANG_EQUAL : BANG);
            case '>' -> addToken(match('=') ? GREATER_EQUAL : GREATER);
            case '<' -> addToken(match('=') ? LESS_EQUAL : LESS);
            case '=' -> addToken(match('=') ? EQUAL_EQUAL : EQUAL);
            case ' ', '\r', '\t' -> {}
            case '"' -> string();
            case '\n' -> line++;
            default -> {
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                }
                else {
                    Lox.error(line, "Unexpected character: " + c);
                }
            }
        }
    }

    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') {
                line++;
            }
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string: ");
            return;
        }


        assert source.charAt(current) == '"';
        advance();

        var val = source.substring(start + 1, current - 1);
        addToken(STRING, val);
    }

    private void number() {
        while (isDigit(peek())) advance();

        if (peek() == '.' && isDigit(peekNext())) {
            advance();

            while (isDigit(peek())) {
                advance();
            }
        }

        var value = Double.parseDouble(source.substring(start, current));
        addToken(NUMBER, value);
    }

    private void identifier() {

        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);
        var tokenType = Optional.ofNullable(RESERVED_WORDS.get(text)).orElse(IDENTIFIER);
        addToken(tokenType);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        var text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) return '\0';
        return source.charAt(current + 1);
    }

    private boolean match(char c) {
        if (isAtEnd()) return false;
        if (source.charAt(current) != c) return false;

        current++;
        return true;
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'A' && c <= 'Z')
                || (c >= 'a' && c <= 'z')
                || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }


    private static Map<String, TokenType> reservedWordsMap() {
        return Map.ofEntries(entry("and", AND),
                entry("or", OR),
                entry("class", CLASS),
                entry("else", ELSE),
                entry("true", TRUE),
                entry("false", FALSE),
                entry("for", FOR),
                entry("fun", FUN),
                entry("if", IF),
                entry("nil", NIL),
                entry("print", PRINT),
                entry("return", RETURN),
                entry("super", SUPER),
                entry("this", THIS),
                entry("var", VAR),
                entry("while", WHILE),
                entry("break", BREAK)
        );
    }
}
