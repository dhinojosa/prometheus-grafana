package com.evolutionnext.coffee.inventory;

import com.evolutionnext.coffee.CoffeeItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, CoffeeItem> {}
