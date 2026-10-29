package pe.upeu.nullpointerpos.orders;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "tableId es obligatorio") Long tableId,
        @Size(max = 1000, message = "notes no puede superar 1000 caracteres") String notes,
        @NotEmpty(message = "La comanda debe incluir al menos un plato") List<@Valid OrderItemRequest> items) { }
