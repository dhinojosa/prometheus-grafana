package com.evolutionnext.coffee.order;

import com.evolutionnext.coffee.inventory.InventoryService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.time.LocalDateTime;

@ApplicationScoped
public class OrderService {

    @Inject
    OrderRepository orderRepository;

    @Inject
    InventoryService inventoryService;

    @Inject
    MeterRegistry meterRegistry;

    @Transactional
    public Order placeOrder(OrderRequest request) {
        inventoryService.deduct(request.item(), request.quantity());

        Order order = new Order();
        order.setItem(request.item());
        order.setQuantity(request.quantity());
        order.setStatus(OrderStatus.PENDING);
        order.setPlacedAt(LocalDateTime.now());

        orderRepository.persist(order);

        meterRegistry.counter("orders_placed_total", "item", request.item().name().toLowerCase()).increment();

        return order;
    }

    public Order getOrder(Long id) {
        return orderRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    @Transactional
    public Order completeOrder(Long id) {
        Order order = orderRepository.findByIdOptional(id)
                .orElseThrow(() -> new NotFoundException("Order not found: " + id));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new WebApplicationException("Order already completed: " + id, Response.Status.CONFLICT);
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        Duration fulfillmentTime = Duration.between(order.getPlacedAt(), order.getCompletedAt());
        Timer.builder("order_fulfillment_duration_seconds")
                .tag("item", order.getItem().name().toLowerCase())
                .register(meterRegistry)
                .record(fulfillmentTime);

        return order;
    }
}
