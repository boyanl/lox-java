package com.interpreters.lox;

import java.util.List;
public sealed interface Stmt {
	interface Visitor<R> {
		R visit(Expression stmt);
		R visit(VarDeclaration stmt);
		R visit(Block stmt);
		R visit(Print stmt);
		R visit(If stmt);
		R visit(While stmt);
	}

	<R> R accept(Visitor<R> visitor);

    record Expression(Expr expr) implements Stmt {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    record VarDeclaration(Token name, Expr initializer) implements Stmt {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    record Block(List<Stmt> statements) implements Stmt {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    record Print(Expr expr) implements Stmt {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    record If(Expr condition, Stmt thenClause, Stmt elseClause) implements Stmt {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

    record While(Expr condition, Stmt body) implements Stmt {
        @Override
        public <R> R accept(Visitor<R> visitor) {
            return visitor.visit(this);
        }
    }

}