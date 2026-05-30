package com.evolutionnext.coffee.inventory;

import com.evolutionnext.coffee.CoffeeItem;
import jakarta.persistence.*;

@Entity
@Table(name = "inventory")
public class InventoryItem {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private CoffeeItem item;

    private int quantity;

    public InventoryItem() {}

    public InventoryItem(CoffeeItem item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public CoffeeItem getItem() { return item; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
