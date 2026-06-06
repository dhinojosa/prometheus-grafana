package com.evolutionnext.coffee.order;

import com.evolutionnext.coffee.CoffeeItem;

public record OrderRequest(CoffeeItem item, int quantity) {}
