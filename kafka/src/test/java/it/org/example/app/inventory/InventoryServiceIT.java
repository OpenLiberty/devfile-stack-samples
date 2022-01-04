/*
 * Copyright (c) 2021 IBM Corporation and others
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
package it.org.example.app.inventory;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.example.app.inventory.InventoryResource;
import org.example.app.models.SystemLoad;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.kafka.KafkaProducerClient;

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
@TestMethodOrder(OrderAnnotation.class)
public class InventoryServiceIT {

    static {
        System.setProperty("org.microshed.kafka.bootstrap.servers", System.getProperty("bootstrap.servers"));
    }

    @RESTClient
    public static InventoryResource inventoryResource;

    @KafkaProducerClient(
        valueSerializer = org.example.app.models.SystemLoad.SystemLoadSerializer.class, 
        keySerializer = org.apache.kafka.common.serialization.StringSerializer.class)
    public static KafkaProducer<String, SystemLoad> producer;

    @AfterAll
    public static void cleanup() {
        inventoryResource.resetSystems();
    }

    @Test
    public void testCpuUsage() throws InterruptedException, UnknownHostException {
        // Publish a the system load as a base event.
        SystemLoad sl = new SystemLoad(InetAddress.getLocalHost().getHostName(), 1.1);
        producer.send(new ProducerRecord<String, SystemLoad>("systemLoadTopic", sl));
        Thread.sleep(5000);

        // Read the latest published system load and validate it. Iterate for uo to 30 seconds.
        List<Properties> systems= null;
        int status = 0;
        int retryCount = 1;
        while (retryCount <= 6) {
            Response response = inventoryResource.getSystems();
            status = response.getStatus();
            if (status != 200) {
                retryCount++;
                Thread.sleep(5000);
                continue;
            }

            systems = response.readEntity(new GenericType<List<Properties>>() {});

            if (systems.size() == 0) {
                retryCount++;
                Thread.sleep(5000);
                continue;
            } else {
                break;
            }
        }

        Assertions.assertEquals(200, status, "The response should be 200.");
        Assertions.assertEquals(1, systems.size(), "The response should have contained one system' data. It had zero.");

        for (Properties system : systems) {
            Assertions.assertEquals(sl.hostname, system.get("hostname"), "Hostname doesn't match!");
            BigDecimal systemLoad = (BigDecimal) system.get("systemLoad");
            Assertions.assertNotEquals(0.0, systemLoad.doubleValue(), "CPU load should not be 0!");
        }
    }
}
