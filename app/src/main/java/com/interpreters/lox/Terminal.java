package com.interpreters.lox;

import org.jline.keymap.BindingReader;
import org.jline.keymap.KeyMap;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.jline.keymap.KeyMap.*;


public class Terminal {

    private enum TerminalOp { INSERT, UP, DOWN, LEFT, RIGHT, EOT, BACKSPACE, DELETE, GOTO_START, GOTO_END, KILL_LINE }

    private final Consumer<String> handler;
    int linePos = 0;
    List<String> history;
    org.jline.terminal.Terminal terminal;

    public Terminal(Consumer<String> handler) {
        this.handler = handler;
        this.history = new ArrayList<>();
    }


    public void run() throws IOException {
        try (var terminal = createTerminal()) {
            this.terminal = terminal;
            terminal.enterRawMode();
            var bindingReader = new BindingReader(terminal.reader());
            var keyMap = createKeymap(terminal);

            while (true) {
                System.out.print("> ");
                String line = readLine(bindingReader, keyMap);
                if (line == null) {
                    return;
                }
                this.handler.accept(line);
                history.add(line);
            }
        }
    }

    private String readLine(BindingReader bindingReader, KeyMap<TerminalOp> keyMap) {

        linePos = 0;
        StringBuilder line = new StringBuilder();
        while (true) {
                TerminalOp op = bindingReader.readBinding(keyMap);

                switch (op) {
                    case INSERT -> {
                        var last = bindingReader.getLastBinding();
                        if (last.charAt(0) == '\r') {
                            System.out.println();
                            return line.toString();
                        } else {
                            if (linePos == line.length()) {
                                line.append(last);
                            } else {
                                line.insert(linePos, last);
                            }

                            var rest = line.substring(linePos);
                            print(rest);
                            back(rest.length() - last.length());
                        }
                    }
                    case UP -> {
                        // TODO: Scroll through history by repeating up/down, currently this only supports last cmd
                        if (line.isEmpty() && !history.isEmpty()) {
                            var last = history.getLast();
                            line.append(last);
                            print(last);
                        }
                    }
                    case LEFT -> {
                        if (linePos > 0) {
                            back(1);
                        }
                    }
                    case RIGHT -> {
                        if (linePos < line.length()) {
                            print(line.charAt(linePos));
                        }
                    }
                    case DOWN -> {
                    }
                    case EOT -> {
                        return null;
                    }
                    case BACKSPACE -> {
                        if (linePos > 0) {
                            del(line, linePos-1);
                        }
                    }
                    case DELETE -> {
                        if (linePos < line.length()) {
                            del(line, linePos);
                        }
                    }
                    case GOTO_START -> {
                        back(linePos);
                    }
                    case GOTO_END -> {
                        if (linePos < line.length()) {
                            print(line.substring(linePos));
                        }
                    }
                    case KILL_LINE -> {
                        var toDelete = line.length() - linePos;
                        if (toDelete > 0) {
                            line.delete(linePos, line.length());
                            print(" ".repeat(toDelete));
                            back(toDelete);
                        }
                    }
                }
            }

    }

    private void back(int n) {
        assert n >= 0;
        System.out.print("\b".repeat(n));
        linePos -= n;
    }

    void print(String s) {
        System.out.print(s);
        linePos += s.length();
    }

    void print(char c) {
        System.out.print(c);
        linePos++;
    }

    private void del(StringBuilder line, int pos) {
        assert linePos >= pos;

        line.deleteCharAt(pos);
        back(linePos - pos);
        var rest = line.substring(pos);
        print(rest + " ");
        back(rest.length() + 1);
    }

    private KeyMap<TerminalOp> createKeymap(org.jline.terminal.Terminal terminal) {
        var keyMap = new KeyMap<TerminalOp>();
        // cursor_left doesn't capture left arrow in KDE's Konsole
        keyMap.bind(TerminalOp.LEFT,  esc() + "[D");
        keyMap.bind(TerminalOp.LEFT, key(terminal, InfoCmp.Capability.cursor_left));
        keyMap.bind(TerminalOp.RIGHT, key(terminal, InfoCmp.Capability.cursor_right));
        keyMap.bind(TerminalOp.UP, key(terminal, InfoCmp.Capability.cursor_up));
        keyMap.bind(TerminalOp.DOWN,  esc() + "[B");
        keyMap.bind(TerminalOp.DOWN, key(terminal, InfoCmp.Capability.cursor_down));
        keyMap.bind(TerminalOp.BACKSPACE, KeyMap.del());
        keyMap.bind(TerminalOp.DELETE, esc() + "[3~");
        keyMap.bind(TerminalOp.GOTO_START, ctrl('a'));
        keyMap.bind(TerminalOp.GOTO_END, ctrl('e'));
        keyMap.bind(TerminalOp.EOT, ctrl('d'));
        keyMap.bind(TerminalOp.KILL_LINE, ctrl('k'));
        // TODO: Add proper back/forward word?
        keyMap.bind(TerminalOp.LEFT, alt('b'));
        keyMap.bind(TerminalOp.RIGHT, alt('f'));
        keyMap.setNomatch(TerminalOp.INSERT);

        return keyMap;
    }


    private org.jline.terminal.Terminal createTerminal() throws IOException {
        return TerminalBuilder.builder()
                .build();

    }
}
