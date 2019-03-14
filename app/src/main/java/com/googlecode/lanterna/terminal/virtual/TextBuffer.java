/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2016 Martin
 */
package com.googlecode.lanterna.terminal.virtual;

import com.googlecode.lanterna.TextCharacter;
import nars.$;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

/**
 * This class is used to store lines of text inside of a terminal emulator. As used by {@link DefaultVirtualTerminal}, it keeps
 * two {@link TextBuffer}s, one for private mode and one for normal mode and it can switch between them as needed.
 */
class TextBuffer {
    private static final TextCharacter DOUBLE_WIDTH_CHAR_PADDING = new TextCharacter(' ');

    @Deprecated
    public final List<List<TextCharacter>> lines;
    private final int maxLineWidth;

    TextBuffer(int maxLineWidth) {
        this.lines =
                //new ArrayDeque();
                new LinkedList<>();
        this.maxLineWidth = maxLineWidth;
        newLine();
    }

    void newLine() {
        synchronized (lines) {
            lines.add($.newArrayList(maxLineWidth));
        }
    }

    void removeTopLines(int numberOfLinesToRemove) {
        synchronized (lines) {
            int n = Math.min(lines.size(), numberOfLinesToRemove);
            if (n > 0) {

                Iterator<List<TextCharacter>> x = lines.iterator();
                while (x.hasNext() && ((n--) > 0)) {
                    x.next();
                    x.remove();
                }
            }
        }
//        for(int i = 0; i < numberOfLinesToRemove; i++) {
//            lines.removeFirst();
//        }
    }

    void clear() {
        synchronized (lines) {
            lines.clear();
        }
        newLine();
    }


    @NotNull
    public List<TextCharacter> getLine(int l) {
        synchronized (lines) {
            if (lines.size() > l)
                return lines.get(l);
            else
                return Collections.emptyList();
        }
    }

    int getLineCount() {
        return lines.size();
    }

    int set(int lineNumber, int columnIndex, TextCharacter textCharacter) {
        if (lineNumber < 0 || columnIndex < 0) {
            throw new IllegalArgumentException("Illegal argument to TextBuffer.setCharacter(..), lineNumber = " +
                    lineNumber + ", columnIndex = " + columnIndex);
        }
        if (textCharacter == null) {
            textCharacter = TextCharacter.DEFAULT_CHARACTER;
        }

        List<TextCharacter> line;
        synchronized (lines) {
            int s = lines.size();
            while (lineNumber >= s) {
                newLine();
                s++;
            }
            line = lines.get(lineNumber);
        }

        while (line.size() <= columnIndex) {
            line.add(TextCharacter.DEFAULT_CHARACTER);
        }


        // Default
        int returnStyle = 0;

        // Check if we are overwriting a double-width character, in that case we need to reset the other half
        TextCharacter lc = line.get(columnIndex);
        if (lc.isDoubleWidth()) {
            line.set(columnIndex + 1, lc.withCharacter(' '));
            returnStyle = 1; // this character and the one to the right
        } else if (lc == DOUBLE_WIDTH_CHAR_PADDING) {
            line.set(columnIndex - 1, TextCharacter.DEFAULT_CHARACTER);
            returnStyle = 2; // this character and the one to the left
        }
        line.set(columnIndex, textCharacter);

        if (textCharacter.isDoubleWidth()) {
            // We don't report this column as dirty (yet), it's implied since a double-width character is reported
            set(lineNumber, columnIndex + 1, DOUBLE_WIDTH_CHAR_PADDING);
        }
        return returnStyle;
    }

    TextCharacter get(int lineNumber, int columnIndex) {
        if (lineNumber < 0 || columnIndex < 0) {
            throw new IllegalArgumentException("Illegal argument to TextBuffer.getCharacter(..), lineNumber = " +
                    lineNumber + ", columnIndex = " + columnIndex);
        }

        List<TextCharacter> line;
        synchronized (lines) {
            if (lineNumber >= lines.size()) {
                return TextCharacter.DEFAULT_CHARACTER;
            }

            line = getLine(lineNumber);
        }

        if (line.size() <= columnIndex) {
            return TextCharacter.DEFAULT_CHARACTER;
        }
        TextCharacter textCharacter = line.get(columnIndex);
        if (textCharacter == DOUBLE_WIDTH_CHAR_PADDING) {
            return line.get(columnIndex - 1);
        }
        return textCharacter;
    }

    final static VirtualTerminal.BufferLine emptyLine = new VirtualTerminal.BufferLine() {
        @Override
        public TextCharacter getCharacterAt(int column) {
            return TextCharacter.DEFAULT_CHARACTER;
        }
    };

    @Deprecated public void forEachLine(int startRow, int endRow, VirtualTerminal.BufferWalker bufferWalker) {

        synchronized (lines) {
            ListIterator<List<TextCharacter>> iterator = lines.listIterator(startRow);
            for (int row = startRow; row <= endRow; row++) {
                VirtualTerminal.BufferLine bufferLine = emptyLine;
                if (iterator.hasNext()) {
                    final List<TextCharacter> list = iterator.next();
                    bufferLine = column -> {
                        if (column >= list.size()) {
                            return TextCharacter.DEFAULT_CHARACTER;
                        }
                        return list.get(column);
                    };
                }
                bufferWalker.onLine(row, bufferLine);
            }
        }

    }
    public void forEachLine(int startRow, int endRow, Consumer<List<TextCharacter>> each) {

        synchronized (lines) {
            ListIterator<List<TextCharacter>> iterator = lines.listIterator(startRow);
            for (int row = startRow; iterator.hasNext() && row <= endRow; row++) {
                final List<TextCharacter> list = iterator.next();
                each.accept(list);
            }
        }

    }

}