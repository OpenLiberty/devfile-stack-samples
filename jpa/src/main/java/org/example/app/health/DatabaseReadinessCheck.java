/*
 * Copyright (c) 2020 IBM Corporation and others
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
package org.example.app.health;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.sql.DataSource;
import org.eclipse.microprofile.health.Readiness;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 * Determines the database's readiness to process application requests.
 */
@Readiness
@ApplicationScoped
public class DatabaseReadinessCheck implements HealthCheck {

    @Resource
    private DataSource datasource;

    @Override
    public HealthCheckResponse call() {
        try {
            Connection connection = datasource.getConnection();
            DatabaseMetaData connData = connection.getMetaData();
            boolean valid = connection.isValid(1);
            return HealthCheckResponse.named(this.getClass().getSimpleName())
                        .withData("databaseProductName", connData.getDatabaseProductName())
                        .withData("databaseProductVersion", connData.getDatabaseProductVersion())
                        .withData("driverName", connData.getDriverName())
                        .withData("driverVersion", connData.getDriverVersion())
                        .withData("connectionStatus", (valid)? "Successfully validated" : "Failed to establish a valid connection to the database")
                        .state(valid)
                        .build();
        } catch (Exception e) {
            return HealthCheckResponse.named(this.getClass().getSimpleName()).down().withData("message", e.getMessage()).build();
        }
    }
}