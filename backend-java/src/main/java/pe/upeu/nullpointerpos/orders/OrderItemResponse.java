package pe.upeu.nullpointerpos.orders;

import java.math.BigDecimal;

public record OrderItemResponse(Long orderItemId, Long productId, String productName,
                                Integer quantity, BigDecimal unitPrice, String notes,
                                BigDecimal lineTotal, String status) { }
