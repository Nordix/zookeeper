/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server.jersey.resources;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

import org.apache.zookeeper.server.jersey.jaxb.ZChildrenJSON;
import org.apache.zookeeper.server.jersey.jaxb.ZPath;
import org.apache.zookeeper.server.jersey.jaxb.ZStat;


/**
 * Tell Jersey how to resolve JSON formatting. Specifically detail the
 * fields which are arrays and which are numbers (not strings).
 */
@Provider
public final class JAXBContextResolver implements ContextResolver<JAXBContext> {
    private final JAXBContext context;

    private final Set<Class<?>> typesSet = new HashSet<>(Arrays.asList(
            ZPath.class, ZStat.class, ZChildrenJSON.class
    ));

    public JAXBContextResolver() {
        try {
            this.context = JAXBContext.newInstance(
                    ZPath.class,
                    ZStat.class,
                    ZChildrenJSON.class
            );
        } catch (JAXBException e) {
            throw new RuntimeException("Failed to create JAXBContext", e);
        }
    }

    @Override
    public JAXBContext getContext(Class<?> objectType) {
        return (typesSet.contains(objectType)) ? context : null;
    }
}