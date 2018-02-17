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
package org.boon.benchmark.datarepo;

import org.boon.benchmark.datarepo.model.Employee;
import org.boon.benchmark.datarepo.utils.BenchmarkHelper;
import org.boon.benchmark.datarepo.utils.MeasuredRun;
import org.boon.criteria.internal.Criteria;
import org.boon.datarepo.Repo;
import org.boon.datarepo.Repos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.boon.Exceptions.die;
import static org.boon.Lists.list;
import static org.boon.criteria.ObjectFilter.eq;

public class BenchMark {

    static int numCreations = 100_000;

    @Test
    public void test() {
        numCreations = 100;
    }

    public static void main(String[] args) throws Exception {
        final List<Employee> employees = BenchmarkHelper.createMetricTonOfEmployees(numCreations);
        System.out.println("employees created " + employees.size());
        Map<String, List<MeasuredRun>> testResults = new ConcurrentHashMap<>();
        MeasuredRun run1 = testIndex(employees, testResults);
        MeasuredRun run2 = testLinear(employees, testResults);
        List<MeasuredRun> runs = list(run1, run2);
        for (int index = 0; index < 2; index++) {
            for (MeasuredRun run : runs) {
                System.gc();
                Thread.sleep(10);
                run.run();
            }
        }
        Thread.sleep(3_000);
        System.out.println("Waiting...");
        Thread.sleep(1_000);
        System.out.println("Start Now...");
        Thread.sleep(1_000);
        for (int index = 0; index < 5; index++) {
            for (MeasuredRun run : runs) {
                System.gc();
                Thread.sleep(10);
                run.run();
            //puts( "Name", run.name(), "Time", run.time() );
            }
        }
        System.out.println("done");
    }

    private static MeasuredRun testIndex(final List<Employee> employees, final Map<String, List<MeasuredRun>> results) {
        return new MeasuredRun("index ", 1000, 1_000_000, results) {

            Repo repo;

            Criteria exp = eq("firstName", "Mike");

            @Override
            protected void init() {
                /* Create a repo, and decide what to index. */
                repo = Repos.builder().primaryKey("id").searchIndex("firstName").lookupIndex("firstName").build(String.class, Employee.class);
                repo.addAll(employees);
            }

            @Override
            protected void test() {
                List<Employee> results = repo.query(exp);
                boolean found = false;
                for (Employee employee : results) {
                    if (employee.getFirstName().equals("Mike") && employee.getLastName().equals("Middleoflist")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    die("not found");
                }
            }
        };
    }

    private static MeasuredRun testLinear(final List<Employee> employees, final Map<String, List<MeasuredRun>> results) {
        return new MeasuredRun("linear ", 10, 1_000, results) {

            List<Employee> results;

            @Override
            protected void init() {
                results = list(employees);
            }

            @Override
            protected void test() {
                boolean found = false;
                for (Employee employee : results) {
                    if (employee.getFirstName().equals("Mike") && employee.getLastName().equals("Middleoflist")) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    die("not found");
                }
            }
        };
    }
}
