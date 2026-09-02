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
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.server.jersey.cfg.Credentials;
import org.apache.zookeeper.server.jersey.cfg.Endpoint;
import org.apache.zookeeper.server.jersey.cfg.RestCfg;
import org.apache.zookeeper.server.jersey.filters.HTTPBasicAuth;

import java.net.URI;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
/**
 * Demonstration of how to run the REST service using Grizzly
 */
public class RestMain {

    private static Logger LOG = LoggerFactory.getLogger(RestMain.class);

    private HttpServer server;
    private RestCfg cfg;

    public RestMain(RestCfg cfg) {
        this.cfg = cfg;
    }

    public void start() throws IOException {
        if (cfg.useSSL()) {
            System.out.println("Starting Grizzly2 with SSL ...");

            SSLContextConfigurator sslConfig = new SSLContextConfigurator();
            sslConfig.setKeyStoreFile(cfg.getJKS("keys/rest.jks"));
            sslConfig.setKeyStorePass(cfg.getJKSPassword());

            URI baseUri = URI.create("https://0.0.0.0:" + cfg.getPort() + "/");

            ResourceConfig rc = new ResourceConfig()
                    .packages("org.apache.zookeeper.server.jersey.resources");

            rc.register(new HTTPBasicAuth(cfg.getCredentials()));

            // NOTE: Old GrizzlyWebServer required a doc root when registering multiple adapters.
            // With Grizzly2 and ResourceConfig, this limitation no longer applies.
            SSLEngineConfigurator sslEngineConfigurator =
                    new SSLEngineConfigurator(sslConfig.createSSLContext())
                            .setClientMode(false)
                            .setNeedClientAuth(false);

            server = GrizzlyHttpServerFactory.createHttpServer(
                    baseUri, rc, true, sslEngineConfigurator
            );
        } else {
            System.out.println("Starting Grizzly2 ...");

            URI baseUri = URI.create("http://0.0.0.0:" + cfg.getPort() + "/");

            ResourceConfig rc = new ResourceConfig()
                    .packages("org.apache.zookeeper.server.jersey.resources");

            rc.register(new HTTPBasicAuth(cfg.getCredentials()));

            // NOTE: Old GrizzlyWebServer required a doc root when registering multiple adapters.
            // With Grizzly2 and ResourceConfig, this limitation no longer applies.
            server = GrizzlyHttpServerFactory.createHttpServer(baseUri, rc);
        }

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
     *
     */
    public static void main(String[] args) throws Exception {
        RestCfg cfg = new RestCfg("rest.properties");

        final RestMain main = new RestMain(cfg);
        main.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                main.stop();
                System.out.println("Got exit request. Bye.");
            }
        });

        printEndpoints(cfg);
        System.out.println("Server started.");
    }

    private static void printEndpoints(RestCfg cfg) {
        int port = cfg.getPort();

        for (Endpoint e : cfg.getEndpoints()) {

            String context = e.getContext();
            if (context.charAt(context.length() - 1) != '/') {
                context += "/";
            }

            System.out.println(String.format(
                    "Started %s - WADL: http://localhost:%d%sapplication.wadl",
                    context, port, context));
        }
    }

}