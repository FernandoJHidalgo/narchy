/*
 * Copyright 2013-2014 Richard M. Hightower
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * __________                              _____          __   .__
 * \______   \ ____   ____   ____   /\    /     \ _____  |  | _|__| ____    ____
 *  |    |  _//  _ \ /  _ \ /    \  \/   /  \ /  \\__  \ |  |/ /  |/    \  / ___\
 *  |    |   (  <_> |  <_> )   |  \ /\  /    Y    \/ __ \|    <|  |   |  \/ /_/  >
 *  |______  /\____/ \____/|___|  / \/  \____|__  (____  /__|_ \__|___|  /\___  /
 *         \/                   \/              \/     \/     \/       \//_____/
 *      ____.                     ___________   _____    ______________.___.
 *     |    |____ ___  _______    \_   _____/  /  _  \  /   _____/\__  |   |
 *     |    \__  \\  \/ /\__  \    |    __)_  /  /_\  \ \_____  \  /   |   |
 * /\__|    |/ __ \\   /  / __ \_  |        \/    |    \/        \ \____   |
 * \________(____  /\_/  (____  / /_______  /\____|__  /_______  / / ______|
 *               \/           \/          \/         \/        \/  \/
 */

package org.boon.examples;

import java.io.*;
import java.net.*;
import java.util.Scanner;


public class EchoClient {
    public static void main( String... args ) throws IOException {

        String host;
        int port;

        if ( args.length != 2 ) {
            System.out.println( "binding to port localhost:9999" );
            host = "localhost";
            port = 9999;
        } else {
            host = args[ 0 ];
            port = Integer.parseInt( args[ 1 ] );

        }


        try (
                Socket echoSocket = new Socket( host, port );
                PrintWriter socketOut =
                        new PrintWriter( echoSocket.getOutputStream(), true );
                Scanner socketIn = new Scanner( echoSocket.getInputStream() );
                Scanner console = new Scanner( System.in );
        ) {
            System.out.println( "TypeType in some text please." );
            while ( console.hasNextLine() ) {
                String userInput = console.nextLine();
                socketOut.println( userInput );
                System.out.println( "echo: " + socketIn.nextLine() );
            }
        }


    }

}