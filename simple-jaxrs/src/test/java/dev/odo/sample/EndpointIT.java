/*******************************************************************************
Copyright (c) 2020 IBM Corporation and others

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*******************************************************************************/
package dev.odo.sample;

import static org.junit.jupiter.api.Assertions.*;

import dev.odo.starter.AppContainerConfig;
import org.junit.jupiter.api.Test;
import org.microshed.testing.SharedContainerConfig;
import org.microshed.testing.jaxrs.RESTClient;
import org.microshed.testing.jupiter.MicroShedTest;
import org.microshed.testing.testcontainers.ApplicationContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@MicroShedTest
@SharedContainerConfig(AppContainerConfig.class)
public class EndpointIT {

    Logger logger = LoggerFactory.getLogger(EndpointIT.class);
    
    @RESTClient
    public static StarterResource appService;
    
    @Test
    public void testAppResponse() {
        logger.info("In test method: testAppResponse");
        assertEquals("Hello! Welcome to Openliberty", appService.getRequest());
    }
}
