package com.evolutionnext.coffee.inventory;

import com.evolutionnext.coffee.CoffeeItem;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    public List<InventoryItem> list() {
        return inventoryService.getAll();
    }

    @PostMapping("/restock")
    public InventoryItem restock(@RequestBody RestockRequest request) {
        return inventoryService.restock(request);
    }

    @PatchMapping("/{item}/deduct")
    public void deduct(@PathVariable CoffeeItem item, @RequestBody DeductRequest request) {
        inventoryService.deduct(item, request.quantity());
    }
}
