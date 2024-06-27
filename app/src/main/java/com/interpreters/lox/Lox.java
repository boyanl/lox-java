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
            run(line);
            hadError = false;
            hadRuntimeError = false;
        }).run();
    }

    private static void run(String source) {

        var scanner = new Scanner(source);
        var tokens = scanner.scan();

        var parser = new Parser(tokens);
        Expr expression = parser.parse();

        if (hadError) {
            return;
        }

        interpreter.interpret(expression);
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
        System.out.printf("[line=%d] Error%s: %s%n", line, where, error);
        hadError = true;
    }

}