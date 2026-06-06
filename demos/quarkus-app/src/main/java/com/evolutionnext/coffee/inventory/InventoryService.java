package com.evolutionnext.coffee.inventory;

import com.evolutionnext.coffee.CoffeeItem;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
public class InventoryService {

    @Inject
    InventoryRepository inventoryRepository;

    @Inject
    MeterRegistry meterRegistry;

    private final Map<CoffeeItem, AtomicInteger> stockGauges = new ConcurrentHashMap<>();

    @PostConstruct
    public void initGauges() {
        inventoryRepository.listAll().forEach(item -> {
            AtomicInteger ref = registerGauge(item.getItem());
            ref.set(item.getQuantity());
        });
    }

    private AtomicInteger registerGauge(CoffeeItem item) {
        return stockGauges.computeIfAbsent(item, k -> {
            AtomicInteger ref = new AtomicInteger(0);
            Gauge.builder("inventory_stock_level", ref, AtomicInteger::get)
                    .description("Current stock level by coffee item")
                    .tag("item", item.name().toLowerCase())
                    .register(meterRegistry);
            return ref;
        });
    }

    @Transactional
    public InventoryItem restock(RestockRequest request) {
        InventoryItem item = inventoryRepository.findByItem(request.item())
                .orElse(new InventoryItem(request.item(), 0));
        item.setQuantity(item.getQuantity() + request.quantity());
        inventoryRepository.persist(item);
        registerGauge(request.item()).set(item.getQuantity());
        return item;
    }

    @Transactional
    public void deduct(CoffeeItem coffeeItem, int quantity) {
        InventoryItem item = inventoryRepository.findByItem(coffeeItem)
                .orElseThrow(() -> new WebApplicationException("No inventory for " + coffeeItem, Response.Status.CONFLICT));
        if (item.getQuantity() < quantity) {
            throw new WebApplicationException("Insufficient inventory for " + coffeeItem, Response.Status.CONFLICT);
        }
        item.setQuantity(item.getQuantity() - quantity);
        registerGauge(coffeeItem).set(item.getQuantity());
    }

    public List<InventoryItem> getAll() {
        return inventoryRepository.listAll();
    }
}
