package app.krista.extensions.essentials.cloudfiles.sharepoint.api;

import app.krista.extension.executor.Invoker;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("listeners")
public class ListenerResource {

    private final Invoker invoker;

    @Inject
    public ListenerResource(@Named("self") Invoker invoker) {
        this.invoker = invoker;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListeners() {
        return Response.ok().entity(invoker.listEventListeners()).build();
    }

    @DELETE
    @Path("clear")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearListeners() {
        invoker.listEventListeners().forEach(l -> invoker.unregisterEventListener(l.getListenerId()));
        return Response.ok("Success").build();
    }

}
