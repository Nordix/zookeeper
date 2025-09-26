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

import java.util.Arrays;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.jersey.jaxb.ZPath;
import org.junit.Test;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


/**
 * Test stand-alone server.
 *
 */
@RunWith(Parameterized.class)
public class CreateTest extends Base {
    protected static final Logger LOG = LoggerFactory.getLogger(CreateTest.class);

    private String accept;
    private String path;
    private String name;
    private String encoding;
    private Response.Status expectedStatus;
    private ZPath expectedPath;
    private byte[] data;
    private boolean sequence;

    public static class MyWatcher implements Watcher {
        public void process(WatchedEvent event) {
            // ignore for now
        }
    }

    @Parameters
    public static Collection<Object[]> data() throws Exception {
        String baseZnode = Base.createBaseZNode();

        return Arrays.asList(new Object[][] {
                {MediaType.APPLICATION_JSON, baseZnode, "foo bar", "utf8",
                        Response.Status.CREATED, new ZPath(baseZnode + "/foo bar"), null, false },
                {MediaType.APPLICATION_JSON, baseZnode, "c-t1", "utf8",
                        Response.Status.CREATED, new ZPath(baseZnode + "/c-t1"), null, false },
                {MediaType.APPLICATION_JSON, baseZnode, "c-t1", "utf8",
                        Response.Status.CONFLICT, null, null, false },
                {MediaType.APPLICATION_JSON, baseZnode, "c-t2", "utf8",
                        Response.Status.CREATED, new ZPath(baseZnode + "/c-t2"), "".getBytes(), false },
                {MediaType.APPLICATION_JSON, baseZnode, "c-t2", "utf8",
                        Response.Status.CONFLICT, null, null, false },
                {MediaType.APPLICATION_JSON, baseZnode, "c-t3", "utf8",
                        Response.Status.CREATED, new ZPath(baseZnode + "/c-t3"), "foo".getBytes(), false },
                {MediaType.APPLICATION_JSON, baseZnode, "c-t3", "utf8",
                        Response.Status.CONFLICT, null, null, false },
                {MediaType.APPLICATION_JSON, baseZnode, "c-t4", "base64",
                        Response.Status.CREATED, new ZPath(baseZnode + "/c-t4"), "foo".getBytes(), false },
                {MediaType.APPLICATION_JSON, baseZnode, "c-", "utf8",
                        Response.Status.CREATED, new ZPath(baseZnode + "/c-"), null, true },
                {MediaType.APPLICATION_JSON, baseZnode, "c-", "utf8",
                        Response.Status.CREATED, new ZPath(baseZnode + "/c-"), null, true }
        });
    }

    public CreateTest(String accept, String path, String name, String encoding,
                      Response.Status status, ZPath expectedPath, byte[] data,
                      boolean sequence)
    {
        this.accept = accept;
        this.path = path;
        this.name = name;
        this.encoding = encoding;
        this.expectedStatus = status;
        this.expectedPath = expectedPath;
        this.data = data;
        this.sequence = sequence;
    }

    @Test
    public void testCreate() throws Exception {
        WebTarget target = znodesr.path(path)
                .queryParam("dataformat", encoding)
                .queryParam("name", name);

        if (data == null) {
            target = target.queryParam("null", "true");
        }
        if (sequence) {
            target = target.queryParam("sequence", "true");
        }

        Invocation.Builder builder = target.request(accept);

        Response response;
        if (data == null) {
            response = builder.post(null);
        } else {
            response = builder.post(Entity.entity(data, MediaType.APPLICATION_OCTET_STREAM));
        }

        Assert.assertEquals(expectedStatus.getStatusCode(), response.getStatus());

        if (expectedPath == null) {
            return;
        }

        ZPath zpath = response.readEntity(ZPath.class);
        if (sequence) {
            Assert.assertTrue(zpath.path.startsWith(expectedPath.path));
            Assert.assertTrue(zpath.uri.startsWith(znodesr.path(path).toString()));
        } else {
            Assert.assertEquals(expectedPath, zpath);
            Assert.assertEquals(znodesr.path(path).toString(), zpath.uri);
        }

        // verify data
        byte[] actualData = zk.getData(zpath.path, false, new Stat());
        if (actualData == null && this.data == null) {
            return;
        } else if (actualData == null || this.data == null) {
            Assert.assertArrayEquals(this.data, actualData);
        } else {
            Assert.assertTrue(Arrays.equals(actualData, this.data));
        }
    }
}
