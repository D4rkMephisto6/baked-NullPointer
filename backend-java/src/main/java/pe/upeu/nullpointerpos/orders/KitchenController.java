package pe.upeu.nullpointerpos.orders;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cocina")
public class KitchenController {
    private final OrderService orderService;
    public KitchenController(OrderService orderService) { this.orderService = orderService; }

    @GetMapping("/pedidos")
    public List<OrderResponse> activeKitchenOrders() { return orderService.kitchenOrders(); }
}
