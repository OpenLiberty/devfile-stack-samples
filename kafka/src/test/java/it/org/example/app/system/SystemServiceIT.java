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
package it.org.example.app.system;

import static org.junit.Assert.assertNotNull;

import java.time.Duration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.example.app.models.SystemLoad;
import org.junit.jupiter.api.Test;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.kafka.KafkaConsumerClient;

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
public class SystemServiceIT {

    static {
        System.setProperty("org.microshed.kafka.bootstrap.servers", System.getProperty("bootstrap.servers"));
    }

    @KafkaConsumerClient(
        valueDeserializer = org.example.app.models.SystemLoad.SystemLoadDeserializer.class, 
        groupId = "system-load-status", 
        topics = "systemLoadTopic", 
        properties = ConsumerConfig.AUTO_OFFSET_RESET_CONFIG + "=earliest")
    public static KafkaConsumer<String, SystemLoad> consumer;

    @Test
    public void testCpuStatus() {
        ConsumerRecords<String, SystemLoad> records = consumer.poll(Duration.ofMillis(30 * 1000));
        System.out.println("Polled " + records.count() + " records from Kafka:");

        for (ConsumerRecord<String, SystemLoad> record : records) {
            SystemLoad sl = record.value();
            System.out.println(sl);
            assertNotNull(sl.hostname);
            assertNotNull(sl.loadAverage);
        }
        consumer.commitAsync();
    }
}