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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.example.app.Person;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DatabaseIT {

	private static Jsonb jsonb;
	private static Client client;

	@BeforeAll
	public static void init() {
		client = ClientBuilder.newClient();
		jsonb = JsonbBuilder.newBuilder().build();
	}

	@AfterAll
	public static void destroy() throws Exception {
		jsonb.close();
		client.close();
	}

	public WebTarget getTarget(String path) {
		String port = System.getProperty("http.port");
		String context = System.getProperty("context.root");
		String url = "http://localhost:" + port + "/" + context + "/";
		return client.target(url + path);
	}

	@Test
	public void testGetPerson() {
		// Add a person to the database.
		Person bob = new Person("Bob", 24);
		WebTarget target = getTarget("").queryParam("name", bob.getName()).queryParam("age", bob.getAge());
		Response postResponse = target.request().post(null);
		Long personId;
		try {
			assertEquals(200, postResponse.getStatus(), "Request received an invalid status response. Details: " + postResponse.getStatusInfo().getReasonPhrase());
			personId = jsonb.fromJson(postResponse.readEntity(InputStream.class), Long.class);
			bob.setId(personId);
		} finally {
			postResponse.close();
		}

		// Retrieve the database entry.
		target = getTarget(personId.toString());
		Response getResponse = target.request().get();
		try {
			assertEquals(200, getResponse.getStatus(), "Request received an invalid status response. Details: " + getResponse.getStatusInfo().getReasonPhrase());
			Person person = jsonb.fromJson(getResponse.readEntity(InputStream.class), Person.class);
			assertEquals(bob, person);
		} finally {
			getResponse.close();
		}

		// Remove the datase entry.
		target = getTarget(personId.toString());
		Response delResponse = target.request().delete();
		try {
			assertEquals(200, delResponse.getStatus(), "Request received an invalid status response. Details: " + delResponse.getStatusInfo().getReasonPhrase());
		} finally {
			delResponse.close();
		}
	}

	@Test
	public void testGetAllPeople() {
		// Add two people to the database.
		Person mary = new Person("Mary", 1);
		WebTarget target = getTarget("").queryParam("name", mary.getName()).queryParam("age", mary.getAge());
		Response post1Response = target.request().post(null);

		try {
			assertEquals(200, post1Response.getStatus(), "Request received an invalid status response. Details: " + post1Response.getStatusInfo().getReasonPhrase());
			Long maryId = jsonb.fromJson(post1Response.readEntity(InputStream.class), Long.class);
			mary.setId(maryId);
		} finally {
			post1Response.close();
		}

		Person james = new Person("James", 2);
		target = getTarget("").queryParam("name", james.getName()).queryParam("age", james.getAge());
		Response post2Response = target.request().post(null);
		try {
			assertEquals(200, post2Response.getStatus(), "Request received an invalid status response. Details: " + post2Response.getStatusInfo().getReasonPhrase());
			Long jamesId = jsonb.fromJson(post2Response.readEntity(InputStream.class), Long.class);
			james.setId(jamesId);
		} finally {
			post2Response.close();
		}

		// Retrieve the database entries.
		Response getResponse = target.request().get();
		try {
			assertEquals(200, getResponse.getStatus(), "Request received an invalid status response. Details: " + getResponse.getStatusInfo().getReasonPhrase());
			Collection<Person> people = jsonb.fromJson(getResponse.readEntity(InputStream.class),new ArrayList<Person>() {}.getClass().getGenericSuperclass());

			assertTrue(people.size() >= 2, "Expected at least 2 people to be registered, but there were only: " + people.size());
			assertTrue(people.contains(mary));
			assertTrue(people.contains(james));
		} finally {
			getResponse.close();
		}

		// Remove the datase entries.
		target = getTarget(String.valueOf(mary.getId()));
		Response del1Response = target.request().delete();
		try {
			assertEquals(200, del1Response.getStatus(), "Request received an invalid status response. Details: " + del1Response.getStatusInfo().getReasonPhrase());
		} finally {
			del1Response.close();
		}

		target = getTarget(String.valueOf(james.getId()));
		Response del2response = target.request().delete();
		try {
			assertEquals(200, del2response.getStatus(), "Request received an invalid status response. Details: " + del2response.getStatusInfo().getReasonPhrase());
		} finally {
			del2response.close();
		}
	}
}
