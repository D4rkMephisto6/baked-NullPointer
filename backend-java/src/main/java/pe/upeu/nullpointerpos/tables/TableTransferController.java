package pe.upeu.nullpointerpos.tables;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.upeu.nullpointerpos.orders.OrderService;
import pe.upeu.nullpointerpos.orders.TableTransferResponse;
import pe.upeu.nullpointerpos.orders.TransferTableRequest;

@RestController
@RequestMapping("/api/v1/mesas")
public class TableTransferController {
    private final OrderService orderService;
    public TableTransferController(OrderService orderService) { this.orderService = orderService; }

    @PutMapping("/transferir")
    public TableTransferResponse transfer(@Valid @RequestBody TransferTableRequest request,
                                          Authentication authentication) {
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
        return orderService.transfer(request, authentication.getName(), administrator);
    }
}
