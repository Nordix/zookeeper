package org.apache.zookeeper.server.jersey.resources;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.zookeeper.server.jersey.ZooKeeperService;
import org.apache.zookeeper.server.jersey.jaxb.ZError;
import org.apache.zookeeper.server.jersey.jaxb.ZSession;

@Path("sessions/v1/{session: .*}")
public class SessionsResource {

    private static final Logger LOG = LoggerFactory.getLogger(SessionsResource.class);

    private final String contextPath;

    public SessionsResource(@Context HttpServletRequest request) {
        String cp = request.getContextPath();
        this.contextPath = (cp == null || cp.isEmpty()) ? "/" : cp;
    }

    @PUT
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    public Response keepAliveSession(@PathParam("session") String session,
                                     @Context UriInfo ui,
                                     byte[] data) {

        if (!ZooKeeperService.isConnected(contextPath, session)) {
            throwNotFound(session, ui);
        }

        ZooKeeperService.resetTimer(contextPath, session);
        return Response.ok().build();
    }

    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public Response createSession(@QueryParam("op") String op,
                                  @DefaultValue("5") @QueryParam("expire") String expire,
                                  @Context UriInfo ui) {
        if (!"create".equals(op)) {
            throw new WebApplicationException(
                    Response.status(Response.Status.BAD_REQUEST)
                            .entity(new ZError(ui.getRequestUri().toString(), "Invalid op"))
                            .build());
        }

        int expireInSeconds;
        try {
            expireInSeconds = Integer.parseInt(expire);
        } catch (NumberFormatException e) {
            throw new WebApplicationException(Response.status(
                    Response.Status.BAD_REQUEST).build());
        }

        String uuid = UUID.randomUUID().toString();
        while (ZooKeeperService.isConnected(contextPath, uuid)) {
            uuid = UUID.randomUUID().toString();
        }

        try {
            ZooKeeperService.getClient(contextPath, uuid, expireInSeconds);
        } catch (IOException e) {
            LOG.error("Failed while trying to create a new session", e);
            throw new WebApplicationException(Response.status(
                    Response.Status.INTERNAL_SERVER_ERROR).build());
        }

        URI uri = ui.getAbsolutePathBuilder().path(uuid).build();
        return Response.created(uri)
                .entity(new ZSession(uuid, uri.toString()))
                .build();
    }

    @DELETE
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
    public void deleteSession(@PathParam("session") String session,
                              @Context UriInfo ui) {
        ZooKeeperService.close(contextPath, session);
    }

    private static void throwNotFound(String session, UriInfo ui)
            throws WebApplicationException {
        throw new WebApplicationException(Response.status(
                Response.Status.NOT_FOUND).entity(
                new ZError(ui.getRequestUri().toString(),
                        session + " not found")).build());
    }
}
