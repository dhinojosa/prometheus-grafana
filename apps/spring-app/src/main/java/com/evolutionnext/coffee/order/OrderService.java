package com.evolutionnext.coffee.order;

import com.evolutionnext.coffee.inventory.InventoryService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final MeterRegistry meterRegistry;

    public OrderService(OrderRepository orderRepository, InventoryService inventoryService, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.meterRegistry = meterRegistry;
    }

    @Transactional
    public Order placeOrder(OrderRequest request) {
        inventoryService.deduct(request.item(), request.quantity());

        Order order = new Order();
        order.setItem(request.item());
        order.setQuantity(request.quantity());
        order.setStatus(OrderStatus.PENDING);
        order.setPlacedAt(LocalDateTime.now());

        Order saved = orderRepository.save(order);

        meterRegistry.counter("orders_placed_total", "item", request.item().name().toLowerCase()).increment();

        return saved;
    }

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));
    }

    @Transactional
    public Order completeOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found: " + id));

        if (order.getStatus() == OrderStatus.COMPLETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already completed: " + id);
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        Duration fulfillmentTime = Duration.between(order.getPlacedAt(), order.getCompletedAt());
        Timer.builder("order_fulfillment_duration_seconds")
                .tag("item", order.getItem().name().toLowerCase())
                .register(meterRegistry)
                .record(fulfillmentTime);

        return orderRepository.save(order);
    }
}
