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
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.jersey.jaxb.ZStat;
import org.junit.Assert;
import org.junit.Test;
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
public class SetTest extends Base {
    protected static final Logger LOG = LoggerFactory.getLogger(SetTest.class);

    private String accept;
    private String path;
    private String encoding;
    private Response.Status expectedStatus;
    private ZStat expectedStat;
    private byte[] data;

    public static class MyWatcher implements Watcher {
        public void process(WatchedEvent event) {
            // FIXME ignore for now
        }
    }

    @Parameters
    public static Collection<Object[]> data() throws Exception {
        String baseZnode = Base.createBaseZNode();

        return Arrays.asList(new Object[][] {
          {MediaType.APPLICATION_JSON, baseZnode + "/s-t1", "utf8",
              Response.Status.OK,
              new ZStat(baseZnode + "/s-t1", null, null), null },
          {MediaType.APPLICATION_JSON, baseZnode + "/s-t2", "utf8",
              Response.Status.OK,
              new ZStat(baseZnode + "/s-t2", null, null), new byte[0] },
          {MediaType.APPLICATION_JSON, baseZnode + "/s-t3", "utf8",
              Response.Status.OK,
              new ZStat(baseZnode + "/s-t3", null, null), "foobar".getBytes() },
          {MediaType.APPLICATION_JSON, baseZnode + "/s-t4", "base64",
              Response.Status.OK,
              new ZStat(baseZnode + "/s-t4", null, null), null },
          {MediaType.APPLICATION_JSON, baseZnode + "/s-t5", "base64",
              Response.Status.OK,
              new ZStat(baseZnode + "/s-t5", null, null), new byte[0] },
          {MediaType.APPLICATION_JSON, baseZnode + "/s-t6", "base64",
              Response.Status.OK,
              new ZStat(baseZnode + "/s-t6", null, null),
              "foobar".getBytes() },
          {MediaType.APPLICATION_JSON, baseZnode + "/dkdkdkd", "utf8",
              Response.Status.NOT_FOUND, null, null },
          {MediaType.APPLICATION_JSON, baseZnode + "/dkdkdkd", "base64",
              Response.Status.NOT_FOUND, null, null },
          });
    }

    public SetTest(String accept, String path, String encoding,
            Response.Status status, ZStat expectedStat, byte[] data)
    {
        this.accept = accept;
        this.path = path;
        this.encoding = encoding;
        this.expectedStatus = status;
        this.expectedStat = expectedStat;
        this.data = data;
    }

    @Test
    public void testSet() throws Exception {
        if (expectedStat != null) {
            zk.create(expectedStat.path, "initial".getBytes(), Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT);
        }

        WebTarget target = znodesr.path(path).queryParam("dataformat", encoding);
        if (data == null) {
            target = target.queryParam("null", "true");
        }

        Invocation.Builder builder = target.request(accept)
                .header("Content-Type", MediaType.APPLICATION_OCTET_STREAM);

        Response response;
        if (data == null) {
            response = builder.put(null);
        } else {
            response = builder.put(Entity.entity(new String(data), MediaType.APPLICATION_OCTET_STREAM));
        }

        Assert.assertEquals(expectedStatus.getStatusCode(), response.getStatus());

        if (expectedStat == null) {
            return;
        }

        ZStat zstat = response.readEntity(ZStat.class);
        Assert.assertEquals(expectedStat, zstat);

        // use out-of-band method to verify
        byte[] actualData = zk.getData(zstat.path, false, new Stat());
        if (actualData == null && this.data == null) {
            return;
        } else if (actualData == null || this.data == null) {
            Assert.fail((actualData == null ? null : new String(actualData)) + " == "
                    + (this.data == null ? null : new String(this.data)));
        } else {
            Assert.assertTrue(new String(actualData) + " == " + new String(this.data),
                    Arrays.equals(actualData, this.data));
        }
    }
}
