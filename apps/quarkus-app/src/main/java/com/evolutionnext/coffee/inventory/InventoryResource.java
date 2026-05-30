package com.evolutionnext.coffee.inventory;

import com.evolutionnext.coffee.CoffeeItem;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class InventoryResource {

    @Inject
    InventoryService inventoryService;

    @GET
    public List<InventoryItem> list() {
        return inventoryService.getAll();
    }

    @POST
    @Path("/restock")
    public InventoryItem restock(RestockRequest request) {
        return inventoryService.restock(request);
    }

    @PATCH
    @Path("/{item}/deduct")
    public void deduct(@PathParam("item") CoffeeItem item, DeductRequest request) {
        inventoryService.deduct(item, request.quantity());
    }
}
