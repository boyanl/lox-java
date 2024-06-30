package com.interpreters.lox.generator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static java.lang.StringTemplate.STR;

public class GenerateAst {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.println("Usage: Generator <output-dir>");
            System.exit(1);
        }

        var outputPath = Paths.get(args[0]);
        if (!outputPath.toFile().exists()) {
            outputPath.toFile().mkdirs();
        }

        defineAst(outputPath, "Expr", """
Assign      : Token name, Expr value
Ternary     : Expr condition, Expr first, Expr second
Binary      : Expr left, Token operator, Expr right
Grouping    : Expr expression
Literal     : Object value
Variable    : Token name
Unary       : Token operator, Expr right""");

        defineAst(outputPath, "Stmt", """
Expression      : Expr expr
VarDeclaration  : Token name, Expr initializer
Block           : List<Stmt> statements
Print           : Expr expr
""");
    }

    private static void defineAst(Path outputDir, String className, String rules) throws IOException {
        var rulesList = Arrays.stream(rules.split("\n")).toList();
        defineAst(outputDir, className, rulesList);
    }

    private static void defineAst(Path outputDir, String className, List<String> exprOptions) throws IOException {
        var outputFile = outputDir.resolve(className + ".java").toFile();


        try (var writer = new PrintWriter(new FileWriter(outputFile))) {


            writer.println("package com.interpreters.lox;");
            writer.println();
            writer.println("import java.util.List;");
            writer.printf("public sealed interface %s {\n", className);

            var expressionDefs = exprOptions.stream().map(GenerateAst::parseExpressionDef).toList();
            defineVisitor(writer, className, expressionDefs);

            writer.println("\t<R> R accept(Visitor<R> visitor);");
            writer.println();

            for (var expr : expressionDefs) {
                writer.printf(STR.
"""
    record \{expr.name()}(\{expr.fields()}) implements \{className} {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

""");
            }

            writer.write("}");

        }

    }

    private static void defineVisitor(PrintWriter writer, String className, List<ExpressionDef> expressionDefs) {
        writer.println("\tinterface Visitor<R> {");

        for (var exprDef : expressionDefs) {
            writer.println(STR."\t\tR visit(\{ exprDef.name() } \{ className.toLowerCase() });");
        }

        writer.println("\t}");
        writer.println();

    }

    private static ExpressionDef parseExpressionDef(String exprOption) {
        var parts = exprOption.split(":");
        assert parts.length == 2;

        var optionName = parts[0].trim();
        var fields = parts[1].trim();

        return new ExpressionDef(optionName, fields);
    }


    private record ExpressionDef(String name, String fields) {}
}
