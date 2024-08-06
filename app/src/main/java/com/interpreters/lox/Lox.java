package com.interpreters.lox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Lox {
    private static boolean hadError;
    private static boolean hadRuntimeError;

    private static Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [file]");
            System.exit(65);
        } else if (args.length == 1) {
            runFile(args[0]);
            if (hadError) System.exit(64);
            if (hadRuntimeError) System.exit(70);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String filename) throws IOException {
        var bytes = Files.readAllBytes(Paths.get(filename));
        run(new String(bytes, StandardCharsets.UTF_8));
    }


    private static void runPrompt() throws IOException {
        new Terminal(line -> {
            runRepl(line);
            hadError = false;
            hadRuntimeError = false;
        }).run();
    }

    private static void run(String source) {
        var scanner = new Scanner(source);
        var tokens = scanner.scan();

        var parser = new Parser(tokens);
        var statements = parser.parse();

        if (hadError) {
            return;
        }

        var resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        if (hadError) {
            return;
        }

        interpreter.interpret(statements);
    }

    private static void runRepl(String source) {
        var scanner = new Scanner(source);
        var tokens = scanner.scan();

        var parser = new Parser(tokens);

        var endsInSemicolon = tokens.size() >= 2 && tokens.get(tokens.size() - 2).type == TokenType.SEMICOLON;
        if (!endsInSemicolon) {
            // insert semicolon before EOF and parse
            assert tokens.stream().anyMatch(t -> t.type == TokenType.EOF);
            if (tokens.size() == 1) {
                return;
            }

            tokens.add(tokens.size() - 1, new Token(TokenType.SEMICOLON, ";", null, 1));
            var statements = parser.parse();

            if (hadError) {
                return;
            }

            assert statements.size() == 1;
            var stmt = statements.get(0);

            if (stmt instanceof Stmt.Expression) {
                interpreter.evaluteAndPrint(((Stmt.Expression) stmt).expr());
            } else {
                interpreter.interpret(statements);
            }
        } else {
            var statements = parser.parse();

            if (hadError) {
                return;
            }

            interpreter.interpret(statements);
        }
    }


    public static void error(int line, String error) {
        report(line, "", error);
    }

    public static void error(Token token, String error) {
        if (token.type == TokenType.EOF) {
            report(token.line, "at end", error);
        } else {
            report(token.line, " at '%s'".formatted(token.lexeme), error);
        }
    }

    public static void runtimeError(RuntimeError error) {
        System.err.printf("%s\n[line = %d]%n", error.getMessage(), error.getToken().line);
        hadRuntimeError = true;
    }

    public static void report(int line, String where, String error) {
        System.out.printf("[line=%d] Error %s: %s%n", line, where, error);
        hadError = true;
    }

}