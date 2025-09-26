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

package org.apache.zookeeper.server.jersey;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.server.jersey.cfg.Credentials;
import org.apache.zookeeper.server.jersey.cfg.Endpoint;
import org.apache.zookeeper.server.jersey.cfg.RestCfg;
import org.apache.zookeeper.server.jersey.filters.HTTPBasicAuth;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;

/**
 * Demonstration of how to run the REST service using Grizzly
 */
public class RestMain {

    private static final Logger LOG = LoggerFactory.getLogger(RestMain.class);

    private HttpServer server;
    private final RestCfg cfg;

    public RestMain(RestCfg cfg) {
        this.cfg = cfg;
    }

    public void start() throws IOException {
        System.out.println("Starting Grizzly2 ...");

        // base URI for the REST service
        URI baseUri = URI.create("http://0.0.0.0:" + cfg.getPort() + "/");

        // Register your resource packages
        ResourceConfig rc = new ResourceConfig()
                .packages("org.apache.zookeeper.server.jersey.resources");

        // Register your auth filter if needed
        rc.register(new HTTPBasicAuth(cfg.getCredentials()));

        server = GrizzlyHttpServerFactory.createHttpServer(baseUri, rc);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.shutdownNow();
        }
        ZooKeeperService.closeAll();
    }

    /**
     * The entry point for starting the server
     */
    public static void main(String[] args) throws Exception {
        RestCfg cfg = new RestCfg("rest.properties");

        final RestMain main = new RestMain(cfg);
        main.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            main.stop();
            System.out.println("Got exit request. Bye.");
        }));

        printEndpoints(cfg);
        System.out.println("Server started.");
    }

    private static void printEndpoints(RestCfg cfg) {
        int port = cfg.getPort();

        for (Endpoint e : cfg.getEndpoints()) {
            String context = e.getContext();
            if (!context.endsWith("/")) {
                context += "/";
            }

            System.out.printf(
                    "Started %s - WADL: http://localhost:%d%sapplication.wadl%n",
                    context, port, context);
        }
    }
}