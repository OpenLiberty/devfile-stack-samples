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

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import javax.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

/**
 * Determines the database server's availability to process application requests.
 */
@Liveness
@ApplicationScoped
public class DatabaseLivenessCheck implements HealthCheck {

    private void pingDatabaseServer(String serverName, int port) throws IOException {
        SocketAddress endpoint = new InetSocketAddress(serverName, port);
        Socket socket = new Socket();
        socket.connect(endpoint);
        socket.close();
    }

    @Override
    public HealthCheckResponse call() {
        String database = System.getenv("PGCLUSTER_DATABASE");
        String namespace = System.getenv("PGCLUSTER_NAMESPACE");
        String port = System.getenv("PGCLUSTER_PORT");
        String serverName = database + "." + namespace;

        try {
            pingDatabaseServer(serverName, Integer.valueOf(port).intValue());
            return HealthCheckResponse.named(this.getClass().getSimpleName()).up().build();
        } catch (Exception e) {
            return HealthCheckResponse.named(this.getClass().getSimpleName()).down()
                    .withData("message", e.getMessage()).build();
        }
    }
}