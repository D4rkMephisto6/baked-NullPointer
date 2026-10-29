package pe.upeu.nullpointerpos.menu;

import java.math.BigDecimal;

public record ProductResponse(Long productId, String sku, String name, String description,
                              String imageUrl, BigDecimal unitPrice, String categoryName,
                              String kitchenStation, boolean available) { }
