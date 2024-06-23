build:
	javac --enable-preview --release 21 app/src/main/java/com/interpreters/lox/*.java -cp lib/*.jar -d target/
