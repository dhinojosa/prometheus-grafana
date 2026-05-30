package com.evolutionnext.coffee.inventory;

import com.evolutionnext.coffee.CoffeeItem;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class InventoryRepository implements PanacheRepository<InventoryItem> {

    public Optional<InventoryItem> findByItem(CoffeeItem item) {
        return find("item", item).firstResultOptional();
    }
}
