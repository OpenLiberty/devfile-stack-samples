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
package org.example.app.inventory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class InventoryManager {

    private Map<String, Properties> systems = Collections.synchronizedMap(new TreeMap<String, Properties>());

    public void addSystem(String hostname, Double systemLoad) {
        if (!systems.containsKey(hostname)) {
            Properties p = new Properties();
            p.put("hostname", hostname);
            p.put("systemLoad", systemLoad);
            systems.put(hostname, p);
        }
    }

    public void updateCpuStatus(String hostname, Double systemLoad) {
        Optional<Properties> p = getSystem(hostname);
        if (p.isPresent()) {
            if (p.get().getProperty(hostname) == null && hostname != null)
                p.get().put("systemLoad", systemLoad);
        }
    }

    public Optional<Properties> getSystem(String hostname) {
        Properties p = systems.get(hostname);
        return Optional.ofNullable(p);
    }

    public Map<String, Properties> getSystems() {
        return new TreeMap<>(systems);
    }

    public void resetSystems() {
        systems.clear();
    }
}