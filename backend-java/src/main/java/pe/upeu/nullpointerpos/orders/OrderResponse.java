package pe.upeu.nullpointerpos.orders;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record OrderResponse(Long orderId, String orderCode, Long tableId, String tableCode,
                            Long waiterUserId, String waiterEmployeeCode, String status,
                            String notes, BigDecimal subtotal, BigDecimal totalAmount,
                            OffsetDateTime createdAt, OffsetDateTime sentAt,
                            List<OrderItemResponse> items) { }
