package com.interpreters.lox;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Lox {
    private static boolean hadError;

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [file]");
            System.exit(65);
        } else if (args.length == 1) {
            runFile(args[0]);
            if (hadError) System.exit(64);
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

        System.out.println(new AstPrinter().visit(expression));
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

    public static void report(int line, String where, String error) {
        System.out.printf("[line=%d] Error%s: %s%n", line, where, error);
        hadError = true;
    }

}