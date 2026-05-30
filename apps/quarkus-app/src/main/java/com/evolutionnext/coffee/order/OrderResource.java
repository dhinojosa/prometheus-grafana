package com.evolutionnext.coffee.order;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    OrderService orderService;

    @POST
    public Response placeOrder(OrderRequest request) {
        return Response.status(Response.Status.CREATED)
                .entity(orderService.placeOrder(request))
                .build();
    }

    @GET
    @Path("/{id}")
    public Order getOrder(@PathParam("id") Long id) {
        return orderService.getOrder(id);
    }

    @PATCH
    @Path("/{id}/complete")
    public Order completeOrder(@PathParam("id") Long id) {
        return orderService.completeOrder(id);
    }
}
