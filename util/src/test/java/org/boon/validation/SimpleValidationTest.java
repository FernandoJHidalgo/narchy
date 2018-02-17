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

import org.boon.Lists;
import org.boon.validation.annotations.Length;
import org.boon.validation.annotations.ProperNoun;
import org.boon.validation.validators.CompositeValidator;
import org.boon.validation.validators.LengthValidator;
import org.boon.validation.validators.RequiredValidator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.boon.Exceptions.die;
import static org.boon.Maps.map;
import static org.boon.validation.ValidatorMetaData.validatorMeta;
import static org.boon.validation.Validators.length;
import static org.boon.validation.Validators.required;

public class SimpleValidationTest {

    @BeforeEach
    public void setup() {
        ValidationContext.create();
    }

    @AfterEach
    public void cleanup() {
        ValidationContext.destroy();
    }

    @Test
    public void testRequired() {
        RequiredValidator required = Validators.required("phone number required");
        ValidatorMessage message = (ValidatorMessage) required.validate(null, "Phone Number");
        boolean ok = true;
        ok |= message.hasError() || die("Phone number is required");
        message = (ValidatorMessage) required.validate("", "Phone Number");
        //Empty strings don't count!
        ok |= message.hasError() || die("Phone number is required");
    }

    @Test
    public void testLengthShowsMustBePresentToBeValidated() {
        LengthValidator length = Validators.length(7, 12, "phone number must be 7 to  12 characters long");
        ValidatorMessage message = (ValidatorMessage) length.validate(null, "Phone Number");
        boolean ok = true;
        ok |= !message.hasError() || die("Phone number must be between 7, and 12");
        message = (ValidatorMessage) length.validate("", "Phone Number");
        ok |= message.hasError() || die("Phone number must be between 7 and 12");
    }

    @Test
    public void testComposite() {
        CompositeValidator validators = Validators.validators(required("phone number required"), length(7, 12, "phone number must be 7 to  12 characters long"));
        ValidatorMessages messages = (ValidatorMessages) validators.validate(null, "Phone Number");
        boolean ok = true;
        ok |= messages.hasError() || die("required");
        messages = (ValidatorMessages) validators.validate("123", "Phone Number");
        ok |= messages.hasError() || die("wrong length");
        messages = (ValidatorMessages) validators.validate("1231234567", "Phone Number");
        ok |= !messages.hasError() || die("all good now");
    }

    public static class Employee {

        String firstName;

        int age;

        String phone;

        public Employee(String name, int age, String phone) {
            this.firstName = name;
            this.age = age;
            this.phone = phone;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    Map<String, List<ValidatorMetaData>> rules = map("phone", Lists.list(validatorMeta("length", map("max", (Object) 12, "min", 5))), "firstName", Lists.list(validatorMeta("personName", Collections.EMPTY_MAP)));

    Map<Class<Employee>, Map<String, List<ValidatorMetaData>>> classToRulesMap = map(Employee.class, rules);

    class ValidatorReader implements ValidatorMetaDataReader {

        @Override
        public List<ValidatorMetaData> readMetaData(Class<?> clazz, String propertyName) {
            if (classToRulesMap.get(clazz).get(propertyName) == null) {
                return Collections.EMPTY_LIST;
            } else {
                return classToRulesMap.get(clazz).get(propertyName);
            }
        }
    }

    @Test
    public void testRecursive() {
        Map<String, Object> objectMap = map("/org/boon/validator/length", (Object) new LengthValidator(), "/org/boon/validator/personName", Validators.personName("", ""));
        RecursiveDescentPropertyValidator validator = new RecursiveDescentPropertyValidator();
        validator.setValidatorMetaDataReader(new ValidatorReader());
        List<RecursiveDescentPropertyValidator.MessageHolder> messageHolders = Collections.EMPTY_LIST;
        messageHolders = validator.validateObject(new Employee("Rick", 43, "555-121-3333"), objectMap);
        int errors = 0;
        for (RecursiveDescentPropertyValidator.MessageHolder messageHolder : messageHolders) {
            outputs(messageHolder.propertyPath);
            outputs(messageHolder.holder.hasError());
            if (messageHolder.holder.hasError()) {
                errors++;
            }
        }
        if (errors > 0) {
            die(" Not expecting any errors ");
        }
        messageHolders = validator.validateObject(new Employee("123", 50, "A"), objectMap);
        errors = 0;
        for (RecursiveDescentPropertyValidator.MessageHolder messageHolder : messageHolders) {
            outputs(messageHolder.propertyPath);
            outputs(messageHolder.holder.hasError());
            if (messageHolder.holder.hasError()) {
                errors++;
            }
        }
        if (errors != 2) {
            die(" expecting two errors " + errors);
        }
    }

    private void outputs(Object propertyPath) {
    }

    public static class Employee2 {

        @ProperNoun(detailMessage = "First Name must be a proper noun")
        String firstName;

        int age;

        @Length(max = 12, min = 5, detailMessage = "Phone number must be a phone number")
        String phone;

        public Employee2(String name, int age, String phone) {
            this.firstName = name;
            this.age = age;
            this.phone = phone;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    @Test
    public void testRecursiveWithAnnotations() {
        Map<String, Object> objectMap = map("/org/boon/validator/length", (Object) new LengthValidator(), "/org/boon/validator/properNoun", Validators.properNoun("", ""));
        RecursiveDescentPropertyValidator validator = new RecursiveDescentPropertyValidator();
        List<RecursiveDescentPropertyValidator.MessageHolder> messageHolders = Collections.EMPTY_LIST;
        messageHolders = validator.validateObject(new Employee2("Rick", 43, "555-121-3333"), objectMap);
        int errors = 0;
        for (RecursiveDescentPropertyValidator.MessageHolder messageHolder : messageHolders) {
            outputs(messageHolder.propertyPath);
            outputs(messageHolder.holder.hasError());
            if (messageHolder.holder.hasError()) {
                errors++;
            }
        }
        if (errors > 0) {
            die(" Not expecting any errors ");
        }
        messageHolders = validator.validateObject(new Employee2("123", 50, "A"), objectMap);
        errors = 0;
        for (RecursiveDescentPropertyValidator.MessageHolder messageHolder : messageHolders) {
            outputs(messageHolder.propertyPath);
            outputs(messageHolder.holder.hasError());
            if (messageHolder.holder.hasError()) {
                errors++;
            }
        }
        if (errors != 2) {
            die(" expecting two errors " + errors);
        }
    }
}
