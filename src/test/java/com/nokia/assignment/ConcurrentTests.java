package com.nokia.assignment;

import com.nokia.assignment.model.service.Person;
import com.nokia.assignment.service.PersonService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConcurrentTests {

    @Autowired
    private PersonService personService;

    private ExecutorService executorService = Executors.newScheduledThreadPool(20);

    @Test
    public void addPersons_ConcurrentRequests() throws InterruptedException {
        initializeTest();
        // Given
        int numberOfPersons = 1000; // Each person will be submitted as a task
        Set<Callable<Boolean>> tasks = new HashSet<>();

        for (int i = 0; i < numberOfPersons; ++i) {
            String uniqueString = "person_" + i;
            com.nokia.assignment.model.view.Person person = ModelFactory.person(uniqueString, uniqueString);
            tasks.add(new AddPersonTask(person));
        }

        invokeAndCheckFuture(tasks);

        // Then get all persons
        ArrayList<Person> persons = personService.getAll();
        assertEquals(numberOfPersons, persons.size());
    }

    @Test
    public void addPersons_deletePersons_ConcurrentRequests() throws InterruptedException {
        initializeTest();
        // Given
        Set<Callable<Boolean>> tasks = new HashSet<>();

        for (int i = 0; i < 1000; ++i) {
            String uniqueString = "person_" + i;
            com.nokia.assignment.model.view.Person person = ModelFactory.person(uniqueString, uniqueString);
            tasks.add(new AddPersonTask(person));
            tasks.add(new DeletePersonTask(person));
        }

        invokeAndCheckFuture(tasks);
    }

    @Test
    public void addPersons_deletePersons_searchPersons_ConcurrentRequests() throws InterruptedException {
        initializeTest();
        // Given
        Set<Callable<Boolean>> tasks = new HashSet<>();

        for (int i = 0; i < 1000; ++i) {
            String uniqueString = "person_" + i;
            com.nokia.assignment.model.view.Person person = ModelFactory.person(uniqueString, uniqueString);
            tasks.add(new AddPersonTask(person));
            tasks.add(new DeletePersonTask(person));
            tasks.add(new SearchPerson(uniqueString));
        }
        invokeAndCheckFuture(tasks);
    }

    @Test
    public void deletePersons_ConcurrentRequests() throws InterruptedException {
        initializeTest();
        Set<Callable<Boolean>> tasks = new HashSet<>();

        // Given
        for (int i = 0; i < 1000; ++i) { // add 1000 persons & prepare delete tasks
            String uniqueString = "person_" + i;
            com.nokia.assignment.model.view.Person person = ModelFactory.person(uniqueString, uniqueString);
            personService.add(person.getId(), person.getName());
            tasks.add(new DeletePersonTask(person));
        }

        // Then
        invokeAndCheckFuture(tasks);
        ArrayList<Person> result = personService.getAll();
        assertEquals(0, result.size());
    }

    @Test
    public void searchPersons_ConcurrentRequests() throws InterruptedException {
        initializeTest();
        Set<Callable<Boolean>> tasks = new HashSet<>();

        // Given
        for (int i = 0; i < 1000; ++i) { // add 1000 persons
            String uniqueString = "person_" + i;
            personService.add(uniqueString, uniqueString);
            tasks.add(new SearchPerson(uniqueString));
        }
        invokeAndCheckFuture(tasks);
    }

    private void invokeAndCheckFuture(Set<Callable<Boolean>> tasks) throws InterruptedException {
        List<Future<Boolean>> futures = executorService.invokeAll(tasks);

        for (Future<Boolean> future : futures) {
            try {
                assertTrue(future.get()); // all tasks must return true
            } catch (Exception e) { // fail the test if one of the task throw exception like concurrentModificationException
                e.printStackTrace();
                assertTrue(false, "Exception thrown");
            }
        }
        executorService.shutdown();
    }

    private class AddPersonTask implements Callable {
        private com.nokia.assignment.model.view.Person person;

        public AddPersonTask(com.nokia.assignment.model.view.Person person) {
            this.person = person;
        }

        @Override
        public Boolean call() throws Exception {
            TimeUnit.MILLISECONDS.sleep(ModelFactory.sleep(10));
            return personService.add(person.getId(), person.getName());
        }
    }

    private class DeletePersonTask implements Callable {
        private com.nokia.assignment.model.view.Person person;

        public DeletePersonTask(com.nokia.assignment.model.view.Person person) {
            this.person = person;
        }

        @Override
        public Boolean call() throws Exception {
            TimeUnit.MILLISECONDS.sleep(ModelFactory.sleep(10));
            personService.deleteByName(person.getName());
            return Boolean.TRUE;
        }
    }

    private class SearchPerson implements Callable {
        private String name;

        public SearchPerson(String name) {
            this.name = name;
        }

        @Override
        public Boolean call() throws Exception {
            TimeUnit.MILLISECONDS.sleep(ModelFactory.sleep(10));
            personService.searchByName(name);
            return Boolean.TRUE;
        }
    }

    private void initializeTest() {
        personService.clearAll();
    }
}
