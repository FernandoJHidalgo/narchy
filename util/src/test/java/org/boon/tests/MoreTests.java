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
package org.boon.tests;

import org.boon.Lists;
import org.boon.core.Typ;
import org.boon.criteria.ObjectFilter;
import org.boon.criteria.ProjectedSelector;
import org.boon.criteria.internal.Criteria;
import org.boon.datarepo.DataRepoException;
import org.boon.datarepo.Repo;
import org.boon.datarepo.RepoBuilder;
import org.boon.datarepo.Repos;
import org.boon.datarepo.impl.RepoBuilderDefault;
import org.boon.tests.model.Employee;
import org.boon.tests.model.HourlyEmployee;
import org.boon.tests.model.SalesEmployee;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.boon.Exceptions.die;
import static org.boon.core.Conversions.toDate;
import static org.boon.core.reflection.BeanUtils.copy;
import static org.boon.criteria.ObjectFilter.empty;
import static org.boon.criteria.ObjectFilter.eq;
import static org.boon.datarepo.Collections.$q;
import static org.boon.datarepo.Collections.sortedQuery;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoreTests {

    List<Employee> list;

    List<Employee> bigList;

    List<Employee> h_list;

    @BeforeEach
    public void setUp() throws Exception {
        list = Lists.list(Employee.employee("firstA", "LastA", "123", "5.29.1970:00:00:01", 100), Employee.employee("firstB", "LastB", "124", "5.29.1960:00:00:00", 200));
        h_list = Lists.list(Employee.employee("firstA", "LastA", "123", "5.29.1970:00:00:01", 100), Employee.employee("firstB", "LastB", "124", "5.29.1960:00:00:00", 200), Employee.employee("firstZ", "LastB", "125", "5.29.1960:00:00:00", 200, true), new HourlyEmployee());
        bigList = copy(list);
        for (int index = 0; index < 2000; index++) {
            String dateString = "5.29.1970:00:00:01";
            if (index % 2 == 0) {
                dateString = "5.29.1950:00:00:01";
            } else {
                dateString = "5.29.1990:00:00:01";
            }
            bigList.add(Employee.employee("firstC" + index, "last" + index, "ssn" + index, dateString, 1000 + index));
        }
    }

    @Test
    public void testProjections() throws Exception {
        Repo<String, Employee> repo = Repos.builder().primaryKey("id").searchIndex("salary").build(Typ.string, Employee.class);
        repo.addAll(bigList);
        int max = repo.results(ObjectFilter.gt("salary", 100)).firstInt(ProjectedSelector.max("salary"));
        assertEquals(2999, max);
        max = repo.results(ObjectFilter.gt("salary", 100)).firstInt(ProjectedSelector.maxInt("salary"));
        assertEquals(2999, max);
        int min = repo.results(ObjectFilter.gt("salary", 0)).firstInt(ProjectedSelector.min("salary"));
        assertEquals(100, min);
        min = repo.results(ObjectFilter.gt("salary", 0)).firstInt(ProjectedSelector.minInt("salary"));
        assertEquals(100, min);
    }

    @Test
    public void fieldOnlyInSubClass() throws Exception {
        List<Employee> queryableList = $q(h_list, SalesEmployee.class);
        List<Employee> results = sortedQuery(queryableList, "firstName", eq("commissionRate", 1));
        assertEquals(1, results.size());
        assertEquals("SalesEmployee", results.get(0).getClass().getSimpleName());
    }

    @Test
    public void fieldOnlyInSubClass2() throws Exception {
        List<Employee> queryableList = $q(h_list, Employee.class, SalesEmployee.class);
        List<Employee> results = sortedQuery(queryableList, "firstName", eq("commissionRate", 1));
        assertEquals(1, results.size());
        assertEquals("SalesEmployee", results.get(0).getClass().getSimpleName());
    }

    @Test
    public void fieldOnlyInSubClass3() throws Exception {
        List<Employee> queryableList = $q(h_list, Employee.class, SalesEmployee.class, HourlyEmployee.class);
        List<Employee> results = sortedQuery(queryableList, "firstName", eq("commissionRate", 1));
        assertEquals(1, results.size());
        assertEquals("SalesEmployee", results.get(0).getClass().getSimpleName());
        results = sortedQuery(queryableList, "firstName", eq("weeklyHours", 40));
        assertEquals(1, results.size());
        assertEquals("HourlyEmployee", results.get(0).getClass().getSimpleName());
    }

    public void fieldOnlyInSubClass4() throws Exception {
        List<Employee> queryableList = $q(h_list, Employee.class, SalesEmployee.class);
        List<Employee> results = sortedQuery(queryableList, "firstName", eq("commissionRate", 1));
        assertEquals(1, results.size());
        assertEquals("SalesEmployee", results.get(0).getClass().getSimpleName());
        results = sortedQuery(queryableList, "firstName", eq("weeklyHours", 40));
        assertEquals(0, results.size());
    }

    @Test
    public void typeOfTestLongName() throws Exception {
        List<Employee> queryableList = $q(h_list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.typeOf("SalesEmployee"));
        assertEquals(1, results.size());
        assertEquals("SalesEmployee", results.get(0).getClass().getSimpleName());
    }

    @Test
    public void typeOfTest() throws Exception {
        List<Employee> queryableList = $q(h_list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.typeOf("SalesEmployee"));
        assertEquals(1, results.size());
        assertEquals("SalesEmployee", results.get(0).getClass().getSimpleName());
    }

    @Test
    public void instanceOfTest() throws Exception {
        List<Employee> queryableList = $q(h_list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.instanceOf(SalesEmployee.class));
        assertEquals(1, results.size());
        assertEquals("SalesEmployee", results.get(0).getClass().getSimpleName());
    }

    @Test
    public void implementsTest() throws Exception {
        List<Employee> queryableList = $q(h_list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.implementsInterface(Comparable.class));
        assertEquals(1, results.size());
        assertEquals("SalesEmployee", results.get(0).getClass().getSimpleName());
    }

    @Test
    public void superClassTest() throws Exception {
        List<Employee> queryableList = $q(h_list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.typeOf("ZSalaryEmployee"));
        assertEquals(0, results.size());
    }

    @Test
    public void testBetweenSalary_AND_LOTS_OF_TERMS_BIG_LIST() throws Exception {
        List<Employee> queryableList = $q(bigList, Employee.class, HourlyEmployee.class);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.and(ObjectFilter.between("salary", 1000, 2000), eq("firstName", "firstC1"), ObjectFilter.startsWith("lastName", "last"), ObjectFilter.gt("birthDate", toDate("5.29.1940")), ObjectFilter.startsWith("id", "ssn"), ObjectFilter.gt("salary", 1000)));
        assertEquals(1, results.size());
    }

    @Test
    public void testBetweenSalary_OR_PRECISE_NESTED_OR_AND() throws Exception {
        List<Employee> queryableList = $q(bigList);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.or(ObjectFilter.between("salary", 1001, 1002), ObjectFilter.between("salary", 1002, 1003), ObjectFilter.or(eq("firstName", "firstC12"), ObjectFilter.and(eq("firstName", "firstC10"), eq("firstName", "firstC11"))), ObjectFilter.and(eq("firstName", "firstC20"), eq("firstName", "firstC21"), eq("firstName", "first22"), ObjectFilter.or(eq("firstName", "firstC30"), eq("firstName", "firstC31"))), ObjectFilter.or(ObjectFilter.or(ObjectFilter.or(eq("firstName", "firstC52"), ObjectFilter.and(eq("firstName", "firstC60"), eq("firstName", "firstC61")))))));
        System.out.println(results);
        assertEquals(4, results.size());
        assertEquals("firstC1", results.get(0).getFirstName());
        assertEquals("firstC12", results.get(1).getFirstName());
        assertEquals("firstC2", results.get(2).getFirstName());
        assertEquals("firstC52", results.get(3).getFirstName());
    }

    @Test
    public void testBetweenSalary_OR_PRECISE_NESTED_AND() throws Exception {
        List<Employee> queryableList = $q(bigList);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.or(ObjectFilter.between("salary", 1001, 1002), ObjectFilter.between("salary", 1002, 1003), ObjectFilter.and(eq("firstName", "firstC10"), eq("firstName", "firstC11"))));
        assertEquals(2, results.size());
    }

    @Test
    public void testBetweenSalary_OR_PRECISE() throws Exception {
        List<Employee> queryableList = $q(bigList);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.or(ObjectFilter.between("salary", 1001, 1002), ObjectFilter.between("salary", 1002, 1003), eq("firstName", "firstC10"), eq("firstName", "firstC11")));
        assertEquals(4, results.size());
    }

    @Test
    public void testBetweenSalary_OR_LOTS_OF_TERMS_BIG_LIST() throws Exception {
        List<Employee> queryableList = $q(bigList);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.or(ObjectFilter.between("salary", 1000, 2000), eq("firstName", "firstC1"), ObjectFilter.startsWith("lastName", "last"), ObjectFilter.gt("birthDate", toDate("5.29.1940")), ObjectFilter.startsWith("id", "ssn"), ObjectFilter.gt("salary", 1000)));
        assertEquals(2002, results.size());
    }

    @Test
    public void testBetweenSalary_OR_FirstNameEQ_SECOND_TERM_NOT_FOUND_BIG_LIST() throws Exception {
        List<Employee> queryableList = $q(bigList);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.or(ObjectFilter.between("salary", 1000, 2000), eq("firstName", "RANDOMISHSTRING" + System.currentTimeMillis())));
        assertEquals(1000, results.size());
    }

    @Test
    public void testBetweenSalary_AND_FirstNameEQ_SECOND_TERM_NOT_FOUND_BIG_LIST() throws Exception {
        List<Employee> queryableList = $q(bigList);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.between("salary", 1000, 2000), eq("firstName", "RANDOMISHSTRING" + System.currentTimeMillis()));
        assertEquals(0, results.size());
    }

    @Test
    public void testBetweenSalary_OR_FirstNameEQ_FIRST_TERM_NOT_FOUND_BIG_LIST() throws Exception {
        List<Employee> queryableList = $q(bigList);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.or(ObjectFilter.between("salary", 1, 3), ObjectFilter.startsWith("firstName", "firstC")));
        assertEquals(2000, results.size());
    }

    @Test
    public void testBetweenSalary_AND_FirstNameEQ_FIRST_TERM_NOT_FOUND_BIG_LIST() throws Exception {
        List<Employee> queryableList = $q(bigList);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.and(ObjectFilter.between("salary", 1, 3), ObjectFilter.startsWith("firstName", "firstC")));
        assertEquals(0, results.size());
    }

    @Test
    public void testBetweenSalary_OR_FirstNameEQ_SECOND_TERM_NOT_FOUND() throws Exception {
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.or(ObjectFilter.between("salary", 199, 201), eq("firstName", "RANDOMISHSTRING" + System.currentTimeMillis())));
        assertEquals(1, results.size());
    }

    @Test
    public void testBetweenSalary_OR_FirstNameEQ_FIRST_TERM_NOT_FOUND() throws Exception {
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.or(ObjectFilter.between("salary", -1, -1), eq("firstName", "firstA")));
        assertEquals(1, results.size());
    }

    @Test
    public void testBetweenSalaryAndFirstNameEQ_FIRST_TERM_NOT_FOUND() throws Exception {
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.and(ObjectFilter.between("salary", -1, -1), eq("firstName", "firstA")));
        assertEquals(0, results.size());
    }

    @Test
    public void testBetweenSalaryAndFirstNameEQ_SECOND_TERM_NOT_FOUND() throws Exception {
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.and(ObjectFilter.between("salary", 0, 1000), eq("firstName", "RANDOMISHSTRING" + System.currentTimeMillis())));
        assertEquals(0, results.size());
    }

    @Test
    public void testBetweenSalaryAndFirstNameEQ_SECOND_TERM_FOUND() throws Exception {
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.and(ObjectFilter.between("salary", 0, 1000), eq("firstName", "firstA")));
        assertEquals(1, results.size());
    }

    @Test
    public void testBetweenSalary() throws Exception {
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.between("salary", 100, 200));
        assertEquals(1, results.size());
        assertEquals("firstA", results.get(0).getFirstName());
    }

    @Test
    public void testBetweenSalaryExact() throws Exception {
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.between("salary", 100, 201));
        assertEquals(2, results.size());
        assertEquals("firstA", results.get(0).getFirstName());
        assertEquals("firstB", results.get(1).getFirstName());
    }

    @Test
    public void testBetweenSalaryExactOutOfRange() throws Exception {
        //rint(listStream);
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.between("salary", 400, 500));
        assertEquals(0, results.size());
    }

    //    @Test  //Java data handling SUCKS! I don't think it is an issue with index lib.
    //    public void testBetweenDateExact() throws Exception {
    //
    //        //rint(listStream);
    //
    //        List<Employee> queryableList = $q( list );
    //        List<Employee> results = sortedQuery( queryableList, "firstName", ObjectFilter.between( "birthDate", "5/29/1960:00:00:01", "5/29/1970:00:00:00" ) );
    //
    //        assertEquals( 2, results.size() );
    //        assertEquals( "firstA", results.get( 0 ).getFirstName() );
    //        assertEquals( "firstB", results.get( 1 ).getFirstName() );
    //
    //    }
    @Test
    public void testBetweenDateExactJustOverAndUnder1Year() throws Exception {
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.between(Employee.class, "birthDate", "5/29/1959", "5/29/1971"));
        assertEquals(2, results.size());
        assertEquals("firstA", results.get(0).getFirstName());
        assertEquals("firstB", results.get(1).getFirstName());
    }

    @Test
    public void testBetweenDate() throws Exception {
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.between(Employee.class, "birthDate", "5/29/1950", "5/29/1990"));
        assertEquals(2, results.size());
        assertEquals("firstA", results.get(0).getFirstName());
        assertEquals("firstB", results.get(1).getFirstName());
    }

    @Test
    public void testBetweenDatePreInit() throws Exception {
        List<Employee> queryableList = $q(list);
        List<Employee> results = sortedQuery(queryableList, "firstName", ObjectFilter.between(Employee.class, "birthDate", "5/29/1950", "5/29/1990"));
        assertEquals(2, results.size());
        assertEquals("firstA", results.get(0).getFirstName());
        assertEquals("firstB", results.get(1).getFirstName());
    }

    //Test from chris
    @Test
    public void testLinearVsIndexedEmpty() {
        //Add 2,000 users 1000 with no last name, and 1000 Smith
        List<Employee> employees = new ArrayList<>();
        for (int i = 0; i < 2000; i++) {
            Employee e = new Employee();
            if (i % 2 == 0) {
                e.setLastName("");
            } else {
                e.setLastName("Smith");
            }
            //NO ID no employee added
            e.setId("" + i);
            employees.add(e);
        }
        RepoBuilder indexedRepoBuilder = new RepoBuilderDefault();
        //build repo indexed on id, with search index on lastName
        Repo<String, Employee> indexedRepo = indexedRepoBuilder.primaryKey("id").searchIndex("lastName").build(String.class, Employee.class);
        //dump employees into repo
        indexedRepo.addAll(employees);
        //now see who does not have a lastName
        List<Employee> indexedResult = indexedRepo.query(empty("lastName"));
        //Test
        assertEquals(1000, indexedResult.size());
        RepoBuilder nonindexedRepoBuilder = new RepoBuilderDefault();
        Repo<String, Employee> nonindexedRepo = nonindexedRepoBuilder.primaryKey("id").build(String.class, Employee.class);
        nonindexedRepo.addAll(employees);
        assertEquals(2000, nonindexedRepo.size());
        List<Employee> nonindexedResult = nonindexedRepo.query(empty("lastName"));
        assertEquals(1000, nonindexedResult.size());
    }

    @Test
    public void testDeleteWithNullIndexedField() {
        Repo<String, Employee> repo = Repos.builder().primaryKey("id").searchIndex("firstName").build(Typ.string, Employee.class);
        Employee e = Employee.employee(null, "LastA", "9131971", "5.29.1970:00:00:01", 100);
        repo.put(e);
        e = repo.get("9131971");
        assertEquals("9131971", e.getId());
        repo.delete(e);
        e = repo.get("9131971");
        assertEquals(null, e);
    }

    @Test
    public void testQueryAfterUpdate() {
        String id = "9131971";
        Repo<String, Employee> repo = Repos.builder().primaryKey("id").searchIndex("firstName").build(Typ.string, Employee.class);
        Employee e = Employee.employee("FirstA", "LastA", id, "5.29.1970:00:00:01", 100);
        repo.put(e);
        assertEquals(1, repo.size());
        // Find the new employee
        Criteria exp = eq("firstName", "FirstA");
        List<Employee> results = repo.query(exp);
        assertEquals(1, results.size());
        Employee e2 = repo.get(id);
        repo.modify(e2);
        assertEquals(1, repo.size());
        Employee e3 = repo.get(id);
        repo.modify(e3);
        assertEquals(1, repo.size());
        List<Employee> results2 = repo.query(exp);
        assertEquals(1, results2.size());
    }

    @Test
    public void testQueryAfterUpdate2() {
        String id = "9131971";
        Repo<String, Employee> repo = Repos.builder().primaryKey("id").searchIndex("firstName").build(Typ.string, Employee.class);
        Employee e = Employee.employee("FirstA", "LastA", id, "5.29.1970:00:00:01", 100);
        repo.put(e);
        assertEquals(1, repo.size());
        // Find the new employee
        Criteria exp = eq("firstName", "FirstA");
        List<Employee> results = repo.query(exp);
        assertEquals(1, results.size());
        // Add returns true or false based on whether it was able to addObject
        // the object to the repo.
        Employee e2 = repo.get(id);
        assertEquals(false, repo.add(e2));
        try {
            Employee e3 = repo.get(id);
            repo.put(e3);
            die("you never get here");
        } catch (DataRepoException dre) {
            outputs("you tried to put something in the repo that is already there", dre.getMessage());
        }
        List<Employee> results2 = repo.query(exp);
        assertEquals(1, results2.size());
    }

    private void outputs(String s, String message) {
    }

    @Test
    public void testQueryAfterUpdate3() {
        String id = "9131971";
        Repo<String, Employee> repo = Repos.builder().primaryKey("id").searchIndex("firstName").build(Typ.string, Employee.class);
        Employee e = Employee.employee("FirstA", "LastA", id, "5.29.1970:00:00:01", 100);
        repo.put(e);
        assertEquals(1, repo.size());
        // Find the new employee
        Criteria exp = eq("firstName", "FirstA");
        List<Employee> results = repo.query(exp);
        assertEquals(1, results.size());
        // Add returns true or false based on whether it was able to addObject
        // the object to the repo.
        Employee e2 = repo.get(id);
        assertEquals(false, repo.add(e2));
        Employee e3 = repo.get(id);
        repo.update(e3);
        Employee e4 = Employee.employee("FirstA", "LastA", "9131971777" + System.currentTimeMillis(), "5.29.1970:00:00:01", 100);
        try {
            repo.update(e4);
            die("you never get here");
        } catch (DataRepoException dre) {
            outputs("you tried to update something but it does not exist", dre.getMessage());
        }
        List<Employee> results2 = repo.query(exp);
        assertEquals(1, results2.size());
    }

    @Test
    public void testQueryAfterUpdate4() {
        String id = "3212333222333";
        Repo<String, Employee> repo = Repos.builder().primaryKey("id").searchIndex("firstName").build(Typ.string, Employee.class);
        Employee e = Employee.employee("FirstA", "LastA", id, "5.29.1970:00:00:01", 100);
        repo.put(e);
        assertEquals(1, repo.size());
        // Find the new employee by first name
        Criteria exp = eq("firstName", "FirstA");
        List<Employee> results = repo.query(exp);
        assertEquals(1, results.size());
        // Change first name
        e.setFirstName("NewFirstName");
        repo.update(e);
        // We should not find any results when searching for the old first name
        List<Employee> results2 = repo.query(exp);
        assertEquals(1, results2.size());
        // We should find one result when searching for the new first name
        Criteria expNewFirstName = eq("firstName", "NewFirstName");
        List<Employee> results3 = repo.query(expNewFirstName);
        assertEquals(1, results3.size());
    }

    @Test
    public void testQueryAfterUpdateUseCloneEdits() {
        String id = "3212333222333";
        Repo<String, Employee> repo = Repos.builder().primaryKey("id").searchIndex("firstName").cloneEdits(//<--- Clone edits makes a clone
        true).build(Typ.string, Employee.class);
        Employee e = Employee.employee("FirstA", "LastA", id, "5.29.1970:00:00:01", 100);
        repo.put(e);
        assertEquals(1, repo.size());
        // Find the new employee by first name
        Criteria exp = eq("firstName", "FirstA");
        List<Employee> results = repo.query(exp);
        assertEquals(1, results.size());
        // Change first name
        e.setFirstName("NewFirstName");
        repo.update(e);
        // We should not find any results when searching for the old first name
        List<Employee> results2 = repo.query(exp);
        assertEquals(0, results2.size());
        // We should find one result when searching for the new first name
        Criteria expNewFirstName = eq("firstName", "NewFirstName");
        List<Employee> results3 = repo.query(expNewFirstName);
        assertEquals(1, results3.size());
    }

    @Test
    public void testQueryAfterUpdateUseUpdateMethod() {
        String id = "3212333222333";
        Repo<String, Employee> repo = Repos.builder().primaryKey("id").searchIndex("firstName").cloneEdits(//<--- No cloning
        false).build(Typ.string, Employee.class);
        Employee e = Employee.employee("FirstA", "LastA", id, "5.29.1970:00:00:01", 100);
        repo.put(e);
        assertEquals(1, repo.size());
        // Find the new employee by first name
        Criteria exp = eq("firstName", "FirstA");
        List<Employee> results = repo.query(exp);
        assertEquals(1, results.size());
        // Change first name
        repo.update(e.getId(), "firstName", "NewFirstName");
        // We should not find any results when searching for the old first name
        List<Employee> results2 = repo.query(exp);
        assertEquals(0, results2.size());
        // We should find one result when searching for the new first name
        Criteria expNewFirstName = eq("firstName", "NewFirstName");
        List<Employee> results3 = repo.query(expNewFirstName);
        assertEquals(1, results3.size());
    }
}
