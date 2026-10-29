package pe.upeu.nullpointerpos.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ModifyOrderItemsRequest(
        @NotNull(message = "items es obligatorio") List<@Valid OrderItemRequest> items) { }
