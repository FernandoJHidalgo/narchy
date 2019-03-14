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

package com.googlecode.lanterna.terminal;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TestTerminalFactory;
import com.googlecode.lanterna.TextColor;

import java.io.IOException;
import java.util.Random;

/**
 * This class will try using the 8-bit indexed color extension supported by some terminal emulators
 * @author Martin
 */
public class Terminal8bitIndexedColorTest {
    public static void main(String[] args) throws IOException {
        final String string = "Hello!";
        Random random = new Random();
        Terminal terminal = new TestTerminalFactory(args).createTerminal();
        terminal.enterPrivateMode();
        terminal.clearScreen();
        TerminalPosition size = terminal.terminalSize();

        while(true) {
            if(terminal.pollInput() != null) {
                terminal.exitPrivateMode();
                return;
            }

            TextColor.Indexed foregroundIndex = TextColor.Indexed.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255));
            TextColor.Indexed backgroundIndex = TextColor.Indexed.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255));

            terminal.fore(foregroundIndex);
            terminal.back(backgroundIndex);
            terminal.moveCursorTo(random.nextInt(size.col - string.length()), random.nextInt(size.row));
            printString(terminal, string);

            try {
                Thread.sleep(200);
            }
            catch(InterruptedException e) {
            }
        }
    }

    private static void printString(Terminal terminal, String string) throws IOException {
        for(int i = 0; i < string.length(); i++)
            terminal.put(string.charAt(i));
        terminal.flush();
    }
}