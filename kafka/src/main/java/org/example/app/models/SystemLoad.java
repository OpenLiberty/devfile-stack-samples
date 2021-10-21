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
package org.example.app.models;

import java.util.Objects;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;

public class SystemLoad {

    private static final Jsonb jsonb = JsonbBuilder.create();

    public String hostname;
    public Double loadAverage;

    public SystemLoad(String hostname, Double cpuLoadAvg) {
        this.hostname = hostname;
        this.loadAverage = cpuLoadAvg;
    }

    public SystemLoad() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SystemLoad))
            return false;
        SystemLoad sl = (SystemLoad) o;
        return Objects.equals(hostname, sl.hostname) && Objects.equals(loadAverage, sl.loadAverage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostname, loadAverage);
    }

    @Override
    public String toString() {
        return "CpuLoadAverage: " + jsonb.toJson(this);
    }

    public static class SystemLoadSerializer implements Serializer<Object> {
        @Override
        public byte[] serialize(String topic, Object data) {
            return jsonb.toJson(data).getBytes();
        }
    }

    public static class SystemLoadDeserializer implements Deserializer<SystemLoad> {
        @Override
        public SystemLoad deserialize(String topic, byte[] data) {
            if (data == null)
                return null;
            return jsonb.fromJson(new String(data), SystemLoad.class);
        }
    }
}