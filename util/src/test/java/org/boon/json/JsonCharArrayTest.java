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
package org.boon.json;

import org.boon.BoonTest;
import org.boon.IO;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.Map;

/**
 * Created by rick on 12/12/13.
 */
public class JsonCharArrayTest extends JsonParserAndMapperBaseTest {

    public JsonParserAndMapper parser() {
        return new JsonParserFactory().createJsonCharArrayParser();
    }

    public JsonParserAndMapper objectParser() {
        return parser();
    }

    @Test
    public void testNest() throws URISyntaxException {
        String nest = IO.read(BoonTest.resource("files/nest.json"));
        this.jsonParserAndMapper.parse(Map.class, nest);
    }

    @Test
    public void noNest() throws URISyntaxException {
        String json = IO.read(BoonTest.resource("files/nonest.json"));
        this.jsonParserAndMapper.parse(Map.class, json);
    }
}
