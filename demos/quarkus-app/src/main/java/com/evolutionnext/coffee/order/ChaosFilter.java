package com.evolutionnext.coffee.order;

import jakarta.annotation.Priority;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Injects synthetic 500 errors on a configurable fraction of POST /orders
 * requests, driven by the CHAOS_ERROR_RATE env var. Used to demo Argo
 * Rollouts canary analysis aborting on a Prometheus error-rate query.
 */
@Provider
@Priority(Priorities.USER)
public class ChaosFilter implements ContainerRequestFilter {

    @ConfigProperty(name = "chaos.error-rate", defaultValue = "0.0")
    double errorRate;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        if (errorRate <= 0.0) {
            return;
        }

        boolean isOrderPlacement = HttpMethod.POST.equals(requestContext.getMethod())
                && requestContext.getUriInfo().getPath().equals("/orders");

        if (isOrderPlacement && ThreadLocalRandom.current().nextDouble() < errorRate) {
            requestContext.abortWith(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity("Chaos: simulated failure")
                            .build());
        }
    }
}
