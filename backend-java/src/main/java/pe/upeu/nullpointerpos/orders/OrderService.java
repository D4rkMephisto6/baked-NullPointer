package pe.upeu.nullpointerpos.orders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public OrderService(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request, String employeeCode) {
        Long waiterId = findUserId(employeeCode);
        TableInfo table = lockTable(request.tableId());
        if (!"LIBRE".equals(table.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La mesa " + table.tableCode() + " no está libre.");
        }
        String orderCode = "NP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Long orderId = jdbc.queryForObject(
                "INSERT INTO pos_schema.orders(order_code, table_id, waiter_user_id, status, notes, subtotal, total_amount) " +
                        "VALUES (?, ?, ?, 'BORRADOR', ?, 0, 0) RETURNING order_id",
                Long.class, orderCode, table.tableId(), waiterId, request.notes());
        replaceItems(orderId, request.items());
        recalculateTotals(orderId);
        jdbc.update("INSERT INTO pos_schema.audit_logs(actor_user_id, action, entity_name, entity_id, reason) " +
                        "VALUES (?, 'ORDER_CREATED', 'ORDER', ?, 'Comanda en borrador desde Sprint 1')",
                waiterId, orderId.toString());
        return loadOrder(orderId);
    }

    @Transactional
    public OrderResponse updateItems(Long orderId, ModifyOrderItemsRequest request,
                                     String employeeCode, boolean administrator) {
        OrderMeta meta = lockOrder(orderId);
        assertOwner(meta, employeeCode, administrator);
        if (!"BORRADOR".equals(meta.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede editar una comanda en estado BORRADOR.");
        }
        jdbc.update("DELETE FROM pos_schema.order_items WHERE order_id = ?", orderId);
        replaceItems(orderId, request.items());
        recalculateTotals(orderId);
        jdbc.update("UPDATE pos_schema.orders SET updated_at = NOW() WHERE order_id = ?", orderId);
        jdbc.update("INSERT INTO pos_schema.audit_logs(actor_user_id, action, entity_name, entity_id, reason) " +
                        "VALUES (?, 'ORDER_ITEMS_UPDATED', 'ORDER', ?, 'Edición de platos antes de enviar a cocina')",
                findUserId(employeeCode), orderId.toString());
        return loadOrder(orderId);
    }

    @Transactional
    public TableTransferResponse transfer(TransferTableRequest request, String employeeCode,
                                          boolean administrator) {
        OrderMeta order = lockOrder(request.orderId());
        assertOwner(order, employeeCode, administrator);
        if (List.of("PAGADO", "ANULADO").contains(order.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede mover una comanda pagada o anulada.");
        }
        if (order.tableId().equals(request.targetTableId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La comanda ya pertenece a esa mesa.");
        }
        TableInfo destination = lockTable(request.targetTableId());
        if (!"LIBRE".equals(destination.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La mesa destino debe estar libre.");
        }
        String sourceCode = tableCode(order.tableId());
        jdbc.update("UPDATE pos_schema.orders SET table_id = ?, updated_at = NOW() WHERE order_id = ?",
                destination.tableId(), request.orderId());
        Long actorId = findUserId(employeeCode);
        jdbc.update("INSERT INTO pos_schema.table_transfers(order_id, from_table_id, to_table_id, transferred_by_user_id, reason) " +
                        "VALUES (?, ?, ?, ?, ?)", request.orderId(), order.tableId(), destination.tableId(), actorId, request.reason());
        jdbc.update("INSERT INTO pos_schema.audit_logs(actor_user_id, action, entity_name, entity_id, reason) " +
                        "VALUES (?, 'TABLE_TRANSFERRED', 'ORDER', ?, ?)", actorId, request.orderId().toString(), request.reason());
        OffsetDateTime transferredAt = jdbc.queryForObject("SELECT transferred_at FROM pos_schema.table_transfers " +
                "WHERE order_id = ? ORDER BY transferred_at DESC LIMIT 1", (rs, row) -> rs.getObject(1, OffsetDateTime.class), request.orderId());
        return new TableTransferResponse(request.orderId(), sourceCode, destination.tableCode(), order.status(), transferredAt);
    }

    @Transactional
    public OrderResponse sendToKitchen(Long orderId, String employeeCode, boolean administrator) {
        OrderMeta meta = lockOrder(orderId);
        assertOwner(meta, employeeCode, administrator);
        if (!"BORRADOR".equals(meta.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede enviar a cocina una comanda en BORRADOR.");
        }
        Integer itemCount = jdbc.queryForObject("SELECT COUNT(*) FROM pos_schema.order_items WHERE order_id = ?", Integer.class, orderId);
        if (itemCount == null || itemCount == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Agrega al menos un plato antes de enviar a cocina.");
        }
        jdbc.update("UPDATE pos_schema.orders SET status = 'ENVIADO_COCINA', sent_at = NOW(), updated_at = NOW() WHERE order_id = ?", orderId);
        OrderResponse response = loadOrder(orderId);
        try {
            String payload = objectMapper.writeValueAsString(response);
            jdbc.update("INSERT INTO pos_schema.outbox_events(aggregate_type, aggregate_id, event_type, payload) " +
                    "VALUES ('ORDER', ?, 'ORDER_SENT_TO_KITCHEN', CAST(? AS jsonb))", orderId, payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el evento de cocina.", e);
        }
        jdbc.update("INSERT INTO pos_schema.audit_logs(actor_user_id, action, entity_name, entity_id, reason) " +
                        "VALUES (?, 'ORDER_SENT_TO_KITCHEN', 'ORDER', ?, 'Comanda enviada a cocina')",
                findUserId(employeeCode), orderId.toString());
        return response;
    }

    public OrderResponse getById(Long orderId, String employeeCode, boolean administrator) {
        OrderMeta meta = findOrderMeta(orderId);
        assertOwner(meta, employeeCode, administrator);
        return loadOrder(orderId);
    }

    public List<OrderResponse> kitchenOrders() {
        List<Long> ids = jdbc.query("SELECT order_id FROM pos_schema.orders " +
                        "WHERE status IN ('ENVIADO_COCINA','EN_PREPARACION','LISTO') ORDER BY sent_at, order_id",
                (rs, row) -> rs.getLong("order_id"));
        return ids.stream().map(this::loadOrder).toList();
    }

    private void replaceItems(Long orderId, List<OrderItemRequest> items) {
        for (OrderItemRequest item : items) {
            ProductInfo product = findAvailableProduct(item.productId());
            jdbc.update("INSERT INTO pos_schema.order_items(order_id, product_id, product_name_snapshot, quantity, unit_price, notes) " +
                            "VALUES (?, ?, ?, ?, ?, ?)", orderId, product.productId(), product.name(),
                    item.quantity(), product.unitPrice(), item.notes());
        }
    }

    private void recalculateTotals(Long orderId) {
        BigDecimal subtotal = jdbc.queryForObject("SELECT COALESCE(SUM(line_total), 0) FROM pos_schema.order_items WHERE order_id = ?",
                BigDecimal.class, orderId);
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        jdbc.update("UPDATE pos_schema.orders SET subtotal = ?, total_amount = ?, updated_at = NOW() WHERE order_id = ?",
                subtotal, subtotal, orderId);
    }

    private ProductInfo findAvailableProduct(Long productId) {
        try {
            return jdbc.queryForObject("SELECT product_id, name, unit_price FROM pos_schema.products " +
                            "WHERE product_id = ? AND is_active = TRUE AND is_available = TRUE",
                    (rs, row) -> new ProductInfo(rs.getLong("product_id"), rs.getString("name"), rs.getBigDecimal("unit_price")), productId);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto " + productId + " no existe o no está disponible.");
        }
    }

    private TableInfo lockTable(Long tableId) {
        try {
            return jdbc.queryForObject("SELECT table_id, table_code, status FROM pos_schema.dining_tables " +
                            "WHERE table_id = ? AND is_active = TRUE FOR UPDATE",
                    (rs, row) -> new TableInfo(rs.getLong("table_id"), rs.getString("table_code"), rs.getString("status")), tableId);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesa no encontrada o inactiva.");
        }
    }

    private OrderMeta lockOrder(Long orderId) {
        try {
            return jdbc.queryForObject("SELECT o.order_id, o.table_id, o.status, u.employee_code " +
                            "FROM pos_schema.orders o JOIN auth_schema.users u ON u.user_id = o.waiter_user_id " +
                            "WHERE o.order_id = ? FOR UPDATE OF o",
                    (rs, row) -> new OrderMeta(rs.getLong("order_id"), rs.getLong("table_id"),
                            rs.getString("status"), rs.getString("employee_code")), orderId);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comanda no encontrada.");
        }
    }

    private OrderMeta findOrderMeta(Long orderId) {
        try {
            return jdbc.queryForObject("SELECT o.order_id, o.table_id, o.status, u.employee_code " +
                            "FROM pos_schema.orders o JOIN auth_schema.users u ON u.user_id = o.waiter_user_id WHERE o.order_id = ?",
                    (rs, row) -> new OrderMeta(rs.getLong("order_id"), rs.getLong("table_id"),
                            rs.getString("status"), rs.getString("employee_code")), orderId);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comanda no encontrada.");
        }
    }

    private void assertOwner(OrderMeta order, String employeeCode, boolean administrator) {
        if (!administrator && !order.employeeCode().equalsIgnoreCase(employeeCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La comanda pertenece a otro mozo.");
        }
    }

    private Long findUserId(String employeeCode) {
        try {
            return jdbc.queryForObject("SELECT user_id FROM auth_schema.users WHERE employee_code = ? AND is_active = TRUE",
                    Long.class, employeeCode);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado o inactivo.");
        }
    }

    private String tableCode(Long tableId) {
        return jdbc.queryForObject("SELECT table_code FROM pos_schema.dining_tables WHERE table_id = ?", String.class, tableId);
    }

    private OrderResponse loadOrder(Long orderId) {
        OrderHeader header;
        try {
            header = jdbc.queryForObject("SELECT o.order_id, o.order_code, o.table_id, t.table_code, " +
                            "o.waiter_user_id, u.employee_code AS waiter_employee_code, o.status, o.notes, o.subtotal, " +
                            "o.total_amount, o.created_at, o.sent_at " +
                            "FROM pos_schema.orders o JOIN pos_schema.dining_tables t ON t.table_id = o.table_id " +
                            "JOIN auth_schema.users u ON u.user_id = o.waiter_user_id WHERE o.order_id = ?",
                    (rs, row) -> new OrderHeader(rs.getLong("order_id"), rs.getString("order_code"),
                            rs.getLong("table_id"), rs.getString("table_code"), rs.getLong("waiter_user_id"),
                            rs.getString("waiter_employee_code"), rs.getString("status"), rs.getString("notes"),
                            rs.getBigDecimal("subtotal"), rs.getBigDecimal("total_amount"),
                            rs.getObject("created_at", OffsetDateTime.class), rs.getObject("sent_at", OffsetDateTime.class)), orderId);
        } catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Comanda no encontrada.");
        }
        List<OrderItemResponse> items = jdbc.query("SELECT order_item_id, product_id, product_name_snapshot, quantity, " +
                        "unit_price, notes, line_total, status FROM pos_schema.order_items WHERE order_id = ? ORDER BY order_item_id",
                (rs, row) -> new OrderItemResponse(rs.getLong("order_item_id"),
                        (Long) rs.getObject("product_id"), rs.getString("product_name_snapshot"),
                        rs.getInt("quantity"), rs.getBigDecimal("unit_price"), rs.getString("notes"),
                        rs.getBigDecimal("line_total"), rs.getString("status")), orderId);
        return new OrderResponse(header.orderId(), header.orderCode(), header.tableId(), header.tableCode(),
                header.waiterUserId(), header.waiterEmployeeCode(), header.status(), header.notes(),
                header.subtotal(), header.totalAmount(), header.createdAt(), header.sentAt(), items);
    }

    private record TableInfo(Long tableId, String tableCode, String status) { }
    private record ProductInfo(Long productId, String name, BigDecimal unitPrice) { }
    private record OrderMeta(Long orderId, Long tableId, String status, String employeeCode) { }
    private record OrderHeader(Long orderId, String orderCode, Long tableId, String tableCode,
                               Long waiterUserId, String waiterEmployeeCode, String status, String notes,
                               BigDecimal subtotal, BigDecimal totalAmount, OffsetDateTime createdAt, OffsetDateTime sentAt) { }
}
