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

    private enum TerminalOp {
        INSERT,
        UP,
        DOWN,
        LEFT,
        RIGHT,
        BACKSPACE,
        DELETE,
        GOTO_START,
        GOTO_END,
        BACKWARD_WORD,
        FORWARD_WORD,
        DELETE_WORD_FORWARD,
        DELETE_WORD_BACKWARD,
        KILL_LINE,
        EOT,
        IGNORE
    }

    private final Consumer<String> handler;
    StringBuilder line;
    int linePos = 0;
    List<String> history;
    org.jline.terminal.Terminal terminal;

    public Terminal(final Consumer<String> handler) {
        this.handler = handler;
        this.history = new ArrayList<>();
    }


    public void run() throws IOException {
        try (final var terminal = createTerminal()) {
            this.terminal = terminal;
            terminal.enterRawMode();
            final var bindingReader = new BindingReader(terminal.reader());
            final var keyMap = createKeymap(terminal);

            while (true) {
                System.out.print("> ");
                final String line = readLine(bindingReader, keyMap);
                if (line == null) {
                    return;
                }
                this.handler.accept(line);
                history.add(line);
            }
        }
    }

    private String readLine(final BindingReader bindingReader, final KeyMap<TerminalOp> keyMap) {

        linePos = 0;
        line = new StringBuilder();
        while (true) {
            final TerminalOp op = bindingReader.readBinding(keyMap);

            switch (op) {
                case INSERT -> {
                    final var last = bindingReader.getLastBinding();
                    if (last.charAt(0) == '\r') {
                        System.out.println();
                        return line.toString();
                    } else {
                        if (linePos == line.length()) {
                            line.append(last);
                        } else {
                            line.insert(linePos, last);
                        }

                        final var rest = line.substring(linePos);
                        print(rest);
                        back(rest.length() - last.length());
                    }
                }
                case UP -> {
                    // TODO: Scroll through history by repeating up/down, currently this only supports last cmd
                    if (line.isEmpty() && !history.isEmpty()) {
                        final var last = history.getLast();
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
                        del(linePos - 1);
                    }
                }
                case DELETE -> {
                    if (linePos < line.length()) {
                        del(linePos);
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
                    final var toDelete = line.length() - linePos;
                    if (toDelete > 0) {
                        line.delete(linePos, line.length());
                        print(" ".repeat(toDelete));
                        back(toDelete);
                    }
                }
                case BACKWARD_WORD -> backwardWord();
                case FORWARD_WORD -> forwardWord();
                case DELETE_WORD_BACKWARD -> {
                    var prev = currentWordStart(linePos);
                    del(prev, linePos);
                }
                case DELETE_WORD_FORWARD -> {
                    var next = currentWordEnd(linePos);
                    del(linePos, Math.min(next+1, line.length()));
                }
                case IGNORE -> {
                }
            }
        }

    }

    private void back(final int n) {
        assert n >= 0;
        System.out.print("\b".repeat(n));
        linePos -= n;
    }

    private void forward(final int n) {
        assert linePos + n <= line.length();
        for (int i = 0; i < n; i++) {

            System.out.print(linePos + i < line.length() ? line.charAt(linePos + i) : ' ');
        }
        linePos += n;
    }

    // Returns the index of the
    private int currentWordStart(final int current) {
        int idx = current - 1;
        while (idx > 0 && !isWordDelimiter(line.charAt(idx - 1))) {
            --idx;
        }
        return Math.max(idx, 0);
    }

    private int currentWordEnd(final int current) {
        int idx = current + 1;
        while (idx + 1 < line.length() && !isWordDelimiter(line.charAt(idx + 1))) {
            ++idx;
        }
        return Math.min(idx, line.length());
    }

    private int skipDelimitersForward(int current) {
        int idx = current;
        while (idx < line.length() && isWordDelimiter(line.charAt(idx))) {
            ++idx;
        }
        return Math.min(idx, line.length());
    }

    private boolean isWordDelimiter(final char c) {
        return Character.isWhitespace(c) || c == '+';
    }

    private void backwardWord() {
        var newPos = currentWordStart(linePos);
        back(linePos - newPos);
    }

    private void forwardWord() {
        var wordEnd = currentWordEnd(linePos);
        var newPos = skipDelimitersForward(Math.min(wordEnd + 1, line.length()));
        forward(newPos - linePos);
    }

    void print(final String s) {
        System.out.print(s);
        linePos += s.length();
    }

    void print(final char c) {
        System.out.print(c);
        linePos++;
    }

    private void del(final int pos) {
        assert linePos >= pos;

        line.deleteCharAt(pos);
        back(linePos - pos);
        final var rest = line.substring(pos);
        print(rest + " ");
        back(rest.length() + 1);
    }

    private void del(int start, int end) {
        if (start == end) {
            return;
        }
        assert 0 <= start && start < line.length() && 0 <= end && end <= line.length() && start < end;
        assert linePos == start || linePos == end;

        var n = end - start;
        if (linePos == end) {
            back(n);
            var rest = line.substring(end);
            print(rest + " ".repeat(n));
            back(rest.length() + n);
        } else if (linePos == start) {
            var rest = line.substring(end);
            print(rest + " ".repeat(n));
            back(rest.length() + n);
        }
        line.delete(start, end);
    }

    private KeyMap<TerminalOp> createKeymap(final org.jline.terminal.Terminal terminal) {
        final var keyMap = new KeyMap<TerminalOp>();
        // cursor_left doesn't capture left arrow in KDE's Konsole
        keyMap.bind(TerminalOp.LEFT, esc() + "[D");
        keyMap.bind(TerminalOp.LEFT, key(terminal, InfoCmp.Capability.cursor_left));
        keyMap.bind(TerminalOp.RIGHT, key(terminal, InfoCmp.Capability.cursor_right));
        keyMap.bind(TerminalOp.UP, key(terminal, InfoCmp.Capability.cursor_up));
        keyMap.bind(TerminalOp.DOWN, esc() + "[B");
        keyMap.bind(TerminalOp.DOWN, key(terminal, InfoCmp.Capability.cursor_down));
        keyMap.bind(TerminalOp.BACKSPACE, KeyMap.del());
        keyMap.bind(TerminalOp.DELETE, esc() + "[3~");
        keyMap.bind(TerminalOp.GOTO_START, ctrl('a'));
        keyMap.bind(TerminalOp.GOTO_END, ctrl('e'));
        keyMap.bind(TerminalOp.GOTO_START, esc() + "[H");
        keyMap.bind(TerminalOp.GOTO_END, ctrl('e'));
        keyMap.bind(TerminalOp.GOTO_END, esc() + "[F");
        keyMap.bind(TerminalOp.EOT, ctrl('d'));
        keyMap.bind(TerminalOp.KILL_LINE, ctrl('k'));
        keyMap.bind(TerminalOp.BACKWARD_WORD, alt('b'));
        keyMap.bind(TerminalOp.FORWARD_WORD, alt('f'));
        keyMap.bind(TerminalOp.DELETE_WORD_FORWARD, alt('d'));
        keyMap.bind(TerminalOp.DELETE_WORD_BACKWARD, ctrl('w'));

        // PageUp, PageDown, Print Screen, Context Menu button, Num Lock (== Escape)
        final var nonPrintable = List.of(esc() + "[5~", esc() + "[6~", esc() + "[1;2P", ctrl('P'), esc());
        keyMap.bind(TerminalOp.IGNORE, nonPrintable); // ignore PageUp

        keyMap.setNomatch(TerminalOp.INSERT);

        return keyMap;
    }


    private org.jline.terminal.Terminal createTerminal() throws IOException {
        return TerminalBuilder.builder()
                .build();

    }
}
