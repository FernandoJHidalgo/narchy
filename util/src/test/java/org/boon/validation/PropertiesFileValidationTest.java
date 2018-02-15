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
package org.boon.validation;

import org.boon.validation.RecursiveDescentPropertyValidator.MessageHolder;
import static org.boon.Exceptions.die;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import static org.boon.Maps.map;
import org.boon.validation.readers.PropertiesFileValidatorMetaDataReader;
import org.boon.validation.validators.LengthValidator;
import org.boon.validation.validators.LongRangeValidator;
import org.boon.validation.validators.RequiredValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * @author Selwyn Lehmann
 */
public class PropertiesFileValidationTest {

    @BeforeEach
    public void setup() {
        ValidationContext.create();
    }

    @AfterEach
    public void cleanup() {
        ValidationContext.destroy();
    }

    @Test
    public void testRecursiveWithPropertyFile() {
        Map<String, Object> objectMap = map("/org/boon/validator/required", (Object) new RequiredValidator(), "/org/boon/validator/range", (Object) new LongRangeValidator(), "/org/boon/validator/length", (Object) new LengthValidator(), "/org/boon/validator/personName", Validators.personName("", ""));
        RecursiveDescentPropertyValidator validator = new RecursiveDescentPropertyValidator();
        validator.setValidatorMetaDataReader(new PropertiesFileValidatorMetaDataReader());
        List<MessageHolder> messageHolders = Collections.EMPTY_LIST;
        messageHolders = validator.validateObject(new Employee("Selwyn", 21, "555-555-5555"), objectMap);
        int errors = 0;
        for (MessageHolder messageHolder : messageHolders) {
            outputs(messageHolder.propertyPath);
            outputs(messageHolder.holder.hasError());
            if (messageHolder.holder.hasError()) {
                errors++;
            }
        }
        if (errors > 0) {
            die(" Not expecting any errors ");
        }
    }

    private void outputs(Object propertyPath) {
    }
}
