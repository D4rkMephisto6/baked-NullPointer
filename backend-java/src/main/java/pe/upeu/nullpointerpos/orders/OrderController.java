package pe.upeu.nullpointerpos.orders;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/pedidos")
public class OrderController {
    private final OrderService orderService;
    private final SimpMessagingTemplate messagingTemplate;

    public OrderController(OrderService orderService, SimpMessagingTemplate messagingTemplate) {
        this.orderService = orderService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@Valid @RequestBody CreateOrderRequest request, Authentication authentication) {
        return orderService.create(request, authentication.getName());
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id, Authentication authentication) {
        return orderService.getById(id, authentication.getName(), isAdministrator(authentication));
    }

    @PutMapping("/{id}/detalles")
    public OrderResponse updateItems(@PathVariable Long id, @Valid @RequestBody ModifyOrderItemsRequest request,
                                     Authentication authentication) {
        return orderService.updateItems(id, request, authentication.getName(), isAdministrator(authentication));
    }

    @PostMapping("/{id}/enviar-cocina")
    public Map<String, Object> sendToKitchen(@PathVariable Long id, Authentication authentication) {
        OrderResponse order = orderService.sendToKitchen(id, authentication.getName(), isAdministrator(authentication));
        // El servicio transaccional ya confirmó la operación antes de devolver el control al controlador.
        messagingTemplate.convertAndSend("/topic/cocina", Map.of(
                "eventType", "ORDER_SENT_TO_KITCHEN",
                "occurredAt", java.time.OffsetDateTime.now().toString(),
                "order", order));
        return Map.of("message", "Pedido enviado a cocina correctamente.", "order", order);
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
    }
}
