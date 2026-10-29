package pe.upeu.nullpointerpos.orders;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OrderItemRequest(
        @NotNull(message = "productId es obligatorio") Long productId,
        @NotNull(message = "quantity es obligatoria") @Min(value = 1, message = "quantity debe ser al menos 1") Integer quantity,
        @Size(max = 500, message = "notes no puede superar 500 caracteres") String notes) { }
