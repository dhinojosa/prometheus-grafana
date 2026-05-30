package com.evolutionnext.coffee.inventory;

import com.evolutionnext.coffee.CoffeeItem;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final MeterRegistry meterRegistry;
    private final Map<CoffeeItem, AtomicInteger> stockGauges = new ConcurrentHashMap<>();

    public InventoryService(InventoryRepository inventoryRepository, MeterRegistry meterRegistry) {
        this.inventoryRepository = inventoryRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initGauges() {
        inventoryRepository.findAll().forEach(item -> {
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
        InventoryItem inventoryItem = inventoryRepository.findById(request.item())
                .orElse(new InventoryItem(request.item(), 0));
        inventoryItem.setQuantity(inventoryItem.getQuantity() + request.quantity());
        InventoryItem saved = inventoryRepository.save(inventoryItem);
        registerGauge(request.item()).set(saved.getQuantity());
        return saved;
    }

    @Transactional
    public void deduct(CoffeeItem item, int quantity) {
        InventoryItem inventoryItem = inventoryRepository.findById(item)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No inventory for " + item));
        if (inventoryItem.getQuantity() < quantity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient inventory for " + item);
        }
        inventoryItem.setQuantity(inventoryItem.getQuantity() - quantity);
        InventoryItem saved = inventoryRepository.save(inventoryItem);
        registerGauge(item).set(saved.getQuantity());
    }

    public List<InventoryItem> getAll() {
        return inventoryRepository.findAll();
    }
}
