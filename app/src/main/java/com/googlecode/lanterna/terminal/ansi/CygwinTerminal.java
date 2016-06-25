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
package com.googlecode.lanterna.terminal.ansi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * This class extends UnixLikeTerminal and implements the Cygwin-specific implementations. This means, running a Java
 * application using Lanterna inside the Cygwin Terminal application. The standard Windows command prompt (cmd.exe) is
 * not supported by this class.<p>
 * <p>
 * <b>NOTE:</b> This class is experimental and does not fully work! Some of the operations, like disabling echo and
 * changing cbreak seems to be impossible to do without resorting to native code. Running "stty raw" before starting the
 * JVM will improve compatibility.
 *
 * @author Martin
 * @author Andreas
 */
public class CygwinTerminal extends UnixLikeTerminal {
    /**
     * Creates a new CygwinTerminal based off input and output streams and a character set to use
     * @param terminalInput Input stream to read input from
     * @param terminalOutput Output stream to write output to
     * @param terminalCharset Character set to use when writing to the output stream
     * @throws IOException If there was an I/O error when trying to initialize the class and setup the terminal
     */
    public CygwinTerminal(
            InputStream terminalInput,
            OutputStream terminalOutput,
            Charset terminalCharset) throws IOException {
        super(new CygwinSTTYTerminalDeviceController(),
                terminalInput,
                terminalOutput,
                terminalCharset,
                CtrlCBehaviour.TRAP);
    }
}
