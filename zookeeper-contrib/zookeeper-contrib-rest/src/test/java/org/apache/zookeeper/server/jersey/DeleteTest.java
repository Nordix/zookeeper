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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


/**
 * Test stand-alone server.
 *
 */
@RunWith(Parameterized.class)
public class DeleteTest extends Base {
    protected static final Logger LOG = LoggerFactory.getLogger(DeleteTest.class);

    private String zpath;
    private Response.Status expectedStatus;

    public static class MyWatcher implements Watcher {
        public void process(WatchedEvent event) {
            // FIXME ignore for now
        }
    }

    @Parameters
    public static Collection<Object[]> data() throws Exception {
        String baseZnode = Base.createBaseZNode();

        return Arrays.asList(new Object[][] {
          {baseZnode, baseZnode, Response.Status.NO_CONTENT },
          {baseZnode, baseZnode, Response.Status.NO_CONTENT }
        });
    }

    public DeleteTest(String path, String zpath, Response.Status status) {
        this.zpath = zpath;
        this.expectedStatus = status;
    }

    public void verify(String type) throws Exception {
        if (expectedStatus != Response.Status.NOT_FOUND) {
            zpath = zk.create(zpath, null, Ids.OPEN_ACL_UNSAFE,
                    CreateMode.PERSISTENT_SEQUENTIAL);
        }

            Response response = znodesr.path(zpath)
                    .request(type)
                    .accept(type)
                    .delete();
            Assert.assertEquals(expectedStatus.getStatusCode(), response.getStatus());

            // use out-of-band method to verify
        Stat stat = zk.exists(zpath, false);
        Assert.assertNull(stat);
    }

    @Test
    public void testDelete() throws Exception {
        verify(MediaType.APPLICATION_OCTET_STREAM);
        verify(MediaType.APPLICATION_JSON);
        verify(MediaType.APPLICATION_XML);
    }
}
