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
package org.example.app.inventory.health;

import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.common.KafkaFuture;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
@ApplicationScoped
public class InventoryReadinessCheck implements HealthCheck {

    private static Logger logger = Logger.getLogger(InventoryReadinessCheck.class.getName());
    
    @Inject
    @ConfigProperty(name = "mp.messaging.connector.liberty-kafka.bootstrap.servers")
    String kafkaServer;

    @Inject
    @ConfigProperty(name = "mp.messaging.incoming.systemLoadIncoming.group.id")
    String groupId;

    @Override
    public HealthCheckResponse call() {
        boolean up = isReady();
        return HealthCheckResponse.named(this.getClass().getSimpleName()).state(up).build();
    }

    private boolean isReady() {
        AdminClient adminClient = createAdminClient();
        return checkIfBarConsumerGroupRegistered(adminClient);
    }

    private AdminClient createAdminClient() {
        Properties connectionProperties = new Properties();
        connectionProperties.put("bootstrap.servers", kafkaServer);
        AdminClient adminClient = AdminClient.create(connectionProperties);
        return adminClient;
    }

    private boolean checkIfBarConsumerGroupRegistered(AdminClient adminClient) {
        ListConsumerGroupsResult groupsResult = adminClient.listConsumerGroups();
        KafkaFuture<Collection<ConsumerGroupListing>> consumerGroupsFuture = groupsResult.valid();
        try {
            Collection<ConsumerGroupListing> consumerGroups = consumerGroupsFuture.get();
            for (ConsumerGroupListing g : consumerGroups)
                logger.info("groupId: " + g.groupId());
            return consumerGroups.stream().anyMatch(group -> group.groupId().equals(groupId));
        } catch (Exception e) {
            return false;
        }
    }
}
