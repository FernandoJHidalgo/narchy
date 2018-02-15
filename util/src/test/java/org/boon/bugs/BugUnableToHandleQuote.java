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
package org.boon.bugs;

import org.boon.Maps;
import org.boon.json.JsonParserAndMapper;
import org.boon.json.JsonParserFactory;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.boon.Boon.puts;
import static org.boon.Boon.toJson;
import static org.boon.Exceptions.die;

/**
 * Created by Richard on 7/24/14.
 */
public class BugUnableToHandleQuote {

    boolean ok;

    @Test
    public void testUnableToHandleQuotesThatAreEsacped() {
        String test = "\"hello\"";
        final JsonParserAndMapper mapper = new JsonParserFactory().create();
        puts(test);
        test = toJson(test);
        puts(test);
        String parsedString = mapper.parseString(test);
        puts(parsedString);
        ok = parsedString.equals("\"hello\"") || die();
    }

    @Test
    public void testUnableToHandleQuotesThatAreEsacped2() {
        String test = "\"hello\"";
        Map<String, Object> map = Maps.map("bob", "jones", "sayHello", (Object) test);
        final JsonParserAndMapper mapper = new JsonParserFactory().create();
        puts(map);
        test = toJson(map);
        puts(test);
        Map<String, Object> newMap = mapper.parseMap(test);
        puts(newMap);
        ok = newMap.get("sayHello").equals("\"hello\"") || die();
    }
}
