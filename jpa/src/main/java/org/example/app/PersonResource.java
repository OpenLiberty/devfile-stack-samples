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
package org.example.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.UserTransaction;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/")
@RequestScoped
@Named
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PersonResource {

    Collection<Person> people;

    @PersistenceContext(unitName = "jpa-unit")
    EntityManager em;

    @Resource
    UserTransaction userTran;

    @PostConstruct
    public void init() {
        people = getAllPeople();
    }

    @GET
    public Collection<Person> getAllPeople() {
        Collection<Person> people = new ArrayList<>();
        try {
            people = em.createNamedQuery("Person.findAll", Person.class).getResultList();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return people;
    }

    @GET
    @Path("/{personId}")
    public Person getPerson(@PathParam("personId") long id) {
        Person person = (Person) em.find(Person.class, id);
        return person;
    }

    @POST
    public Long createPerson(@QueryParam("name") @NotEmpty @Size(min = 2, max = 50) String name,
            @QueryParam("age") @PositiveOrZero int age) {
        try {
            Person p = new Person(name, age);
            userTran.begin();
            em.persist(p);
            userTran.commit();
            return p.id;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @PUT
    @Path("/{personId}")
    public String updatePerson(@PathParam("personId") long id, @Valid Person p) {
        try {
            userTran.begin();
            Person person = em.find(Person.class, p.getId());
            person.setName(p.name);
            person.setAge(p.age);
            em.merge(person);
            userTran.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "/PersonList.xhtml?faces-redirect=true";
    }

    @DELETE
    @Path("/{personId}")
    public String removePerson(@PathParam("personId") long id) {
        Person deletePerson = em.find(Person.class, id);
        try {
            userTran.begin();
            em.remove(em.merge(deletePerson));
            userTran.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "/PersonList.xhtml?faces-redirect=true";
    }

    public Collection<Person> getPeople() {
        return people;
    }

    public String createPersonXHTML(String name,int age) {
        try {
            Person p = new Person(name, age);
            userTran.begin();
            em.persist(p);
            userTran.commit();
            return "PersonList.xhtml?faces-redirect=true";
        } catch (Exception e) {
            e.printStackTrace();
            return "CreatePerson.xhtml?faces-redirect=true";
        }
    }

    public String editPersonRecordInDB(long personId) {
        Person editRecord = null;
        System.out.println("editPersonRecordInDB() : Person Id: " + personId);

        /* Setting The Particular Person Details In Session */
        Map<String, Object> sessionMapObj = FacesContext.getCurrentInstance().getExternalContext().getSessionMap();

        try {
            editRecord = em.find(Person.class, personId);
            sessionMapObj.put("editRecordObj", editRecord);
        } catch (Exception sqlException) {
            sqlException.printStackTrace();
        }
        return "/EditPerson.xhtml?faces-redirect=true";
    }
}
