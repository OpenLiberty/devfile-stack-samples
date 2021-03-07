/*
 * Copyright (c) 2019, 2021 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.example.app.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.example.app.Person;
import org.example.app.PersonResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseIT {
	private static Logger log = LoggerFactory.getLogger(DatabaseIT.class);
	private static PersonResource personSvc;

	@BeforeAll
	public static void init() throws Exception {
		URI uri = UriBuilder.fromUri("http://localhost:"+System.getProperty("http.port")).path(System.getProperty("app.path")).build();
		log.info("URL: " + uri);
		personSvc = JAXRSClientFactory.create(uri.toString(), PersonResource.class, Collections.singletonList(JacksonJsonProvider.class));
	}

	@Test
	public void testGetPerson() {
		Person bob = new Person("Bob", 24);
		Long bobId = personSvc.createPerson("Bob", 24);
		bob.setId(bobId);

		Person person = personSvc.getPerson(bobId);
		assertEquals(bob, person);

		personSvc.removePerson(bobId);
	}

	@Test
	public void testGetAllPeople() {
		Person mary = new Person("Mary", 1);
		Long maryId = personSvc.createPerson(mary.getName(), mary.getAge());
		mary.setId(maryId);
		Person james = new Person("James", 2);
		Long jamesId = personSvc.createPerson(james.getName(), james.getAge());
		james.setId(jamesId);

		Collection<Person> people = personSvc.getAllPeople();
		assertTrue(people.size() >= 2, "Expected at least 2 people to be registered, but there were only: " + people.size());
		assertTrue(people.contains(mary));
		assertTrue(people.contains(james));

		personSvc.removePerson(maryId);
		personSvc.removePerson(jamesId);
	}
}
