package com.evolutionnext.coffee.inventory;

import com.evolutionnext.coffee.CoffeeItem;

public record RestockRequest(CoffeeItem item, int quantity) {}
