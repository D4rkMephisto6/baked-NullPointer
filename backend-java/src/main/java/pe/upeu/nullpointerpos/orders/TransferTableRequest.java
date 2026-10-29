package pe.upeu.nullpointerpos.orders;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TransferTableRequest(
        @NotNull(message = "orderId es obligatorio") Long orderId,
        @NotNull(message = "targetTableId es obligatorio") Long targetTableId,
        @Size(max = 250, message = "reason no puede superar 250 caracteres") String reason) { }
